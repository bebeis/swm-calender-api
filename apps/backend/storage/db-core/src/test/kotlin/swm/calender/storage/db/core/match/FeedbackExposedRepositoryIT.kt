package swm.calender.storage.db.core.match

import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.beans.factory.annotation.Autowired
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.MatchRequestType
import swm.calender.core.enums.Platform
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.domain.model.Feedback
import swm.calender.match.domain.model.FeedbackScores
import swm.calender.match.domain.model.MatchRequest
import swm.calender.match.domain.model.ServiceProfile
import swm.calender.storage.db.core.RepositoryTestSupport
import swm.calender.storage.db.core.team.SubServiceActivationTable
import swm.calender.storage.db.core.team.TeamMemberHistoryTable
import swm.calender.storage.db.core.team.TeamMemberTable
import swm.calender.storage.db.core.team.TeamTable
import java.time.Instant
import java.time.OffsetDateTime

class FeedbackExposedRepositoryIT : RepositoryTestSupport() {
    @Autowired
    private lateinit var matchCampaignExposedRepository: MatchCampaignExposedRepository

    @Autowired
    private lateinit var matchRequestExposedRepository: MatchRequestExposedRepository

    @Autowired
    private lateinit var feedbackExposedRepository: FeedbackExposedRepository

    init {
        extension(SpringExtension())

        val baseInstant = Instant.parse("2026-05-10T00:00:00Z")

        beforeTest {
            cleanMatchTables()
        }

        test("save stores assignment feedback") {
            // given
            val fixture = createAcceptedAssignmentFixture(baseInstant)

            // when
            val savedFeedback = feedbackExposedRepository.save(
                feedback(
                    assignment = fixture.assignment,
                    submittedByTeamId = fixture.requestingTeamId,
                    submittedByUserId = UserId(10L),
                    submittedAt = baseInstant.plusSeconds(60),
                ),
            )

            // then
            savedFeedback.assignmentId shouldBe fixture.assignment.requireId()
            savedFeedback.scores.usability shouldBe 5
            feedbackExposedRepository.existsByAssignmentId(fixture.assignment.requireId()) shouldBe true
            feedbackExposedRepository.findByAssignmentId(fixture.assignment.requireId())?.summary shouldBe
                "The service was useful during testing."
        }

        test("findTeamTestHistoryByTeamId returns completed assignment with feedback summary") {
            // given
            val fixture = createAcceptedAssignmentFixture(baseInstant)
            val savedFeedback = feedbackExposedRepository.save(
                feedback(
                    assignment = fixture.assignment,
                    submittedByTeamId = fixture.requestingTeamId,
                    submittedByUserId = UserId(10L),
                    submittedAt = baseInstant.plusSeconds(60),
                ),
            )
            matchRequestExposedRepository.saveAssignment(
                fixture.assignment.submitFeedback(baseInstant.plusSeconds(60)),
            )

            // when
            val history = feedbackExposedRepository.findTeamTestHistoryByTeamId(fixture.requestingTeamId)

            // then
            history.size shouldBe 1
            history.single().campaignId shouldBe requireNotNull(fixture.campaign.id)
            history.single().serviceName shouldBe "Service"
            history.single().feedback?.id shouldBe savedFeedback.id
        }
    }

    private fun cleanMatchTables() {
        transaction {
            MatchNotificationTable.deleteAll()
            FeedbackTable.deleteAll()
            AssignmentTable.deleteAll()
            MatchRequestStatusHistoryTable.deleteAll()
            MatchRequestTable.deleteAll()
            DuplicateAnalysisMatchDimensionTable.deleteAll()
            DuplicateAnalysisMatchTable.deleteAll()
            DuplicateAnalysisTable.deleteAll()
            CandidateIdeaPlatformTable.deleteAll()
            CandidateIdeaTable.deleteAll()
            BetaCampaignTable.deleteAll()
            ActiveServiceProfileTable.deleteAll()
            ServiceProfileScreenshotTable.deleteAll()
            ServiceProfilePlatformTable.deleteAll()
            ServiceProfileTable.deleteAll()
            TeamMemberHistoryTable.deleteAll()
            TeamMemberTable.deleteAll()
            SubServiceActivationTable.deleteAll()
            TeamTable.deleteAll()
        }
    }

    private fun createAcceptedAssignmentFixture(createdAt: Instant): AssignmentFixture {
        val requestingTeamId = createTeam("Requester", "REQUESTER")
        val targetTeamId = createTeam("Target", "TARGET")
        val campaign = createCampaign(targetTeamId, createdAt)
        val request = matchRequestExposedRepository.saveRequest(
            MatchRequest.createPending(
                campaignId = requireNotNull(campaign.id),
                requestingTeamId = requestingTeamId,
                targetTeamId = targetTeamId,
                type = MatchRequestType.ONE_WAY,
                message = "Please test this service.",
                createdAt = createdAt,
            ).accept(createdAt.plusSeconds(30)),
        )
        val assignment = matchRequestExposedRepository.saveAssignment(
            Assignment.createFrom(request, createdAt.plusSeconds(30)),
        )

        return AssignmentFixture(
            requestingTeamId = requestingTeamId,
            targetTeamId = targetTeamId,
            campaign = campaign,
            assignment = assignment,
        )
    }

    private fun createTeam(
        name: String,
        inviteCode: String,
    ): TeamId = transaction {
        val now = Instant.parse("2026-05-10T00:00:00Z")
        val savedTeamId = TeamTable.insert {
            it[TeamTable.name] = name
            it[description] = "$name description"
            it[TeamTable.inviteCode] = inviteCode
            it[createdAt] = now.toLocalDateTime()
            it[updatedAt] = now.toLocalDateTime()
        }[TeamTable.id]

        SubServiceActivationTable.insert {
            it[teamId] = savedTeamId
            it[calendarEnabled] = false
            it[matchEnabled] = true
            it[calendarEnabledAt] = null
            it[matchEnabledAt] = now.toLocalDateTime()
            it[calendarDisabledAt] = null
            it[matchDisabledAt] = null
        }

        TeamId(savedTeamId)
    }

    private fun createCampaign(
        teamId: TeamId,
        createdAt: Instant,
    ): BetaCampaign {
        val profile = matchCampaignExposedRepository.saveServiceProfile(
            ServiceProfile.createActive(
                teamId = teamId,
                nextVersion = 1,
                isPublic = true,
                name = "Service",
                summary = "Summary",
                description = "Description",
                category = CampaignCategory.PRODUCTIVITY,
                platforms = listOf(Platform.WEB),
                screenshotUrls = emptyList(),
                demoUrl = null,
                createdAt = createdAt,
            ),
        )

        return matchCampaignExposedRepository.saveCampaign(
            BetaCampaign.createOpen(
                teamId = teamId,
                serviceProfileId = requireNotNull(profile.id),
                title = "Beta",
                description = "Try this service",
                targetTeamCount = 3,
                deadline = OffsetDateTime.parse("2026-05-20T00:00:00Z"),
                reciprocalAvailable = true,
                requirements = "Chrome",
                createdAt = createdAt,
            ).copy(status = CampaignStatus.OPEN),
        )
    }

    private fun feedback(
        assignment: Assignment,
        submittedByTeamId: TeamId,
        submittedByUserId: UserId,
        submittedAt: Instant,
    ): Feedback {
        return Feedback.submit(
            assignment = assignment,
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

    private data class AssignmentFixture(
        val requestingTeamId: TeamId,
        val targetTeamId: TeamId,
        val campaign: BetaCampaign,
        val assignment: Assignment,
    )
}
