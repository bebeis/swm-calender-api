package swm.calender.match.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType
import swm.calender.match.domain.model.MatchRequest
import swm.calender.match.exception.MatchDomainException
import java.time.Instant

class MatchRequestTest :
    FunSpec({
        val baseInstant = Instant.parse("2026-05-10T00:00:00Z")

        test("createPending trims message and starts as pending") {
            // given
            val message = "  We can test this week.  "

            // when
            val request = MatchRequest.createPending(
                campaignId = CampaignId(1L),
                requestingTeamId = TeamId(2L),
                targetTeamId = TeamId(3L),
                type = MatchRequestType.ONE_WAY,
                message = message,
                createdAt = baseInstant,
            )

            // then
            request.status shouldBe MatchRequestStatus.PENDING
            request.message shouldBe "We can test this week."
        }

        test("pending request can move to accepted") {
            // given
            val request = pendingRequest(baseInstant)

            // when
            val acceptedRequest = request.accept(baseInstant.plusSeconds(60))

            // then
            acceptedRequest.status shouldBe MatchRequestStatus.ACCEPTED
            acceptedRequest.updatedAt shouldBe baseInstant.plusSeconds(60)
        }

        test("final request status cannot be changed again") {
            // given
            val acceptedRequest = pendingRequest(baseInstant).accept(baseInstant.plusSeconds(60))

            // when & then
            shouldThrow<MatchDomainException> {
                acceptedRequest.reject(baseInstant.plusSeconds(120))
            }
        }

        test("requesting team cannot equal target team") {
            // when & then
            shouldThrow<MatchDomainException> {
                MatchRequest.createPending(
                    campaignId = CampaignId(1L),
                    requestingTeamId = TeamId(2L),
                    targetTeamId = TeamId(2L),
                    type = MatchRequestType.ONE_WAY,
                    message = null,
                    createdAt = baseInstant,
                )
            }
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
