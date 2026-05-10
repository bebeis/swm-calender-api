package swm.calender.match.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.Feedback
import swm.calender.match.domain.model.FeedbackScores
import swm.calender.match.exception.MatchDomainException
import java.time.Instant

class FeedbackTest :
    FunSpec({
        val baseInstant = Instant.parse("2026-05-10T00:00:00Z")

        test("submit creates feedback for assigned tester team") {
            // given
            val assignment = assignedAssignment(baseInstant)
            val scores = FeedbackScores(
                usability = 5,
                value = 4,
                reliability = 5,
                recommendation = 4,
            )

            // when
            val feedback = Feedback.submit(
                assignment = assignment,
                submittedByTeamId = TeamId(1L),
                submittedByUserId = UserId(10L),
                scores = scores,
                summary = "The service was useful during testing.",
                improvementSuggestion = "Add onboarding.",
                submittedAt = baseInstant.plusSeconds(60),
            )

            // then
            feedback.assignmentId shouldBe AssignmentId(31L)
            feedback.submittedByTeamId shouldBe TeamId(1L)
            feedback.scores.value shouldBe 4
        }

        test("score must be between one and five") {
            // when & then
            shouldThrow<MatchDomainException> {
                FeedbackScores(
                    usability = 0,
                    value = 4,
                    reliability = 5,
                    recommendation = 4,
                )
            }
        }

        test("summary length is required") {
            // when & then
            shouldThrow<MatchDomainException> {
                Feedback.submit(
                    assignment = assignedAssignment(baseInstant),
                    submittedByTeamId = TeamId(1L),
                    submittedByUserId = UserId(10L),
                    scores = validScores(),
                    summary = "short",
                    improvementSuggestion = null,
                    submittedAt = baseInstant.plusSeconds(60),
                )
            }
        }

        test("only tester team can submit feedback") {
            // when & then
            shouldThrow<MatchDomainException> {
                Feedback.submit(
                    assignment = assignedAssignment(baseInstant),
                    submittedByTeamId = TeamId(2L),
                    submittedByUserId = UserId(20L),
                    scores = validScores(),
                    summary = "The service was useful during testing.",
                    improvementSuggestion = null,
                    submittedAt = baseInstant.plusSeconds(60),
                )
            }
        }
    }) {
    companion object {
        private fun validScores(): FeedbackScores {
            return FeedbackScores(
                usability = 5,
                value = 4,
                reliability = 5,
                recommendation = 4,
            )
        }

        private fun assignedAssignment(createdAt: Instant): Assignment {
            return Assignment(
                id = AssignmentId(31L),
                requestId = RequestId(11L),
                testerTeamId = TeamId(1L),
                targetTeamId = TeamId(2L),
                status = AssignmentStatus.ASSIGNED,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }
    }
}
