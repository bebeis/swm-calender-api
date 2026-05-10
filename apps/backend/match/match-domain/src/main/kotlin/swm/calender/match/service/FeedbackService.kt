package swm.calender.match.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import swm.calender.core.common.id.UserId
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.domain.model.Feedback
import swm.calender.match.domain.model.Notification
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import swm.calender.match.implement.FeedbackReader
import swm.calender.match.implement.FeedbackWriter
import swm.calender.match.implement.MatchRequestReader
import swm.calender.match.implement.MatchRequestWriter
import swm.calender.match.service.request.FeedbackSubmitRequest
import swm.calender.match.service.request.TeamTestHistoryGetRequest
import swm.calender.match.service.response.FeedbackResponse
import swm.calender.match.service.response.TeamTestHistoryResponse
import java.time.Clock
import java.time.Instant

@Service
class FeedbackService(
    private val teamReader: TeamReader,
    private val matchRequestReader: MatchRequestReader,
    private val matchRequestWriter: MatchRequestWriter,
    private val feedbackReader: FeedbackReader,
    private val feedbackWriter: FeedbackWriter,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Transactional
    fun submitFeedback(request: FeedbackSubmitRequest): FeedbackResponse {
        val actorTeam = getMatchEnabledTeam(request.actorUserId)
        val assignment = matchRequestReader.getAssignment(request.assignmentId)
        val actorTeamId = actorTeam.requireId()
        if (actorTeamId != assignment.testerTeamId) {
            throw MatchDomainException(MatchErrorMessage.ASSIGNMENT_NOT_FOUND)
        }

        feedbackReader.ensureNoFeedback(request.assignmentId)

        val now = now()
        val savedFeedback = feedbackWriter.save(
            Feedback.submit(
                assignment = assignment,
                submittedByTeamId = actorTeamId,
                submittedByUserId = request.actorUserId,
                scores = request.scores,
                summary = request.summary,
                improvementSuggestion = request.improvementSuggestion,
                submittedAt = now,
            ),
        )
        val updatedAssignment = assignment.submitFeedback(now)
        matchRequestWriter.saveAssignment(updatedAssignment)
        matchRequestWriter.saveNotification(
            Notification.feedbackSubmitted(
                teamId = assignment.targetTeamId,
                assignmentId = assignment.requireId(),
                createdAt = now,
            ),
        )

        return FeedbackResponse.from(savedFeedback)
    }

    @Transactional(readOnly = true)
    fun getTeamTestHistory(request: TeamTestHistoryGetRequest): TeamTestHistoryResponse {
        val actorTeam = getMatchEnabledTeam(request.actorUserId)
        return TeamTestHistoryResponse.from(feedbackReader.findTeamTestHistory(actorTeam.requireId()))
    }

    private fun getMatchEnabledTeam(actorUserId: UserId): Team {
        val team = teamReader.getActiveByUserId(actorUserId)
        team.requireMember(actorUserId)

        if (!team.subServiceActivation.matchEnabled) {
            throw MatchDomainException(MatchErrorMessage.MATCH_SUB_SERVICE_DISABLED)
        }

        return team
    }

    private fun now(): Instant = Instant.now(clock)
}
