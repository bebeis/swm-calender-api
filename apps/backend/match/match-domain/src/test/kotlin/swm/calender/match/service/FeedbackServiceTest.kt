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
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.core.enums.SubService
import swm.calender.core.team.domain.model.SubServiceActivation
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.domain.TeamTestHistoryItem
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.Feedback
import swm.calender.match.domain.model.FeedbackScores
import swm.calender.match.domain.model.Notification
import swm.calender.match.implement.FeedbackReader
import swm.calender.match.implement.FeedbackWriter
import swm.calender.match.implement.MatchRequestReader
import swm.calender.match.implement.MatchRequestWriter
import swm.calender.match.service.request.FeedbackSubmitRequest
import swm.calender.match.service.request.TeamTestHistoryGetRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FeedbackServiceTest :
    FunSpec({
        val fixedInstant = Instant.parse("2026-05-10T00:00:00Z")
        val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

        lateinit var teamReader: TeamReader
        lateinit var matchRequestReader: MatchRequestReader
        lateinit var matchRequestWriter: MatchRequestWriter
        lateinit var feedbackReader: FeedbackReader
        lateinit var feedbackWriter: FeedbackWriter
        lateinit var feedbackService: FeedbackService

        beforeTest {
            teamReader = mockk()
            matchRequestReader = mockk()
            matchRequestWriter = mockk()
            feedbackReader = mockk()
            feedbackWriter = mockk()
            feedbackService = FeedbackService(
                teamReader = teamReader,
                matchRequestReader = matchRequestReader,
                matchRequestWriter = matchRequestWriter,
                feedbackReader = feedbackReader,
                feedbackWriter = feedbackWriter,
                clock = fixedClock,
            )
        }

        test("submitFeedback saves feedback and updates assignment status") {
            // given
            val actorUserId = UserId(10L)
            val testerTeamId = TeamId(1L)
            val targetTeamId = TeamId(2L)
            val assignmentId = AssignmentId(31L)
            val assignment = assignedAssignment(
                assignmentId = assignmentId,
                testerTeamId = testerTeamId,
                targetTeamId = targetTeamId,
                createdAt = fixedInstant.minusSeconds(60),
            )
            val feedbackSlot = slot<Feedback>()
            val assignmentSlot = slot<Assignment>()
            every { teamReader.getActiveByUserId(actorUserId) } returns matchEnabledTeam(
                teamId = testerTeamId,
                ownerUserId = actorUserId,
            )
            every { matchRequestReader.getAssignment(assignmentId) } returns assignment
            every { feedbackReader.ensureNoFeedback(assignmentId) } just Runs
            every { feedbackWriter.save(capture(feedbackSlot)) } answers {
                feedbackSlot.captured.copy(id = swm.calender.core.common.id.FeedbackId(41L))
            }
            every { matchRequestWriter.saveAssignment(capture(assignmentSlot)) } answers {
                assignmentSlot.captured
            }
            every { matchRequestWriter.saveNotification(any()) } answers {
                firstArg<Notification>().copy(id = 51L)
            }

            // when
            val response = feedbackService.submitFeedback(
                FeedbackSubmitRequest(
                    actorUserId = actorUserId,
                    assignmentId = assignmentId,
                    scores = validScores(),
                    summary = "The service was useful during testing.",
                    improvementSuggestion = "Add onboarding.",
                ),
            )

            // then
            response.feedbackId shouldBe 41L
            feedbackSlot.captured.submittedByTeamId shouldBe testerTeamId
            assignmentSlot.captured.status shouldBe AssignmentStatus.FEEDBACK_SUBMITTED
            verify(exactly = 1) { matchRequestWriter.saveAssignment(any()) }
            verify(exactly = 1) { matchRequestWriter.saveNotification(any()) }
        }

        test("getTeamTestHistory returns scoped team history") {
            // given
            val actorUserId = UserId(10L)
            val teamId = TeamId(1L)
            val assignment = assignedAssignment(
                assignmentId = AssignmentId(31L),
                testerTeamId = teamId,
                targetTeamId = TeamId(2L),
                createdAt = fixedInstant.minusSeconds(60),
            ).submitFeedback(fixedInstant)
            val feedback = feedback(
                assignmentId = assignment.requireId(),
                submittedByTeamId = teamId,
                submittedByUserId = actorUserId,
                submittedAt = fixedInstant,
            )
            every { teamReader.getActiveByUserId(actorUserId) } returns matchEnabledTeam(
                teamId = teamId,
                ownerUserId = actorUserId,
            )
            every { feedbackReader.findTeamTestHistory(teamId) } returns listOf(
                TeamTestHistoryItem(
                    assignment = assignment,
                    campaignId = CampaignId(21L),
                    serviceName = "Service",
                    feedback = feedback,
                ),
            )

            // when
            val response = feedbackService.getTeamTestHistory(
                TeamTestHistoryGetRequest(actorUserId = actorUserId),
            )

            // then
            response.items.size shouldBe 1
            response.items.single().feedbackSummary shouldBe "The service was useful during testing."
        }
    }) {
    companion object {
        private val baseInstant: Instant = Instant.parse("2026-05-09T23:00:00Z")

        private fun validScores(): FeedbackScores {
            return FeedbackScores(
                usability = 5,
                value = 4,
                reliability = 5,
                recommendation = 4,
            )
        }

        private fun feedback(
            assignmentId: AssignmentId,
            submittedByTeamId: TeamId,
            submittedByUserId: UserId,
            submittedAt: Instant,
        ): Feedback {
            return Feedback(
                id = swm.calender.core.common.id.FeedbackId(41L),
                assignmentId = assignmentId,
                submittedByTeamId = submittedByTeamId,
                submittedByUserId = submittedByUserId,
                scores = validScores(),
                summary = "The service was useful during testing.",
                improvementSuggestion = "Add onboarding.",
                submittedAt = submittedAt,
            )
        }

        private fun assignedAssignment(
            assignmentId: AssignmentId,
            testerTeamId: TeamId,
            targetTeamId: TeamId,
            createdAt: Instant,
        ): Assignment {
            return Assignment(
                id = assignmentId,
                requestId = RequestId(11L),
                testerTeamId = testerTeamId,
                targetTeamId = targetTeamId,
                status = AssignmentStatus.ASSIGNED,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }

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
    }
}
