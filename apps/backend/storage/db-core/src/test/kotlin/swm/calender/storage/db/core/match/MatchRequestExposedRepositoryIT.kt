package swm.calender.storage.db.core.match

import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.beans.factory.annotation.Autowired
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType
import swm.calender.core.enums.Platform
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.domain.model.MatchRequest
import swm.calender.match.domain.model.MatchRequestStatusHistory
import swm.calender.match.domain.model.Notification
import swm.calender.match.domain.model.ServiceProfile
import swm.calender.storage.db.core.RepositoryTestSupport
import swm.calender.storage.db.core.team.SubServiceActivationTable
import swm.calender.storage.db.core.team.TeamMemberHistoryTable
import swm.calender.storage.db.core.team.TeamMemberTable
import swm.calender.storage.db.core.team.TeamTable
import java.time.Instant
import java.time.OffsetDateTime

class MatchRequestExposedRepositoryIT : RepositoryTestSupport() {
    @Autowired
    private lateinit var matchCampaignExposedRepository: MatchCampaignExposedRepository

    @Autowired
    private lateinit var matchRequestExposedRepository: MatchRequestExposedRepository

    init {
        extension(SpringExtension())

        val baseInstant = Instant.parse("2026-05-10T00:00:00Z")

        beforeTest {
            cleanMatchTables()
        }

        test("saveRequest stores pending request and detects active duplicate") {
            // given
            val requestingTeamId = createTeam("Requester", "REQUESTER")
            val targetTeamId = createTeam("Target", "TARGET")
            val campaign = createCampaign(targetTeamId, baseInstant)

            // when
            val savedRequest = matchRequestExposedRepository.saveRequest(
                pendingRequest(
                    campaignId = requireNotNull(campaign.id),
                    requestingTeamId = requestingTeamId,
                    targetTeamId = targetTeamId,
                    createdAt = baseInstant,
                ),
            )

            // then
            savedRequest.status shouldBe MatchRequestStatus.PENDING
            matchRequestExposedRepository.existsActiveRequestByCampaignIdAndRequestingTeamId(
                campaignId = requireNotNull(campaign.id),
                requestingTeamId = requestingTeamId,
            ) shouldBe true
        }

        test("accepted request can persist status history, assignment, and notification") {
            // given
            val requestingTeamId = createTeam("Requester", "REQUESTER")
            val targetTeamId = createTeam("Target", "TARGET")
            val campaign = createCampaign(targetTeamId, baseInstant)
            val savedRequest = matchRequestExposedRepository.saveRequest(
                pendingRequest(
                    campaignId = requireNotNull(campaign.id),
                    requestingTeamId = requestingTeamId,
                    targetTeamId = targetTeamId,
                    createdAt = baseInstant,
                ),
            )
            matchRequestExposedRepository.saveStatusHistory(
                MatchRequestStatusHistory.created(
                    requestId = savedRequest.requireId(),
                    changedByUserId = UserId(10L),
                    createdAt = baseInstant,
                ),
            )

            // when
            val acceptedRequest = matchRequestExposedRepository.saveRequest(
                savedRequest.accept(baseInstant.plusSeconds(60)),
            )
            matchRequestExposedRepository.saveStatusHistory(
                MatchRequestStatusHistory.changed(
                    requestId = acceptedRequest.requireId(),
                    fromStatus = MatchRequestStatus.PENDING,
                    toStatus = MatchRequestStatus.ACCEPTED,
                    changedByUserId = UserId(20L),
                    createdAt = baseInstant.plusSeconds(60),
                ),
            )
            val assignment = matchRequestExposedRepository.saveAssignment(
                Assignment.createFrom(acceptedRequest, baseInstant.plusSeconds(120)),
            )
            val notification = matchRequestExposedRepository.saveNotification(
                Notification.requestAccepted(
                    teamId = requestingTeamId,
                    requestId = acceptedRequest.requireId(),
                    createdAt = baseInstant.plusSeconds(120),
                ),
            )

            // then
            assignment.requestId shouldBe acceptedRequest.requireId()
            matchRequestExposedRepository.findAssignmentByRequestId(acceptedRequest.requireId())?.id shouldBe assignment.id
            notification.teamId shouldBe requestingTeamId
            transaction {
                MatchRequestStatusHistoryTable.selectAll().count()
            } shouldBe 2L
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

    private fun pendingRequest(
        campaignId: CampaignId,
        requestingTeamId: TeamId,
        targetTeamId: TeamId,
        createdAt: Instant,
    ): MatchRequest {
        return MatchRequest.createPending(
            campaignId = campaignId,
            requestingTeamId = requestingTeamId,
            targetTeamId = targetTeamId,
            type = MatchRequestType.ONE_WAY,
            message = "Please test this service.",
            createdAt = createdAt,
        )
    }
}
