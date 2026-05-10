package swm.calender.storage.db.core.team

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.beans.factory.annotation.Autowired
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.SubService
import swm.calender.core.enums.TeamMemberRole
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.domain.model.TeamMemberHistory
import swm.calender.core.team.domain.model.TeamMemberHistoryAction
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import swm.calender.storage.db.core.RepositoryTestSupport
import swm.calender.storage.db.core.calendar.AvailabilitySlotTable
import swm.calender.storage.db.core.calendar.MentoringScheduleTable
import swm.calender.storage.db.core.calendar.TeamCalendarTable
import swm.calender.storage.db.core.calendar.When2meetLinkTable
import java.time.Instant

class TeamExposedRepositoryIT : RepositoryTestSupport() {
    @Autowired
    private lateinit var teamExposedRepository: TeamExposedRepository

    init {
        extension(SpringExtension())

        val createdAt = Instant.parse("2026-05-10T00:00:00Z")

        beforeTest {
            transaction {
                AvailabilitySlotTable.deleteAll()
                MentoringScheduleTable.deleteAll()
                When2meetLinkTable.deleteAll()
                TeamCalendarTable.deleteAll()
                TeamMemberHistoryTable.deleteAll()
                TeamMemberTable.deleteAll()
                SubServiceActivationTable.deleteAll()
                TeamTable.deleteAll()
            }
        }

        test("save stores team, owner membership, and disabled sub-service defaults") {
            // given
            val team = Team.create(
                ownerUserId = UserId(101L),
                ownerName = "Owner Alpha",
                ownerEmail = "owner-alpha@swm.app",
                name = "Team Alpha",
                description = "First team",
                inviteCode = "TEAM-ALPHA",
                createdAt = createdAt,
            )

            // when
            val savedTeam = teamExposedRepository.save(team)

            // then
            savedTeam.name shouldBe "Team Alpha"
            savedTeam.description shouldBe "First team"
            savedTeam.inviteCode shouldBe "TEAM-ALPHA"
            savedTeam.subServiceActivation.calendarEnabled.shouldBeFalse()
            savedTeam.subServiceActivation.matchEnabled.shouldBeFalse()
            savedTeam.subServiceActivation.calendarEnabledAt.shouldBeNull()
            savedTeam.subServiceActivation.matchEnabledAt.shouldBeNull()
            savedTeam.members.shouldHaveSize(1)
            savedTeam.members.single().userId shouldBe UserId(101L)
            savedTeam.members.single().role shouldBe TeamMemberRole.OWNER
        }

        test("findByInviteCode returns the persisted team aggregate") {
            // given
            val savedTeam = teamExposedRepository.save(
                Team.create(
                    ownerUserId = UserId(102L),
                    ownerName = "Owner Bravo",
                    ownerEmail = "owner-bravo@swm.app",
                    name = "Team Bravo",
                    description = null,
                    inviteCode = "BRAVO-CODE",
                    createdAt = createdAt,
                ),
            )

            // when
            val foundTeam = teamExposedRepository.findByInviteCode("BRAVO-CODE")

            // then
            foundTeam shouldBe savedTeam
        }

        test("findActiveByUserId returns the active team for a member") {
            // given
            val savedTeam = teamExposedRepository.save(
                Team.create(
                    ownerUserId = UserId(103L),
                    ownerName = "Owner India",
                    ownerEmail = "owner-india@swm.app",
                    name = "Team India",
                    description = null,
                    inviteCode = "INDIA-CODE",
                    createdAt = createdAt,
                ).addMember(
                    userId = UserId(104L),
                    name = "Member India",
                    email = "member-india@swm.app",
                    joinedAt = createdAt.plusSeconds(60),
                ),
            )

            // when
            val foundTeam = teamExposedRepository.findActiveByUserId(UserId(104L))

            // then
            foundTeam shouldBe savedTeam
        }

        test("save rejects a second active membership for the same user") {
            // given
            val firstTeam = teamExposedRepository.save(
                Team.create(
                    ownerUserId = UserId(201L),
                    ownerName = "Owner Charlie",
                    ownerEmail = "owner-charlie@swm.app",
                    name = "Team Charlie",
                    description = null,
                    inviteCode = "CHARLIE-CODE",
                    createdAt = createdAt,
                ),
            )
            teamExposedRepository.save(
                Team.create(
                    ownerUserId = UserId(202L),
                    ownerName = "Owner Delta",
                    ownerEmail = "owner-delta@swm.app",
                    name = "Team Delta",
                    description = null,
                    inviteCode = "DELTA-CODE",
                    createdAt = createdAt.plusSeconds(60),
                ),
            )
            val joinedTeam = firstTeam.addMember(
                userId = UserId(999L),
                name = "Existing Member",
                email = "existing-member@swm.app",
                joinedAt = createdAt.plusSeconds(120),
            )
            teamExposedRepository.save(joinedTeam)

            // when
            val exception = shouldThrow<TeamDomainException> {
                teamExposedRepository.save(
                    Team.create(
                        ownerUserId = UserId(999L),
                        ownerName = "Duplicate Owner",
                        ownerEmail = "duplicate-owner@swm.app",
                        name = "Team Echo",
                        description = null,
                        inviteCode = "ECHO-CODE",
                        createdAt = createdAt.plusSeconds(180),
                    ),
                )
            }

            // then
            exception.errorMessage shouldBe TeamErrorMessage.TEAM_ALREADY_EXISTS_FOR_USER
        }

        test("save persists member role changes") {
            // given
            val savedTeam = teamExposedRepository.save(
                Team.create(
                    ownerUserId = UserId(301L),
                    ownerName = "Owner Foxtrot",
                    ownerEmail = "owner-foxtrot@swm.app",
                    name = "Team Foxtrot",
                    description = null,
                    inviteCode = "FOXTROT-CODE",
                    createdAt = createdAt,
                ).addMember(
                    userId = UserId(302L),
                    name = "Member Foxtrot",
                    email = "member-foxtrot@swm.app",
                    joinedAt = createdAt.plusSeconds(60),
                ),
            )
            val memberId = requireNotNull(savedTeam.members.last().id)

            // when
            val updatedTeam = savedTeam.changeMemberRole(
                memberId = memberId,
                role = TeamMemberRole.OWNER,
                actorUserId = UserId(301L),
                occurredAt = createdAt.plusSeconds(120),
            )
            val persistedTeam = teamExposedRepository.save(updatedTeam)

            // then
            persistedTeam.members.last().role shouldBe TeamMemberRole.OWNER
        }

        test("save persists member removal and team member history") {
            // given
            val savedTeam = teamExposedRepository.save(
                Team.create(
                    ownerUserId = UserId(501L),
                    ownerName = "Owner Hotel",
                    ownerEmail = "owner-hotel@swm.app",
                    name = "Team Hotel",
                    description = null,
                    inviteCode = "HOTEL-CODE",
                    createdAt = createdAt,
                ).addMember(
                    userId = UserId(502L),
                    name = "Member Hotel",
                    email = "member-hotel@swm.app",
                    joinedAt = createdAt.plusSeconds(60),
                ),
            )
            val memberId = requireNotNull(savedTeam.members.last().id)
            val removedAt = createdAt.plusSeconds(120)

            // when
            val removedTeam = savedTeam.removeMember(
                memberId = memberId,
                actorUserId = UserId(501L),
                occurredAt = removedAt,
            )
            val persistedTeam = teamExposedRepository.save(removedTeam)
            val savedHistory = teamExposedRepository.saveMemberHistory(
                TeamMemberHistory.memberRemoved(
                    teamId = persistedTeam.requireId(),
                    memberId = memberId,
                    actorUserId = UserId(501L),
                    previousRole = TeamMemberRole.MEMBER,
                    occurredAt = removedAt,
                ),
            )
            val histories = teamExposedRepository.findMemberHistoriesByTeamId(persistedTeam.requireId())

            // then
            persistedTeam.members.last().removedAt shouldBe removedAt
            savedHistory.id shouldBe histories.single().id
            histories.single().action shouldBe TeamMemberHistoryAction.MEMBER_REMOVED
            histories.single().previousRole shouldBe TeamMemberRole.MEMBER
            histories.single().changedRole.shouldBeNull()
        }

        test("save toggles calendar and match independently") {
            // given
            val savedTeam = teamExposedRepository.save(
                Team.create(
                    ownerUserId = UserId(401L),
                    ownerName = "Owner Golf",
                    ownerEmail = "owner-golf@swm.app",
                    name = "Team Golf",
                    description = null,
                    inviteCode = "GOLF-CODE",
                    createdAt = createdAt,
                ),
            )

            // when
            val calendarEnabledTeam = teamExposedRepository.save(
                savedTeam.changeSubServiceActivation(
                    subService = SubService.CALENDAR,
                    enabled = true,
                    actorUserId = UserId(401L),
                    occurredAt = createdAt.plusSeconds(300),
                ),
            )
            val fullyUpdatedTeam = teamExposedRepository.save(
                calendarEnabledTeam.changeSubServiceActivation(
                    subService = SubService.MATCH,
                    enabled = true,
                    actorUserId = UserId(401L),
                    occurredAt = createdAt.plusSeconds(360),
                ),
            )

            // then
            calendarEnabledTeam.subServiceActivation.calendarEnabled.shouldBeTrue()
            calendarEnabledTeam.subServiceActivation.matchEnabled.shouldBeFalse()
            calendarEnabledTeam.subServiceActivation.calendarEnabledAt shouldBe createdAt.plusSeconds(300)
            fullyUpdatedTeam.subServiceActivation.calendarEnabled.shouldBeTrue()
            fullyUpdatedTeam.subServiceActivation.matchEnabled.shouldBeTrue()
            fullyUpdatedTeam.subServiceActivation.matchEnabledAt shouldBe createdAt.plusSeconds(360)
        }
    }
}
