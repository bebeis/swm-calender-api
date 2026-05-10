package swm.calender.match.domain.model

import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.Platform
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import java.net.URI
import java.time.Instant

data class ServiceProfile(
    val id: Long? = null,
    val teamId: TeamId,
    val version: Int,
    val active: Boolean,
    val isPublic: Boolean,
    val name: String,
    val summary: String,
    val description: String,
    val category: CampaignCategory,
    val platforms: List<Platform>,
    val screenshotUrls: List<String> = emptyList(),
    val demoUrl: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        validateName(name)
        validateSummary(summary)
        validateDescription(description)
        validatePlatforms(platforms)
        demoUrl?.let(::validateUrl)
        screenshotUrls.forEach(::validateUrl)
    }

    fun archive(archivedAt: Instant): ServiceProfile {
        return copy(active = false, updatedAt = archivedAt)
    }

    companion object {
        fun createActive(
            teamId: TeamId,
            nextVersion: Int,
            isPublic: Boolean,
            name: String,
            summary: String,
            description: String,
            category: CampaignCategory,
            platforms: List<Platform>,
            screenshotUrls: List<String>,
            demoUrl: String?,
            createdAt: Instant,
        ): ServiceProfile {
            return ServiceProfile(
                teamId = teamId,
                version = nextVersion,
                active = true,
                isPublic = isPublic,
                name = name.trim(),
                summary = summary.trim(),
                description = description.trim(),
                category = category,
                platforms = platforms.distinct(),
                screenshotUrls = screenshotUrls.map(String::trim).filter(String::isNotEmpty),
                demoUrl = demoUrl?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }

        private fun validateName(name: String) {
            if (name.isBlank()) {
                throw MatchDomainException(MatchErrorMessage.SERVICE_PROFILE_NAME_REQUIRED)
            }
        }

        private fun validateSummary(summary: String) {
            if (summary.isBlank()) {
                throw MatchDomainException(MatchErrorMessage.SERVICE_PROFILE_SUMMARY_REQUIRED)
            }
        }

        private fun validateDescription(description: String) {
            if (description.isBlank()) {
                throw MatchDomainException(MatchErrorMessage.SERVICE_PROFILE_DESCRIPTION_REQUIRED)
            }
        }

        private fun validatePlatforms(platforms: List<Platform>) {
            if (platforms.isEmpty()) {
                throw MatchDomainException(MatchErrorMessage.SERVICE_PROFILE_PLATFORMS_REQUIRED)
            }
        }

        private fun validateUrl(url: String) {
            val parsed = runCatching { URI(url) }.getOrNull()
            if (parsed == null || parsed.scheme !in setOf("http", "https") || parsed.host.isNullOrBlank()) {
                throw MatchDomainException(MatchErrorMessage.SERVICE_PROFILE_DEMO_URL_INVALID)
            }
        }
    }
}
