package swm.calender.match.implement

import org.springframework.stereotype.Component
import swm.calender.match.domain.FeedbackRepository
import swm.calender.match.domain.model.Feedback

@Component
class FeedbackWriter(
    private val feedbackRepository: FeedbackRepository,
) {
    fun save(feedback: Feedback): Feedback {
        return feedbackRepository.save(feedback)
    }
}
