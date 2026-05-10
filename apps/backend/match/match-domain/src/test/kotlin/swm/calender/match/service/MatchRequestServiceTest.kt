package swm.calender.match.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.FeedbackId
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType
import swm.calender.core.enums.SubService
import swm.calender.core.team.domain.model.SubServiceActivation
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.domain.model.Feedback
import swm.calender.match.domain.model.FeedbackScores
import swm.calender.match.domain.model.MatchRequest
import swm.calender.match.domain.model.MatchRequestStatusHistory
import swm.calender.match.domain.model.Notification
import swm.calender.match.implement.FeedbackReader
import swm.calender.match.implement.MatchCampaignReader
import swm.calender.match.implement.MatchRequestReader
import swm.calender.match.implement.MatchRequestWriter
import swm.calender.match.service.request.AssignmentGetRequest
import swm.calender.match.service.request.MatchRequestCreateRequest
import swm.calender.match.service.request.MatchRequestStatusChangeRequest
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class MatchRequestServiceTest :
    FunSpec({
        val fixedInstant = Instant.parse("2026-05-10T00:00:00Z")
        val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

        lateinit var teamReader: TeamReader
        lateinit var matchCampaignReader: MatchCampaignReader
        lateinit var matchRequestReader: MatchRequestReader
        lateinit var matchRequestWriter: MatchRequestWriter
        lateinit var feedbackReader: FeedbackReader
        lateinit var matchRequestService: MatchRequestService

        beforeTest {
            teamReader = mockk()
            matchCampaignReader = mockk()
            matchRequestReader = mockk()
            matchRequestWriter = mockk()
            feedbackReader = mockk()
            matchRequestService = MatchRequestService(
                teamReader = teamReader,
                matchCampaignReader = matchCampaignReader,
                matchRequestReader = matchRequestReader,
                matchRequestWriter = matchRequestWriter,
                feedbackReader = feedbackReader,
                clock = fixedClock,
            )
        }

        test("createRequest creates pending reciprocal request and notification") {
            // given
            val requesterUserId = UserId(10L)
            val requestingTeamId = TeamId(1L)
            val targetTeamId = TeamId(2L)
            val campaignId = CampaignId(7L)
            val requestSlot = slot<MatchRequest>()
            val notificationSlot = slot<Notification>()
            every { teamReader.getActiveByUserId(requesterUserId) } returns matchEnabledTeam(
                teamId = requestingTeamId,
                ownerUserId = requesterUserId,
            )
            every { matchCampaignReader.getOpenPublicCampaign(campaignId) } returns campaign(
                campaignId = campaignId,
                teamId = targetTeamId,
                reciprocalAvailable = true,
            )
            every { matchCampaignReader.hasOpenPublicCampaign(requestingTeamId) } returns true
            every { matchRequestReader.ensureNoActiveRequest(campaignId, requestingTeamId) } just Runs
            every { matchRequestWriter.saveRequest(capture(requestSlot)) } answers {
                requestSlot.captured.copy(id = RequestId(11L))
            }
            every { matchRequestWriter.saveStatusHistory(any()) } answers {
                firstArg<MatchRequestStatusHistory>().copy(id = 1L)
            }
            every { matchRequestWriter.saveNotification(capture(notificationSlot)) } answers {
                notificationSlot.captured.copy(id = 1L)
            }

            // when
            val response = matchRequestService.createRequest(
                MatchRequestCreateRequest(
                    actorUserId = requesterUserId,
                    campaignId = campaignId,
                    type = MatchRequestType.RECIPROCAL,
                    message = "Please test our service too.",
                ),
            )

            // then
            response.requestId shouldBe 11L
            response.status shouldBe MatchRequestStatus.PENDING
            requestSlot.captured.requestingTeamId shouldBe requestingTeamId
            requestSlot.captured.targetTeamId shouldBe targetTeamId
            notificationSlot.captured.teamId shouldBe targetTeamId
        }

        test("accept pending request creates assignment exactly once") {
            // given
            val targetOwnerUserId = UserId(20L)
            val requestingTeamId = TeamId(1L)
            val targetTeamId = TeamId(2L)
            val requestId = RequestId(11L)
            val assignmentSlot = slot<Assignment>()
            val pendingRequest = pendingRequest(
                requestId = requestId,
                requestingTeamId = requestingTeamId,
                targetTeamId = targetTeamId,
                createdAt = fixedInstant.minusSeconds(120),
            )
            every { teamReader.getActiveByUserId(targetOwnerUserId) } returns matchEnabledTeam(
                teamId = targetTeamId,
                ownerUserId = targetOwnerUserId,
            )
            every { matchRequestReader.getRequest(requestId) } returns pendingRequest
            every { matchRequestWriter.saveRequest(any()) } answers {
                firstArg<MatchRequest>()
            }
            every { matchRequestWriter.saveStatusHistory(any()) } answers {
                firstArg<MatchRequestStatusHistory>().copy(id = 2L)
            }
            every { matchRequestReader.findAssignmentByRequestId(requestId) } returns null
            every { matchRequestWriter.saveAssignment(capture(assignmentSlot)) } answers {
                assignmentSlot.captured.copy(id = AssignmentId(31L))
            }
            every { matchRequestWriter.saveNotification(any()) } answers {
                firstArg<Notification>().copy(id = 2L)
            }

            // when
            val response = matchRequestService.changeRequestStatus(
                MatchRequestStatusChangeRequest(
                    actorUserId = targetOwnerUserId,
                    requestId = requestId,
                    status = MatchRequestStatus.ACCEPTED,
                ),
            )

            // then
            response.request.status shouldBe MatchRequestStatus.ACCEPTED
            response.assignmentId shouldBe 31L
            response.assignmentCreated shouldBe true
            assignmentSlot.captured.requestId shouldBe requestId
            verify(exactly = 1) { matchRequestWriter.saveAssignment(any()) }
        }

        test("getAssignment includes submitted feedback for participant team") {
            // given
            val actorUserId = UserId(10L)
            val testerTeamId = TeamId(1L)
            val targetTeamId = TeamId(2L)
            val assignmentId = AssignmentId(31L)
            val assignment = Assignment(
                id = assignmentId,
                requestId = RequestId(11L),
                testerTeamId = testerTeamId,
                targetTeamId = targetTeamId,
                status = AssignmentStatus.FEEDBACK_SUBMITTED,
                createdAt = fixedInstant.minusSeconds(60),
                updatedAt = fixedInstant,
            )
            every { teamReader.getActiveByUserId(actorUserId) } returns matchEnabledTeam(
                teamId = testerTeamId,
                ownerUserId = actorUserId,
            )
            every { matchRequestReader.getAssignment(assignmentId) } returns assignment
            every { feedbackReader.findByAssignmentId(assignmentId) } returns feedback(
                assignmentId = assignmentId,
                submittedByTeamId = testerTeamId,
                submittedByUserId = actorUserId,
                submittedAt = fixedInstant,
            )

            // when
            val response = matchRequestService.getAssignment(
                AssignmentGetRequest(
                    actorUserId = actorUserId,
                    assignmentId = assignmentId,
                ),
            )

            // then
            response.feedback?.summary shouldBe "The service was useful during testing."
            verify(exactly = 1) { feedbackReader.findByAssignmentId(assignmentId) }
        }
    }) {
    companion object {
        private val baseInstant: Instant = Instant.parse("2026-05-09T23:00:00Z")

        private fun matchEnabledTeam(
            teamId: TeamId,
            ownerUserId: UserId,
        ): Team {
            val team = Team.create(
                name = "Team",
                description = "Description",
                inviteCode = "INVITE${teamId.value}",
                ownerUserId = ownerUserId,
                ownerName = "Owner",
                ownerEmail = "owner${teamId.value}@swm.app",
                createdAt = baseInstant,
            ).changeSubServiceActivation(
                subService = SubService.MATCH,
                enabled = true,
                actorUserId = ownerUserId,
                occurredAt = baseInstant.plusSeconds(60),
            )

            return team.copy(
                id = teamId,
                members = team.members.map {
                    it.copy(id = TeamMemberId(teamId.value), teamId = teamId)
                },
                subServiceActivation = SubServiceActivation(
                    calendarEnabled = false,
                    matchEnabled = true,
                    matchEnabledAt = baseInstant.plusSeconds(60),
                ),
            )
        }

        private fun campaign(
            campaignId: CampaignId,
            teamId: TeamId,
            reciprocalAvailable: Boolean,
        ): BetaCampaign {
            return BetaCampaign.createOpen(
                teamId = teamId,
                serviceProfileId = 5L,
                title = "Beta",
                description = "Try this service",
                targetTeamCount = 3,
                deadline = OffsetDateTime.parse("2026-05-20T00:00:00Z"),
                reciprocalAvailable = reciprocalAvailable,
                requirements = "Chrome",
                createdAt = baseInstant,
            ).copy(id = campaignId, status = CampaignStatus.OPEN)
        }

        private fun pendingRequest(
            requestId: RequestId,
            requestingTeamId: TeamId,
            targetTeamId: TeamId,
            createdAt: Instant,
        ): MatchRequest {
            return MatchRequest.createPending(
                campaignId = CampaignId(7L),
                requestingTeamId = requestingTeamId,
                targetTeamId = targetTeamId,
                type = MatchRequestType.ONE_WAY,
                message = null,
                createdAt = createdAt,
            ).copy(id = requestId)
        }

        private fun feedback(
            assignmentId: AssignmentId,
            submittedByTeamId: TeamId,
            submittedByUserId: UserId,
            submittedAt: Instant,
        ): Feedback {
            return Feedback(
                id = FeedbackId(41L),
                assignmentId = assignmentId,
                submittedByTeamId = submittedByTeamId,
                submittedByUserId = submittedByUserId,
                scores = FeedbackScores(
                    usability = 5,
                    value = 4,
                    reliability = 5,
                    recommendation = 4,
                ),
                summary = "The service was useful during testing.",
                improvementSuggestion = "Add onboarding.",
                submittedAt = submittedAt,
            )
        }
    }
}
