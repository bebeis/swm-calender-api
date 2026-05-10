package swm.calender.match.implement

import org.springframework.stereotype.Component
import swm.calender.match.domain.MatchRequestRepository
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.MatchRequest
import swm.calender.match.domain.model.MatchRequestStatusHistory
import swm.calender.match.domain.model.Notification

@Component
class MatchRequestWriter(
    private val matchRequestRepository: MatchRequestRepository,
) {
    fun saveRequest(matchRequest: MatchRequest): MatchRequest {
        return matchRequestRepository.saveRequest(matchRequest)
    }

    fun saveStatusHistory(history: MatchRequestStatusHistory): MatchRequestStatusHistory {
        return matchRequestRepository.saveStatusHistory(history)
    }

    fun saveAssignment(assignment: Assignment): Assignment {
        return matchRequestRepository.saveAssignment(assignment)
    }

    fun saveNotification(notification: Notification): Notification {
        return matchRequestRepository.saveNotification(notification)
    }
}
