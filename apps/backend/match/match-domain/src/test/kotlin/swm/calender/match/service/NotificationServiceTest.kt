package swm.calender.match.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.SubService
import swm.calender.core.team.domain.model.SubServiceActivation
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.domain.model.Notification
import swm.calender.match.implement.MatchRequestReader
import java.time.Instant

class NotificationServiceTest :
    FunSpec({
        lateinit var teamReader: TeamReader
        lateinit var matchRequestReader: MatchRequestReader
        lateinit var notificationService: NotificationService

        beforeTest {
            teamReader = mockk()
            matchRequestReader = mockk()
            notificationService = NotificationService(
                teamReader = teamReader,
                matchRequestReader = matchRequestReader,
            )
        }

        test("listNotifications returns active member team notifications ordered by repository") {
            // given
            val actorUserId = UserId(10L)
            val teamId = TeamId(1L)
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")
            every { teamReader.getActiveByUserId(actorUserId) } returns matchEnabledTeam(
                teamId = teamId,
                ownerUserId = actorUserId,
            )
            every { matchRequestReader.findNotificationsByTeamId(teamId) } returns listOf(
                Notification.requestAccepted(
                    teamId = teamId,
                    requestId = RequestId(11L),
                    createdAt = createdAt,
                ).copy(id = 21L),
            )

            // when
            val response = notificationService.listNotifications(actorUserId)

            // then
            response.single().notificationId shouldBe 21L
            response.single().read shouldBe false
            verify(exactly = 1) { matchRequestReader.findNotificationsByTeamId(teamId) }
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
