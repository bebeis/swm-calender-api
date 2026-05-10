package swm.calender.core.team.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.TeamMemberRole
import swm.calender.core.team.domain.model.SubServiceActivation
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.domain.model.TeamMember
import swm.calender.core.team.domain.model.TeamMemberHistory
import swm.calender.core.team.domain.model.TeamMemberHistoryAction
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import swm.calender.core.team.implement.TeamReader
import swm.calender.core.team.implement.TeamWriter
import swm.calender.core.team.service.request.TeamMemberRemovalRequest
import swm.calender.core.team.service.request.TeamMemberRoleChangeRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TeamAdministrationServiceTest :
    FunSpec({
        val fixedInstant = Instant.parse("2026-05-10T00:00:00Z")
        val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

        lateinit var teamReader: TeamReader
        lateinit var teamWriter: TeamWriter
        lateinit var teamAdministrationService: TeamAdministrationService

        beforeTest {
            teamReader = mockk()
            teamWriter = mockk()
            teamAdministrationService = TeamAdministrationService(
                teamReader = teamReader,
                teamWriter = teamWriter,
                clock = fixedClock,
            )
        }

        test("changeMemberRole persists the role change and records history") {
            // given
            val teamId = TeamId(1L)
            val memberId = TeamMemberId(2L)
            val historySlot = slot<TeamMemberHistory>()
            every { teamReader.getById(teamId) } returns teamWithOwnerAndMember(teamId)
            every { teamWriter.save(any()) } answers { firstArg() }
            every { teamWriter.saveMemberHistory(capture(historySlot)) } answers {
                historySlot.captured.copy(id = 10L)
            }

            // when
            val response = teamAdministrationService.changeMemberRole(
                TeamMemberRoleChangeRequest(
                    teamId = teamId,
                    memberId = memberId,
                    actorUserId = UserId(1L),
                    role = TeamMemberRole.OWNER,
                ),
            )

            // then
            response.memberId shouldBe memberId.value
            response.role shouldBe TeamMemberRole.OWNER
            historySlot.captured.action shouldBe TeamMemberHistoryAction.ROLE_CHANGED
            historySlot.captured.previousRole shouldBe TeamMemberRole.MEMBER
            historySlot.captured.changedRole shouldBe TeamMemberRole.OWNER
            historySlot.captured.occurredAt shouldBe fixedInstant
            verify(exactly = 1) { teamWriter.save(any()) }
            verify(exactly = 1) { teamWriter.saveMemberHistory(any()) }
        }

        test("removeMember marks the member removed and records history") {
            // given
            val teamId = TeamId(1L)
            val memberId = TeamMemberId(2L)
            val historySlot = slot<TeamMemberHistory>()
            every { teamReader.getById(teamId) } returns teamWithOwnerAndMember(teamId)
            every { teamWriter.save(any()) } answers { firstArg() }
            every { teamWriter.saveMemberHistory(capture(historySlot)) } answers {
                historySlot.captured.copy(id = 11L)
            }

            // when
            val response = teamAdministrationService.removeMember(
                TeamMemberRemovalRequest(
                    teamId = teamId,
                    memberId = memberId,
                    actorUserId = UserId(1L),
                ),
            )

            // then
            response.memberId shouldBe memberId.value
            response.role shouldBe TeamMemberRole.MEMBER
            historySlot.captured.action shouldBe TeamMemberHistoryAction.MEMBER_REMOVED
            historySlot.captured.previousRole shouldBe TeamMemberRole.MEMBER
            historySlot.captured.changedRole shouldBe null
            historySlot.captured.occurredAt shouldBe fixedInstant
            verify(exactly = 1) {
                teamWriter.save(
                    match { it.getMember(memberId).removedAt == fixedInstant },
                )
            }
            verify(exactly = 1) { teamWriter.saveMemberHistory(any()) }
        }

        test("removeMember rejects removing the last owner without writing history") {
            // given
            val teamId = TeamId(1L)
            every { teamReader.getById(teamId) } returns teamWithOwnerAndMember(teamId)

            // when
            val exception = shouldThrow<TeamDomainException> {
                teamAdministrationService.removeMember(
                    TeamMemberRemovalRequest(
                        teamId = teamId,
                        memberId = TeamMemberId(1L),
                        actorUserId = UserId(1L),
                    ),
                )
            }

            // then
            exception.errorMessage shouldBe TeamErrorMessage.TEAM_ACTIVE_OWNER_REQUIRED
            verify(exactly = 0) { teamWriter.save(any()) }
            verify(exactly = 0) { teamWriter.saveMemberHistory(any()) }
        }
    }) {
    companion object {
        private val baseInstant: Instant = Instant.parse("2026-05-09T23:00:00Z")

        private fun teamWithOwnerAndMember(teamId: TeamId): Team {
            return Team(
                id = teamId,
                name = "Team Alpha",
                description = null,
                inviteCode = "TEAM-ALPHA",
                members = listOf(
                    TeamMember.createOwner(
                        userId = UserId(1L),
                        name = "Owner",
                        email = "owner@swm.app",
                        joinedAt = baseInstant,
                        teamId = teamId,
                    ).copy(id = TeamMemberId(1L)),
                    TeamMember.createMember(
                        userId = UserId(2L),
                        name = "Member",
                        email = "member@swm.app",
                        joinedAt = baseInstant.plusSeconds(60),
                        teamId = teamId,
                    ).copy(id = TeamMemberId(2L)),
                ),
                subServiceActivation = SubServiceActivation.inactive(),
                createdAt = baseInstant,
                updatedAt = baseInstant,
            )
        }
    }
}
