package swm.calender.match.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.core.enums.MatchRequestType
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.MatchRequest
import swm.calender.match.exception.MatchDomainException
import java.time.Instant

class AssignmentTest :
    FunSpec({
        val baseInstant = Instant.parse("2026-05-10T00:00:00Z")

        test("createFrom creates assigned assignment from accepted request") {
            // given
            val acceptedRequest = pendingRequest(baseInstant).accept(baseInstant.plusSeconds(60))

            // when
            val assignment = Assignment.createFrom(acceptedRequest, baseInstant.plusSeconds(120))

            // then
            assignment.requestId shouldBe RequestId(10L)
            assignment.testerTeamId shouldBe TeamId(2L)
            assignment.targetTeamId shouldBe TeamId(3L)
            assignment.status shouldBe AssignmentStatus.ASSIGNED
        }

        test("assignment cannot be created from pending request") {
            // given
            val pendingRequest = pendingRequest(baseInstant)

            // when & then
            shouldThrow<MatchDomainException> {
                Assignment.createFrom(pendingRequest, baseInstant.plusSeconds(60))
            }
        }

        test("submitFeedback changes assigned assignment to feedback submitted") {
            // given
            val assignment = Assignment.createFrom(
                pendingRequest(baseInstant).accept(baseInstant.plusSeconds(60)),
                baseInstant.plusSeconds(120),
            ).copy(id = swm.calender.core.common.id.AssignmentId(31L))

            // when
            val changedAssignment = assignment.submitFeedback(baseInstant.plusSeconds(180))

            // then
            changedAssignment.status shouldBe AssignmentStatus.FEEDBACK_SUBMITTED
            changedAssignment.updatedAt shouldBe baseInstant.plusSeconds(180)
        }
    }) {
    companion object {
        private fun pendingRequest(createdAt: Instant): MatchRequest {
            return MatchRequest.createPending(
                campaignId = CampaignId(1L),
                requestingTeamId = TeamId(2L),
                targetTeamId = TeamId(3L),
                type = MatchRequestType.ONE_WAY,
                message = null,
                createdAt = createdAt,
            ).copy(id = RequestId(10L))
        }
    }
}
