package swm.calender.match.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.Platform
import swm.calender.core.enums.SubService
import swm.calender.core.team.domain.model.SubServiceActivation
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.domain.model.ServiceProfile
import swm.calender.match.implement.MatchCampaignReader
import swm.calender.match.implement.MatchCampaignWriter
import swm.calender.match.service.request.CampaignCreateRequest
import swm.calender.match.service.request.ServiceProfileCreateRequest
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class MatchCampaignServiceTest :
    FunSpec({
        val fixedInstant = Instant.parse("2026-05-10T00:00:00Z")
        val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

        lateinit var teamReader: TeamReader
        lateinit var matchCampaignReader: MatchCampaignReader
        lateinit var matchCampaignWriter: MatchCampaignWriter
        lateinit var matchCampaignService: MatchCampaignService

        beforeTest {
            teamReader = mockk()
            matchCampaignReader = mockk()
            matchCampaignWriter = mockk()
            matchCampaignService = MatchCampaignService(
                teamReader = teamReader,
                matchCampaignReader = matchCampaignReader,
                matchCampaignWriter = matchCampaignWriter,
                clock = fixedClock,
            )
        }

        test("createServiceProfile archives the current active profile and saves a new active version") {
            // given
            val teamId = TeamId(1L)
            val ownerUserId = UserId(10L)
            val existingProfile = activeProfile(
                id = 5L,
                teamId = teamId,
                version = 1,
                createdAt = fixedInstant.minusSeconds(3600),
            )
            val savedProfiles = mutableListOf<ServiceProfile>()
            every { teamReader.getActiveByUserId(ownerUserId) } returns matchEnabledTeam(teamId, ownerUserId)
            every { matchCampaignReader.getActiveServiceProfile(teamId) } returns existingProfile
            every { matchCampaignReader.getNextServiceProfileVersion(teamId) } returns 2
            every { matchCampaignWriter.saveServiceProfile(any()) } answers {
                firstArg<ServiceProfile>().let {
                    savedProfiles += it
                    if (it.id == null) it.copy(id = 6L) else it
                }
            }

            // when
            val response = matchCampaignService.createServiceProfile(
                ServiceProfileCreateRequest(
                    actorUserId = ownerUserId,
                    name = "New Service",
                    summary = "New summary",
                    description = "New description",
                    category = CampaignCategory.PRODUCTIVITY,
                    platforms = listOf(Platform.WEB),
                    screenshotUrls = emptyList(),
                    demoUrl = null,
                    isPublic = true,
                ),
            )

            // then
            response.serviceProfileId shouldBe 6L
            response.active shouldBe true
            response.name shouldBe "New Service"
            savedProfiles[0].active shouldBe false
            savedProfiles[1].version shouldBe 2
            verify(exactly = 2) { matchCampaignWriter.saveServiceProfile(any()) }
        }

        test("createCampaign opens a campaign for the active service profile") {
            // given
            val teamId = TeamId(1L)
            val ownerUserId = UserId(10L)
            val campaignSlot = slot<BetaCampaign>()
            every { teamReader.getActiveByUserId(ownerUserId) } returns matchEnabledTeam(teamId, ownerUserId)
            every { matchCampaignReader.getActiveServiceProfile(teamId) } returns activeProfile(
                id = 5L,
                teamId = teamId,
                version = 1,
                createdAt = fixedInstant,
            )
            every { matchCampaignWriter.saveCampaign(capture(campaignSlot)) } answers {
                campaignSlot.captured.copy(id = CampaignId(7L))
            }

            // when
            val response = matchCampaignService.createCampaign(
                CampaignCreateRequest(
                    actorUserId = ownerUserId,
                    title = "Beta",
                    description = "Try this service",
                    targetTeamCount = 3,
                    deadline = OffsetDateTime.parse("2026-05-20T00:00:00Z"),
                    reciprocalAvailable = true,
                    requirements = "Chrome",
                ),
            )

            // then
            response.campaignId shouldBe 7L
            response.status shouldBe CampaignStatus.OPEN
            campaignSlot.captured.serviceProfileId shouldBe 5L
            campaignSlot.captured.createdAt shouldBe fixedInstant
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
                inviteCode = "INVITE123",
                ownerUserId = ownerUserId,
                ownerName = "Owner",
                ownerEmail = "owner@swm.app",
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
                    it.copy(id = TeamMemberId(1L), teamId = teamId)
                },
                subServiceActivation = SubServiceActivation(
                    calendarEnabled = false,
                    matchEnabled = true,
                    matchEnabledAt = baseInstant.plusSeconds(60),
                ),
            )
        }

        private fun activeProfile(
            id: Long,
            teamId: TeamId,
            version: Int,
            createdAt: Instant,
        ): ServiceProfile {
            return ServiceProfile(
                id = id,
                teamId = teamId,
                version = version,
                active = true,
                isPublic = true,
                name = "Service",
                summary = "Summary",
                description = "Description",
                category = CampaignCategory.PRODUCTIVITY,
                platforms = listOf(Platform.WEB),
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }
    }
}
