package swm.calender.core.team.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.SubService
import swm.calender.core.team.domain.model.SubServiceActivation
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.domain.model.TeamMember
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import swm.calender.core.team.implement.TeamReader
import swm.calender.core.team.implement.TeamWriter
import swm.calender.core.team.service.request.TeamCreateRequest
import swm.calender.core.team.service.request.TeamJoinRequest
import swm.calender.core.team.service.request.TeamSubServiceActivationRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TeamServiceTest :
    FunSpec({
        val fixedInstant = Instant.parse("2026-05-10T00:00:00Z")
        val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

        lateinit var teamReader: TeamReader
        lateinit var teamWriter: TeamWriter
        lateinit var inviteCodeGenerator: TeamInviteCodeGenerator
        lateinit var teamService: TeamService

        beforeTest {
            teamReader = mockk()
            teamWriter = mockk()
            inviteCodeGenerator = mockk()
            teamService = TeamService(
                teamReader = teamReader,
                teamWriter = teamWriter,
                teamInviteCodeGenerator = inviteCodeGenerator,
                clock = fixedClock,
            )
        }

        test("createTeam saves a new team with generated invite code") {
            // given
            val ownerUserId = UserId(10L)
            every { teamReader.ensureUserHasNoActiveTeam(ownerUserId) } just Runs
            every { inviteCodeGenerator.generate() } returns "INVITE123456"
            every { teamWriter.save(any()) } answers {
                firstArg<Team>().persisted(teamId = 1L, memberIdStart = 100L)
            }

            // when
            val response = teamService.createTeam(
                TeamCreateRequest(
                    ownerUserId = ownerUserId,
                    ownerName = "Owner",
                    ownerEmail = "owner@swm.app",
                    name = "Team Rocket",
                    description = "Prepare for trouble",
                ),
            )

            // then
            response.teamId shouldBe 1L
            response.name shouldBe "Team Rocket"
            response.inviteCode shouldBe "INVITE123456"
            response.calendarEnabled shouldBe false
            response.matchEnabled shouldBe false
            verify(exactly = 1) { teamReader.ensureUserHasNoActiveTeam(ownerUserId) }
            verify(exactly = 1) { inviteCodeGenerator.generate() }
            verify(exactly = 1) { teamWriter.save(any()) }
        }

        test("createTeam rejects a user who already belongs to an active team") {
            // given
            val ownerUserId = UserId(10L)
            every { teamReader.ensureUserHasNoActiveTeam(ownerUserId) } throws TeamDomainException(
                TeamErrorMessage.TEAM_ALREADY_EXISTS_FOR_USER,
            )

            // when
            val exception = shouldThrow<TeamDomainException> {
                teamService.createTeam(
                    TeamCreateRequest(
                        ownerUserId = ownerUserId,
                        ownerName = "Owner",
                        ownerEmail = "owner@swm.app",
                        name = "Team Rocket",
                        description = null,
                    ),
                )
            }

            // then
            exception.errorMessage shouldBe TeamErrorMessage.TEAM_ALREADY_EXISTS_FOR_USER
            verify(exactly = 0) { inviteCodeGenerator.generate() }
            verify(exactly = 0) { teamWriter.save(any()) }
        }

        test("joinTeam loads by invite code and persists the appended member") {
            // given
            val userId = UserId(20L)
            val existingTeam = team(
                teamId = 1L,
                ownerUserId = 10L,
                inviteCode = "INVITE123456",
            )
            every { teamReader.ensureUserHasNoActiveTeam(userId) } just Runs
            every { teamReader.getByInviteCode("INVITE123456") } returns existingTeam
            every { teamWriter.save(any()) } answers {
                firstArg<Team>().persisted(teamId = 1L, memberIdStart = 100L)
            }

            // when
            val response = teamService.joinTeam(
                TeamJoinRequest(
                    userId = userId,
                    name = "Member",
                    email = "member@swm.app",
                    inviteCode = "INVITE123456",
                ),
            )

            // then
            response.teamId shouldBe 1L
            response.inviteCode shouldBe "INVITE123456"
            verify(exactly = 1) { teamReader.ensureUserHasNoActiveTeam(userId) }
            verify(exactly = 1) { teamReader.getByInviteCode("INVITE123456") }
            verify(exactly = 1) { teamWriter.save(match { it.members.size == 2 }) }
        }

        test("changeSubServiceActivation persists the targeted service state only") {
            // given
            val teamId = TeamId(1L)
            val actorUserId = UserId(10L)
            val existingTeam = team(
                teamId = teamId.value,
                ownerUserId = actorUserId.value,
                inviteCode = "INVITE123456",
            )
            every { teamReader.getById(teamId) } returns existingTeam
            every { teamWriter.save(any()) } answers { firstArg() }

            // when
            val response = teamService.changeSubServiceActivation(
                TeamSubServiceActivationRequest(
                    teamId = teamId,
                    actorUserId = actorUserId,
                    subService = SubService.CALENDAR,
                    enabled = true,
                ),
            )

            // then
            response.teamId shouldBe 1L
            response.calendarEnabled shouldBe true
            response.matchEnabled shouldBe false
            verify(exactly = 1) { teamReader.getById(teamId) }
            verify(exactly = 1) {
                teamWriter.save(
                    match {
                        it.subServiceActivation.calendarEnabled &&
                            !it.subServiceActivation.matchEnabled
                    },
                )
            }
        }
    }) {
    companion object {
        private val baseInstant: Instant = Instant.parse("2026-05-09T23:00:00Z")

        private fun team(
            teamId: Long,
            ownerUserId: Long,
            inviteCode: String,
        ): Team {
            return Team(
                id = TeamId(teamId),
                name = "Team Rocket",
                description = "Prepare for trouble",
                inviteCode = inviteCode,
                members = listOf(
                    TeamMember.createOwner(
                        userId = UserId(ownerUserId),
                        name = "Owner",
                        email = "owner@swm.app",
                        joinedAt = baseInstant,
                        teamId = TeamId(teamId),
                    ).copy(id = TeamMemberId(1L)),
                ),
                subServiceActivation = SubServiceActivation.inactive(),
                createdAt = baseInstant,
                updatedAt = baseInstant,
            )
        }

        private fun Team.persisted(
            teamId: Long,
            memberIdStart: Long,
        ): Team {
            val persistedTeamId = TeamId(teamId)

            return copy(
                id = persistedTeamId,
                members = members.mapIndexed { index, member ->
                    member.copy(
                        id = member.id ?: TeamMemberId(memberIdStart + index),
                        teamId = persistedTeamId,
                    )
                },
            )
        }
    }
}
