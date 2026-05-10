package swm.calender.match.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.CampaignStatus
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import java.time.Instant
import java.time.OffsetDateTime

class BetaCampaignTest :
    FunSpec({
        test("createOpen creates an open beta campaign with a future deadline") {
            // given
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")

            // when
            val campaign = BetaCampaign.createOpen(
                teamId = TeamId(1L),
                serviceProfileId = 10L,
                title = " Beta Campaign ",
                description = " Test our service ",
                targetTeamCount = 3,
                deadline = OffsetDateTime.parse("2026-05-20T00:00:00Z"),
                reciprocalAvailable = true,
                requirements = " Chrome ",
                createdAt = createdAt,
            )

            // then
            campaign.status shouldBe CampaignStatus.OPEN
            campaign.title shouldBe "Beta Campaign"
            campaign.requirements shouldBe "Chrome"
        }

        test("opening a campaign with an expired deadline is rejected") {
            // when
            val exception = shouldThrow<MatchDomainException> {
                BetaCampaign.createOpen(
                    teamId = TeamId(1L),
                    serviceProfileId = 10L,
                    title = "Campaign",
                    description = "Description",
                    targetTeamCount = 3,
                    deadline = OffsetDateTime.parse("2026-05-09T00:00:00Z"),
                    reciprocalAvailable = true,
                    requirements = null,
                    createdAt = Instant.parse("2026-05-10T00:00:00Z"),
                )
            }

            // then
            exception.errorMessage shouldBe MatchErrorMessage.CAMPAIGN_DEADLINE_MUST_BE_FUTURE
        }

        test("closed campaign can reopen only when the deadline remains valid") {
            // given
            val campaign = BetaCampaign(
                id = CampaignId(1L),
                teamId = TeamId(1L),
                serviceProfileId = 10L,
                title = "Campaign",
                description = "Description",
                targetTeamCount = 3,
                deadline = OffsetDateTime.parse("2026-05-20T00:00:00Z"),
                reciprocalAvailable = false,
                status = CampaignStatus.CLOSED,
                createdAt = Instant.parse("2026-05-10T00:00:00Z"),
                updatedAt = Instant.parse("2026-05-10T00:00:00Z"),
            )

            // when
            val reopenedCampaign = campaign.changeStatus(
                status = CampaignStatus.OPEN,
                now = Instant.parse("2026-05-11T00:00:00Z"),
            )

            // then
            reopenedCampaign.status shouldBe CampaignStatus.OPEN
        }
    })
