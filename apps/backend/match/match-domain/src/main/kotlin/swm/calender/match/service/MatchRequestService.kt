package swm.calender.match.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.MatchRequest
import swm.calender.match.domain.model.MatchRequestStatusHistory
import swm.calender.match.domain.model.Notification
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import swm.calender.match.implement.MatchCampaignReader
import swm.calender.match.implement.MatchRequestReader
import swm.calender.match.implement.MatchRequestWriter
import swm.calender.match.service.request.AssignmentGetRequest
import swm.calender.match.service.request.MatchRequestCreateRequest
import swm.calender.match.service.request.MatchRequestStatusChangeRequest
import swm.calender.match.service.response.AssignmentResponse
import swm.calender.match.service.response.MatchRequestResponse
import swm.calender.match.service.response.MatchRequestStatusChangeResponse
import java.time.Clock
import java.time.Instant

@Service
class MatchRequestService(
    private val teamReader: TeamReader,
    private val matchCampaignReader: MatchCampaignReader,
    private val matchRequestReader: MatchRequestReader,
    private val matchRequestWriter: MatchRequestWriter,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Transactional
    fun createRequest(request: MatchRequestCreateRequest): MatchRequestResponse {
        val requestingTeam = getMatchEnabledTeam(request.actorUserId)
        val requestingTeamId = requestingTeam.requireId()
        val targetCampaign = matchCampaignReader.getOpenPublicCampaign(request.campaignId)
        if (targetCampaign.teamId == requestingTeamId) {
            throw MatchDomainException(MatchErrorMessage.MATCH_REQUEST_SELF_REQUEST_NOT_ALLOWED)
        }
        if (request.type == MatchRequestType.RECIPROCAL) {
            validateReciprocalRequest(
                requestingTeamId = requestingTeamId,
                targetCampaignReciprocalAvailable = targetCampaign.reciprocalAvailable,
            )
        }
        matchRequestReader.ensureNoActiveRequest(
            campaignId = request.campaignId,
            requestingTeamId = requestingTeamId,
        )

        val now = now()
        val savedRequest = matchRequestWriter.saveRequest(
            MatchRequest.createPending(
                campaignId = request.campaignId,
                requestingTeamId = requestingTeamId,
                targetTeamId = targetCampaign.teamId,
                type = request.type,
                message = request.message,
                createdAt = now,
            ),
        )
        val requestId = savedRequest.requireId()
        matchRequestWriter.saveStatusHistory(
            MatchRequestStatusHistory.created(
                requestId = requestId,
                changedByUserId = request.actorUserId,
                createdAt = now,
            ),
        )
        matchRequestWriter.saveNotification(
            Notification.requestReceived(
                teamId = savedRequest.targetTeamId,
                requestId = requestId,
                createdAt = now,
            ),
        )

        return MatchRequestResponse.from(savedRequest)
    }

    @Transactional
    fun changeRequestStatus(request: MatchRequestStatusChangeRequest): MatchRequestStatusChangeResponse {
        val actorTeam = getMatchEnabledTeam(request.actorUserId)
        val matchRequest = matchRequestReader.getRequest(request.requestId)
        val now = now()
        val changedRequest = when (request.status) {
            MatchRequestStatus.ACCEPTED -> {
                requireTargetOwner(actorTeam, request.actorUserId, matchRequest)
                matchRequest.accept(now)
            }

            MatchRequestStatus.REJECTED -> {
                requireTargetOwner(actorTeam, request.actorUserId, matchRequest)
                matchRequest.reject(now)
            }

            MatchRequestStatus.CANCELED -> {
                requireRequestingMember(actorTeam, matchRequest)
                matchRequest.cancel(now)
            }

            MatchRequestStatus.PENDING -> throw MatchDomainException(
                MatchErrorMessage.MATCH_REQUEST_STATUS_CHANGE_UNSUPPORTED,
            )
        }

        val savedRequest = matchRequestWriter.saveRequest(changedRequest)
        matchRequestWriter.saveStatusHistory(
            MatchRequestStatusHistory.changed(
                requestId = savedRequest.requireId(),
                fromStatus = matchRequest.status,
                toStatus = savedRequest.status,
                changedByUserId = request.actorUserId,
                createdAt = now,
            ),
        )
        val assignment = createAssignmentIfAccepted(savedRequest, now)
        saveStatusNotification(savedRequest, now)

        return MatchRequestStatusChangeResponse(
            request = MatchRequestResponse.from(savedRequest),
            assignmentId = assignment?.id?.value,
            assignmentCreated = assignment != null,
        )
    }

    @Transactional(readOnly = true)
    fun getAssignment(request: AssignmentGetRequest): AssignmentResponse {
        val actorTeam = getMatchEnabledTeam(request.actorUserId)
        val assignment = matchRequestReader.getAssignment(request.assignmentId)
        if (actorTeam.requireId() != assignment.testerTeamId && actorTeam.requireId() != assignment.targetTeamId) {
            throw MatchDomainException(MatchErrorMessage.ASSIGNMENT_NOT_FOUND)
        }

        return AssignmentResponse.from(assignment)
    }

    private fun validateReciprocalRequest(
        requestingTeamId: TeamId,
        targetCampaignReciprocalAvailable: Boolean,
    ) {
        if (!targetCampaignReciprocalAvailable) {
            throw MatchDomainException(MatchErrorMessage.MATCH_REQUEST_RECIPROCAL_UNAVAILABLE)
        }
        if (!matchCampaignReader.hasOpenPublicCampaign(requestingTeamId)) {
            throw MatchDomainException(MatchErrorMessage.MATCH_REQUEST_RECIPROCAL_CAMPAIGN_REQUIRED)
        }
    }

    private fun createAssignmentIfAccepted(
        matchRequest: MatchRequest,
        now: Instant,
    ): Assignment? {
        if (matchRequest.status != MatchRequestStatus.ACCEPTED) {
            return null
        }
        matchRequestReader.findAssignmentByRequestId(matchRequest.requireId())?.let {
            return null
        }

        return matchRequestWriter.saveAssignment(Assignment.createFrom(matchRequest, now))
    }

    private fun saveStatusNotification(
        matchRequest: MatchRequest,
        now: Instant,
    ) {
        val requestId = matchRequest.requireId()
        val notification = when (matchRequest.status) {
            MatchRequestStatus.ACCEPTED -> Notification.requestAccepted(
                teamId = matchRequest.requestingTeamId,
                requestId = requestId,
                createdAt = now,
            )

            MatchRequestStatus.REJECTED -> Notification.requestRejected(
                teamId = matchRequest.requestingTeamId,
                requestId = requestId,
                createdAt = now,
            )

            MatchRequestStatus.CANCELED -> Notification.requestCanceled(
                teamId = matchRequest.targetTeamId,
                requestId = requestId,
                createdAt = now,
            )

            MatchRequestStatus.PENDING -> return
        }

        matchRequestWriter.saveNotification(notification)
    }

    private fun getMatchEnabledTeam(actorUserId: UserId): Team {
        val team = teamReader.getActiveByUserId(actorUserId)
        team.requireMember(actorUserId)

        if (!team.subServiceActivation.matchEnabled) {
            throw MatchDomainException(MatchErrorMessage.MATCH_SUB_SERVICE_DISABLED)
        }

        return team
    }

    private fun requireTargetOwner(
        actorTeam: Team,
        actorUserId: UserId,
        matchRequest: MatchRequest,
    ) {
        if (actorTeam.requireId() != matchRequest.targetTeamId) {
            throw MatchDomainException(MatchErrorMessage.MATCH_REQUEST_NOT_FOUND)
        }
        if (!actorTeam.isOwner(actorUserId)) {
            throw TeamDomainException(TeamErrorMessage.TEAM_OWNER_REQUIRED)
        }
    }

    private fun requireRequestingMember(
        actorTeam: Team,
        matchRequest: MatchRequest,
    ) {
        if (actorTeam.requireId() != matchRequest.requestingTeamId) {
            throw MatchDomainException(MatchErrorMessage.MATCH_REQUEST_NOT_FOUND)
        }
    }

    private fun now(): Instant = Instant.now(clock)
}
