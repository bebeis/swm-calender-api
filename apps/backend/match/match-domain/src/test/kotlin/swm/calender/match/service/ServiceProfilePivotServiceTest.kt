package swm.calender.match.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.Platform
import swm.calender.core.enums.SubService
import swm.calender.core.team.domain.model.SubServiceActivation
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.domain.model.ServiceProfile
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import swm.calender.match.implement.MatchCampaignReader
import swm.calender.match.implement.MatchCampaignWriter
import swm.calender.match.service.request.ServiceProfileCreateRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ServiceProfilePivotServiceTest :
    FunSpec({
        val fixedInstant = Instant.parse("2026-05-10T00:00:00Z")
        val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

        lateinit var teamReader: TeamReader
        lateinit var matchCampaignReader: MatchCampaignReader
        lateinit var matchCampaignWriter: MatchCampaignWriter
        lateinit var serviceProfilePivotService: ServiceProfilePivotService

        beforeTest {
            teamReader = mockk()
            matchCampaignReader = mockk()
            matchCampaignWriter = mockk()
            serviceProfilePivotService = ServiceProfilePivotService(
                teamReader = teamReader,
                matchCampaignReader = matchCampaignReader,
                matchCampaignWriter = matchCampaignWriter,
                clock = fixedClock,
            )
        }

        test("replaceActiveProfile archives the current active profile and saves a new version") {
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
            every { matchCampaignReader.findActiveServiceProfile(teamId) } returns existingProfile
            every { matchCampaignReader.getNextServiceProfileVersion(teamId) } returns 2
            every { matchCampaignWriter.saveServiceProfile(any()) } answers {
                firstArg<ServiceProfile>().let {
                    savedProfiles += it
                    if (it.id == null) it.copy(id = 6L) else it
                }
            }

            // when
            val response = serviceProfilePivotService.replaceActiveProfile(
                serviceProfileCreateRequest(ownerUserId),
            )

            // then
            response.serviceProfileId shouldBe 6L
            response.active shouldBe true
            response.name shouldBe "New Service"
            savedProfiles[0].active shouldBe false
            savedProfiles[0].updatedAt shouldBe fixedInstant
            savedProfiles[1].version shouldBe 2
            savedProfiles[1].active shouldBe true
            verify(exactly = 2) { matchCampaignWriter.saveServiceProfile(any()) }
        }

        test("replaceActiveProfile creates the first active profile when no active profile exists") {
            // given
            val teamId = TeamId(1L)
            val ownerUserId = UserId(10L)
            every { teamReader.getActiveByUserId(ownerUserId) } returns matchEnabledTeam(teamId, ownerUserId)
            every { matchCampaignReader.findActiveServiceProfile(teamId) } returns null
            every { matchCampaignReader.getNextServiceProfileVersion(teamId) } returns 1
            every { matchCampaignWriter.saveServiceProfile(any()) } answers {
                firstArg<ServiceProfile>().copy(id = 6L)
            }

            // when
            val response = serviceProfilePivotService.replaceActiveProfile(
                serviceProfileCreateRequest(ownerUserId),
            )

            // then
            response.serviceProfileId shouldBe 6L
            response.active shouldBe true
            response.name shouldBe "New Service"
            verify(exactly = 1) { matchCampaignWriter.saveServiceProfile(any()) }
        }

        test("replaceActiveProfile rejects a team with Match disabled") {
            // given
            val ownerUserId = UserId(10L)
            every { teamReader.getActiveByUserId(ownerUserId) } returns matchDisabledTeam(
                teamId = TeamId(1L),
                ownerUserId = ownerUserId,
            )

            // when
            val exception = shouldThrow<MatchDomainException> {
                serviceProfilePivotService.replaceActiveProfile(
                    serviceProfileCreateRequest(ownerUserId),
                )
            }

            // then
            exception.errorMessage shouldBe MatchErrorMessage.MATCH_SUB_SERVICE_DISABLED
            verify(exactly = 0) { matchCampaignWriter.saveServiceProfile(any()) }
        }
    }) {
    companion object {
        private val baseInstant: Instant = Instant.parse("2026-05-09T23:00:00Z")

        private fun serviceProfileCreateRequest(ownerUserId: UserId): ServiceProfileCreateRequest {
            return ServiceProfileCreateRequest(
                actorUserId = ownerUserId,
                name = "New Service",
                summary = "New summary",
                description = "New description",
                category = CampaignCategory.PRODUCTIVITY,
                platforms = listOf(Platform.WEB),
                screenshotUrls = emptyList(),
                demoUrl = null,
                isPublic = true,
            )
        }

        private fun matchEnabledTeam(
            teamId: TeamId,
            ownerUserId: UserId,
        ): Team {
            val team = matchDisabledTeam(teamId, ownerUserId).changeSubServiceActivation(
                subService = SubService.MATCH,
                enabled = true,
                actorUserId = ownerUserId,
                occurredAt = baseInstant.plusSeconds(60),
            )

            return team.copy(
                subServiceActivation = SubServiceActivation(
                    calendarEnabled = false,
                    matchEnabled = true,
                    matchEnabledAt = baseInstant.plusSeconds(60),
                ),
            )
        }

        private fun matchDisabledTeam(
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
            )

            return team.copy(
                id = teamId,
                members = team.members.map { it.copy(id = TeamMemberId(1L), teamId = teamId) },
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
