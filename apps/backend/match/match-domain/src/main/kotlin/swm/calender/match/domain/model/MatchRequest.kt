package swm.calender.match.domain.model

import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import java.time.Instant

private const val MAX_REQUEST_MESSAGE_LENGTH = 1000

data class MatchRequest(
    val id: RequestId? = null,
    val campaignId: CampaignId,
    val requestingTeamId: TeamId,
    val targetTeamId: TeamId,
    val type: MatchRequestType,
    val status: MatchRequestStatus,
    val message: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        validateDifferentTeams(requestingTeamId, targetTeamId)
        validateMessage(message)
    }

    fun accept(now: Instant): MatchRequest {
        return changeStatus(MatchRequestStatus.ACCEPTED, now)
    }

    fun reject(now: Instant): MatchRequest {
        return changeStatus(MatchRequestStatus.REJECTED, now)
    }

    fun cancel(now: Instant): MatchRequest {
        return changeStatus(MatchRequestStatus.CANCELED, now)
    }

    fun requireId(): RequestId {
        return id ?: throw MatchDomainException(MatchErrorMessage.MATCH_REQUEST_NOT_FOUND)
    }

    private fun changeStatus(
        status: MatchRequestStatus,
        now: Instant,
    ): MatchRequest {
        if (this.status != MatchRequestStatus.PENDING) {
            throw MatchDomainException(MatchErrorMessage.MATCH_REQUEST_FINAL_STATUS)
        }

        return copy(status = status, updatedAt = now)
    }

    companion object {
        fun createPending(
            campaignId: CampaignId,
            requestingTeamId: TeamId,
            targetTeamId: TeamId,
            type: MatchRequestType,
            message: String?,
            createdAt: Instant,
        ): MatchRequest {
            return MatchRequest(
                campaignId = campaignId,
                requestingTeamId = requestingTeamId,
                targetTeamId = targetTeamId,
                type = type,
                status = MatchRequestStatus.PENDING,
                message = message?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }

        private fun validateDifferentTeams(
            requestingTeamId: TeamId,
            targetTeamId: TeamId,
        ) {
            if (requestingTeamId == targetTeamId) {
                throw MatchDomainException(MatchErrorMessage.MATCH_REQUEST_SELF_REQUEST_NOT_ALLOWED)
            }
        }

        private fun validateMessage(message: String?) {
            if (message != null && message.length > MAX_REQUEST_MESSAGE_LENGTH) {
                throw MatchDomainException(MatchErrorMessage.MATCH_REQUEST_MESSAGE_TOO_LONG)
            }
        }
    }
}

data class MatchRequestStatusHistory(
    val id: Long? = null,
    val requestId: RequestId,
    val fromStatus: MatchRequestStatus?,
    val toStatus: MatchRequestStatus,
    val changedByUserId: UserId,
    val createdAt: Instant,
) {
    companion object {
        fun created(
            requestId: RequestId,
            changedByUserId: UserId,
            createdAt: Instant,
        ): MatchRequestStatusHistory {
            return MatchRequestStatusHistory(
                requestId = requestId,
                fromStatus = null,
                toStatus = MatchRequestStatus.PENDING,
                changedByUserId = changedByUserId,
                createdAt = createdAt,
            )
        }

        fun changed(
            requestId: RequestId,
            fromStatus: MatchRequestStatus,
            toStatus: MatchRequestStatus,
            changedByUserId: UserId,
            createdAt: Instant,
        ): MatchRequestStatusHistory {
            return MatchRequestStatusHistory(
                requestId = requestId,
                fromStatus = fromStatus,
                toStatus = toStatus,
                changedByUserId = changedByUserId,
                createdAt = createdAt,
            )
        }
    }
}
