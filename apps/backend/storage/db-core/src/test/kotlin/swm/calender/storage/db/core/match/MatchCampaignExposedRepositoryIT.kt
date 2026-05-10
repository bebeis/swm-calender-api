package swm.calender.storage.db.core.match

import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.beans.factory.annotation.Autowired
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.Platform
import swm.calender.match.domain.CampaignSearchFilter
import swm.calender.match.domain.CampaignSearchSort
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.domain.model.ServiceProfile
import swm.calender.storage.db.core.RepositoryTestSupport
import swm.calender.storage.db.core.team.SubServiceActivationTable
import swm.calender.storage.db.core.team.TeamMemberTable
import swm.calender.storage.db.core.team.TeamTable
import java.time.Instant
import java.time.OffsetDateTime

class MatchCampaignExposedRepositoryIT : RepositoryTestSupport() {
    @Autowired
    private lateinit var matchCampaignExposedRepository: MatchCampaignExposedRepository

    init {
        extension(SpringExtension())

        val baseInstant = Instant.parse("2026-05-10T00:00:00Z")

        beforeTest {
            cleanMatchTables()
        }

        test("saveServiceProfile preserves versions and exposes only the active profile") {
            // given
            val teamId = createTeam("Profile Team", "PROFILE-TEAM")
            val firstProfile = serviceProfile(teamId, version = 1, name = "First", createdAt = baseInstant)
            val secondProfile = serviceProfile(teamId, version = 2, name = "Second", createdAt = baseInstant.plusSeconds(60))

            // when
            val savedFirstProfile = matchCampaignExposedRepository.saveServiceProfile(firstProfile)
            val savedSecondProfile = matchCampaignExposedRepository.saveServiceProfile(secondProfile)
            val activeProfile = matchCampaignExposedRepository.findActiveServiceProfileByTeamId(teamId)

            // then
            savedFirstProfile.version shouldBe 1
            savedSecondProfile.version shouldBe 2
            savedSecondProfile.active shouldBe true
            activeProfile?.id shouldBe savedSecondProfile.id
            matchCampaignExposedRepository.countServiceProfilesByTeamId(teamId) shouldBe 2
        }

        test("searchOpenCampaigns filters public open campaigns by platform and reciprocal availability") {
            // given
            val teamId = createTeam("Campaign Team", "CAMPAIGN-TEAM")
            val otherTeamId = createTeam("Other Campaign Team", "OTHER-CAMPAIGN-TEAM")
            val profile = matchCampaignExposedRepository.saveServiceProfile(
                serviceProfile(teamId, version = 1, name = "Web Service", createdAt = baseInstant),
            )
            val otherProfile = matchCampaignExposedRepository.saveServiceProfile(
                serviceProfile(otherTeamId, version = 1, name = "API Service", createdAt = baseInstant)
                    .copy(platforms = listOf(Platform.API)),
            )
            matchCampaignExposedRepository.saveCampaign(
                campaign(
                    teamId = teamId,
                    serviceProfileId = requireNotNull(profile.id),
                    title = "Web Beta",
                    reciprocalAvailable = true,
                    createdAt = baseInstant.plusSeconds(120),
                ),
            )
            matchCampaignExposedRepository.saveCampaign(
                campaign(
                    teamId = otherTeamId,
                    serviceProfileId = requireNotNull(otherProfile.id),
                    title = "API Beta",
                    reciprocalAvailable = false,
                    createdAt = baseInstant.plusSeconds(180),
                ),
            )

            // when
            val foundCampaigns = matchCampaignExposedRepository.searchOpenCampaigns(
                CampaignSearchFilter(
                    category = CampaignCategory.PRODUCTIVITY,
                    platform = Platform.WEB,
                    reciprocalAvailable = true,
                    sort = CampaignSearchSort.DEADLINE,
                ),
            )

            // then
            foundCampaigns.shouldHaveSize(1)
            foundCampaigns.single().teamName shouldBe "Campaign Team"
            foundCampaigns.single().serviceProfile.name shouldBe "Web Service"
            foundCampaigns.single().campaign.title shouldBe "Web Beta"
        }

        test("findReleasedServiceProfiles returns active public profiles with open campaign descriptions") {
            // given
            val teamId = createTeam("Released Team", "RELEASED-TEAM")
            val profile = matchCampaignExposedRepository.saveServiceProfile(
                serviceProfile(teamId, version = 1, name = "Released Service", createdAt = baseInstant),
            )
            matchCampaignExposedRepository.saveCampaign(
                campaign(
                    teamId = teamId,
                    serviceProfileId = requireNotNull(profile.id),
                    title = "Released Beta",
                    reciprocalAvailable = true,
                    createdAt = baseInstant.plusSeconds(120),
                ),
            )

            // when
            val releasedProfiles = matchCampaignExposedRepository.findReleasedServiceProfiles()

            // then
            releasedProfiles.shouldHaveSize(1)
            releasedProfiles.single().serviceProfile.name shouldBe "Released Service"
            releasedProfiles.single().openCampaignDescriptions shouldBe listOf("Try Released Beta")
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

    private fun serviceProfile(
        teamId: TeamId,
        version: Int,
        name: String,
        createdAt: Instant,
    ): ServiceProfile {
        return ServiceProfile.createActive(
            teamId = teamId,
            nextVersion = version,
            isPublic = true,
            name = name,
            summary = "$name summary",
            description = "$name description",
            category = CampaignCategory.PRODUCTIVITY,
            platforms = listOf(Platform.WEB),
            screenshotUrls = listOf("https://example.com/$version.png"),
            demoUrl = "https://example.com/$version",
            createdAt = createdAt,
        )
    }

    private fun campaign(
        teamId: TeamId,
        serviceProfileId: Long,
        title: String,
        reciprocalAvailable: Boolean,
        createdAt: Instant,
    ): BetaCampaign {
        return BetaCampaign.createOpen(
            teamId = teamId,
            serviceProfileId = serviceProfileId,
            title = title,
            description = "Try $title",
            targetTeamCount = 3,
            deadline = OffsetDateTime.parse("2026-05-20T00:00:00Z"),
            reciprocalAvailable = reciprocalAvailable,
            requirements = "Chrome",
            createdAt = createdAt,
        ).copy(status = CampaignStatus.OPEN)
    }
}
