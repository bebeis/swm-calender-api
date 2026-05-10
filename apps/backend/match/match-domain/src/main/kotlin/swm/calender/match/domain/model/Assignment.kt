package swm.calender.match.domain.model

import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import java.time.Instant

data class Assignment(
    val id: AssignmentId? = null,
    val requestId: RequestId,
    val testerTeamId: TeamId,
    val targetTeamId: TeamId,
    val status: AssignmentStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        if (testerTeamId == targetTeamId) {
            throw MatchDomainException(MatchErrorMessage.ASSIGNMENT_SELF_ASSIGNMENT_NOT_ALLOWED)
        }
    }

    fun submitFeedback(submittedAt: Instant): Assignment {
        if (status != AssignmentStatus.ASSIGNED) {
            throw MatchDomainException(MatchErrorMessage.ASSIGNMENT_FEEDBACK_UNAVAILABLE)
        }

        return copy(
            status = AssignmentStatus.FEEDBACK_SUBMITTED,
            updatedAt = submittedAt,
        )
    }

    fun requireId(): AssignmentId {
        return id ?: throw MatchDomainException(MatchErrorMessage.ASSIGNMENT_NOT_FOUND)
    }

    companion object {
        fun createFrom(
            request: MatchRequest,
            createdAt: Instant,
        ): Assignment {
            if (request.status != MatchRequestStatus.ACCEPTED) {
                throw MatchDomainException(MatchErrorMessage.ASSIGNMENT_REQUEST_NOT_ACCEPTED)
            }

            return Assignment(
                requestId = request.requireId(),
                testerTeamId = request.requestingTeamId,
                targetTeamId = request.targetTeamId,
                status = AssignmentStatus.ASSIGNED,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }
    }
}
