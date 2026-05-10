package swm.calender.match.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import swm.calender.core.common.id.UserId
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.implement.MatchRequestReader
import swm.calender.match.service.response.NotificationResponse

@Service
class NotificationService(
    private val teamReader: TeamReader,
    private val matchRequestReader: MatchRequestReader,
) {
    @Transactional(readOnly = true)
    fun listNotifications(actorUserId: UserId): List<NotificationResponse> {
        val team = teamReader.getActiveByUserId(actorUserId)
        team.requireMember(actorUserId)

        return matchRequestReader.findNotificationsByTeamId(team.requireId())
            .map(NotificationResponse::from)
    }
}
