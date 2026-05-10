package swm.calender.match.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.Platform
import swm.calender.match.domain.model.ServiceProfile
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import java.time.Instant

class ServiceProfileTest :
    FunSpec({
        test("createActive creates a public active service profile with normalized fields") {
            // given
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")

            // when
            val serviceProfile = ServiceProfile.createActive(
                teamId = TeamId(1L),
                nextVersion = 1,
                isPublic = true,
                name = " Service ",
                summary = " Team service ",
                description = " Detailed description ",
                category = CampaignCategory.PRODUCTIVITY,
                platforms = listOf(Platform.WEB, Platform.WEB, Platform.API),
                screenshotUrls = listOf(" https://example.com/one.png ", ""),
                demoUrl = "https://example.com",
                createdAt = createdAt,
            )

            // then
            serviceProfile.active.shouldBeTrue()
            serviceProfile.isPublic.shouldBeTrue()
            serviceProfile.name shouldBe "Service"
            serviceProfile.platforms shouldBe listOf(Platform.WEB, Platform.API)
            serviceProfile.screenshotUrls shouldBe listOf("https://example.com/one.png")
        }

        test("archive deactivates an active profile without losing version history") {
            // given
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")
            val serviceProfile = ServiceProfile.createActive(
                teamId = TeamId(1L),
                nextVersion = 3,
                isPublic = true,
                name = "Service",
                summary = "Summary",
                description = "Description",
                category = CampaignCategory.PRODUCTIVITY,
                platforms = listOf(Platform.WEB),
                screenshotUrls = emptyList(),
                demoUrl = null,
                createdAt = createdAt,
            )

            // when
            val archivedProfile = serviceProfile.archive(Instant.parse("2026-05-10T01:00:00Z"))

            // then
            archivedProfile.active.shouldBeFalse()
            archivedProfile.version shouldBe 3
        }

        test("createActive rejects a public profile without platforms") {
            // when
            val exception = shouldThrow<MatchDomainException> {
                ServiceProfile.createActive(
                    teamId = TeamId(1L),
                    nextVersion = 1,
                    isPublic = true,
                    name = "Service",
                    summary = "Summary",
                    description = "Description",
                    category = CampaignCategory.PRODUCTIVITY,
                    platforms = emptyList(),
                    screenshotUrls = emptyList(),
                    demoUrl = null,
                    createdAt = Instant.parse("2026-05-10T00:00:00Z"),
                )
            }

            // then
            exception.errorMessage shouldBe MatchErrorMessage.SERVICE_PROFILE_PLATFORMS_REQUIRED
        }
    })
