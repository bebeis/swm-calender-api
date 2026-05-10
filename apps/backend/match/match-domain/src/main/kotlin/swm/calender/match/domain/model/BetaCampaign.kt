package swm.calender.match.domain.model

import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.CampaignStatus
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import java.time.Instant
import java.time.OffsetDateTime

data class BetaCampaign(
    val id: CampaignId? = null,
    val teamId: TeamId,
    val serviceProfileId: Long,
    val title: String,
    val description: String,
    val targetTeamCount: Int,
    val deadline: OffsetDateTime,
    val reciprocalAvailable: Boolean,
    val requirements: String? = null,
    val status: CampaignStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        validateTitle(title)
        validateDescription(description)
        validateTargetTeamCount(targetTeamCount)
    }

    fun changeStatus(
        status: CampaignStatus,
        now: Instant,
    ): BetaCampaign {
        if (status == CampaignStatus.OPEN && !deadline.toInstant().isAfter(now)) {
            throw MatchDomainException(MatchErrorMessage.CAMPAIGN_DEADLINE_MUST_BE_FUTURE)
        }

        return copy(status = status, updatedAt = now)
    }

    companion object {
        fun createOpen(
            teamId: TeamId,
            serviceProfileId: Long,
            title: String,
            description: String,
            targetTeamCount: Int,
            deadline: OffsetDateTime,
            reciprocalAvailable: Boolean,
            requirements: String?,
            createdAt: Instant,
        ): BetaCampaign {
            if (!deadline.toInstant().isAfter(createdAt)) {
                throw MatchDomainException(MatchErrorMessage.CAMPAIGN_DEADLINE_MUST_BE_FUTURE)
            }

            return BetaCampaign(
                teamId = teamId,
                serviceProfileId = serviceProfileId,
                title = title.trim(),
                description = description.trim(),
                targetTeamCount = targetTeamCount,
                deadline = deadline,
                reciprocalAvailable = reciprocalAvailable,
                requirements = requirements?.trim()?.takeIf { it.isNotEmpty() },
                status = CampaignStatus.OPEN,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }

        private fun validateTitle(title: String) {
            if (title.isBlank()) {
                throw MatchDomainException(MatchErrorMessage.CAMPAIGN_TITLE_REQUIRED)
            }
        }

        private fun validateDescription(description: String) {
            if (description.isBlank()) {
                throw MatchDomainException(MatchErrorMessage.CAMPAIGN_DESCRIPTION_REQUIRED)
            }
        }

        private fun validateTargetTeamCount(targetTeamCount: Int) {
            if (targetTeamCount <= 0) {
                throw MatchDomainException(MatchErrorMessage.CAMPAIGN_TARGET_TEAM_COUNT_INVALID)
            }
        }
    }
}
