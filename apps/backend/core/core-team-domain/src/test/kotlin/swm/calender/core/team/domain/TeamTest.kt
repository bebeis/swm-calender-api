package swm.calender.core.team.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.SubService
import swm.calender.core.enums.TeamMemberRole
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import java.time.Instant

class TeamTest :
    FunSpec({
        test("team creation creates an owner member and disables both sub-services") {
            // given
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")

            // when
            val team = Team.create(
                name = "Team Rocket",
                description = " Steals ideas responsibly ",
                inviteCode = "INVITE123",
                ownerUserId = UserId(1L),
                ownerName = "Owner",
                ownerEmail = "owner@swm.app",
                createdAt = createdAt,
            )

            // then
            team.name shouldBe "Team Rocket"
            team.description shouldBe "Steals ideas responsibly"
            team.members shouldHaveSize 1
            team.members.single().role shouldBe TeamMemberRole.OWNER
            team.subServiceActivation.calendarEnabled.shouldBeFalse()
            team.subServiceActivation.matchEnabled.shouldBeFalse()
        }

        test("adding a new member appends an active MEMBER membership") {
            // given
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")
            val joinedAt = Instant.parse("2026-05-10T01:00:00Z")
            val team = Team.create(
                name = "Team Rocket",
                description = null,
                inviteCode = "INVITE123",
                ownerUserId = UserId(1L),
                ownerName = "Owner",
                ownerEmail = "owner@swm.app",
                createdAt = createdAt,
            )

            // when
            val updatedTeam = team.addMember(
                userId = UserId(2L),
                name = "Member",
                email = "member@swm.app",
                joinedAt = joinedAt,
            )

            // then
            updatedTeam.members shouldHaveSize 2
            updatedTeam.members.last().role shouldBe TeamMemberRole.MEMBER
            updatedTeam.members.last().isActive().shouldBeTrue()
            updatedTeam.updatedAt shouldBe joinedAt
        }

        test("adding the same active member twice is rejected") {
            // given
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")
            val joinedTeam = Team.create(
                name = "Team Rocket",
                description = null,
                inviteCode = "INVITE123",
                ownerUserId = UserId(1L),
                ownerName = "Owner",
                ownerEmail = "owner@swm.app",
                createdAt = createdAt,
            ).addMember(
                userId = UserId(2L),
                name = "Member",
                email = "member@swm.app",
                joinedAt = Instant.parse("2026-05-10T01:00:00Z"),
            )

            // when
            val exception = shouldThrow<TeamDomainException> {
                joinedTeam.addMember(
                    userId = UserId(2L),
                    name = "Member",
                    email = "member@swm.app",
                    joinedAt = Instant.parse("2026-05-10T02:00:00Z"),
                )
            }

            // then
            exception.errorMessage shouldBe TeamErrorMessage.TEAM_MEMBER_ALREADY_EXISTS
        }

        test("owner can toggle one sub-service without changing the other") {
            // given
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")
            val team = Team.create(
                name = "Team Rocket",
                description = null,
                inviteCode = "INVITE123",
                ownerUserId = UserId(1L),
                ownerName = "Owner",
                ownerEmail = "owner@swm.app",
                createdAt = createdAt,
            )

            // when
            val updatedTeam = team.changeSubServiceActivation(
                subService = SubService.CALENDAR,
                enabled = true,
                actorUserId = UserId(1L),
                occurredAt = Instant.parse("2026-05-10T03:00:00Z"),
            )

            // then
            updatedTeam.subServiceActivation.calendarEnabled.shouldBeTrue()
            updatedTeam.subServiceActivation.matchEnabled.shouldBeFalse()
            updatedTeam.subServiceActivation.calendarEnabledAt shouldBe Instant.parse("2026-05-10T03:00:00Z")
        }

        test("non-owner cannot change a sub-service activation") {
            // given
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")
            val team = Team.create(
                name = "Team Rocket",
                description = null,
                inviteCode = "INVITE123",
                ownerUserId = UserId(1L),
                ownerName = "Owner",
                ownerEmail = "owner@swm.app",
                createdAt = createdAt,
            ).addMember(
                userId = UserId(2L),
                name = "Member",
                email = "member@swm.app",
                joinedAt = Instant.parse("2026-05-10T01:00:00Z"),
            )

            // when
            val exception = shouldThrow<TeamDomainException> {
                team.changeSubServiceActivation(
                    subService = SubService.MATCH,
                    enabled = true,
                    actorUserId = UserId(2L),
                    occurredAt = Instant.parse("2026-05-10T04:00:00Z"),
                )
            }

            // then
            exception.errorMessage shouldBe TeamErrorMessage.TEAM_OWNER_REQUIRED
        }

        test("owner can remove an active member while preserving membership history state") {
            // given
            val removedAt = Instant.parse("2026-05-10T05:00:00Z")
            val team = persistedTeamWithOwnerAndMember()

            // when
            val updatedTeam = team.removeMember(
                memberId = TeamMemberId(2L),
                actorUserId = UserId(1L),
                occurredAt = removedAt,
            )

            // then
            updatedTeam.getMember(TeamMemberId(2L)).isActive().shouldBeFalse()
            updatedTeam.getMember(TeamMemberId(2L)).removedAt shouldBe removedAt
            updatedTeam.members.single { it.isActiveOwner() }.userId shouldBe UserId(1L)
            updatedTeam.updatedAt shouldBe removedAt
        }

        test("removing the last active owner is rejected") {
            // given
            val team = persistedTeamWithOwnerAndMember()

            // when
            val exception = shouldThrow<TeamDomainException> {
                team.removeMember(
                    memberId = TeamMemberId(1L),
                    actorUserId = UserId(1L),
                    occurredAt = Instant.parse("2026-05-10T05:00:00Z"),
                )
            }

            // then
            exception.errorMessage shouldBe TeamErrorMessage.TEAM_ACTIVE_OWNER_REQUIRED
        }

        test("changing a member role to the same value is treated as an existing member update") {
            // given
            val changedAt = Instant.parse("2026-05-10T05:00:00Z")
            val team = persistedTeamWithOwnerAndMember()

            // when
            val updatedTeam = team.changeMemberRole(
                memberId = TeamMemberId(2L),
                role = TeamMemberRole.MEMBER,
                actorUserId = UserId(1L),
                occurredAt = changedAt,
            )

            // then
            updatedTeam.getMember(TeamMemberId(2L)).role shouldBe TeamMemberRole.MEMBER
            updatedTeam.updatedAt shouldBe changedAt
        }
    })

private fun persistedTeamWithOwnerAndMember(): Team {
    val createdAt = Instant.parse("2026-05-10T00:00:00Z")
    val teamId = TeamId(1L)
    return Team.create(
        name = "Team Rocket",
        description = null,
        inviteCode = "INVITE123",
        ownerUserId = UserId(1L),
        ownerName = "Owner",
        ownerEmail = "owner@swm.app",
        createdAt = createdAt,
    ).addMember(
        userId = UserId(2L),
        name = "Member",
        email = "member@swm.app",
        joinedAt = createdAt.plusSeconds(60),
    ).let { team ->
        team.copy(
            id = teamId,
            members = team.members.mapIndexed { index, member ->
                member.copy(
                    id = TeamMemberId(index + 1L),
                    teamId = teamId,
                )
            },
        )
    }
}
