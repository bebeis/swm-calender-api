package swm.calender.match.service.response

import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.Platform
import swm.calender.match.domain.CampaignSearchResult
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.domain.model.ServiceProfile
import java.time.OffsetDateTime

data class ServiceProfileResponse(
    val serviceProfileId: Long,
    val teamId: TeamId,
    val active: Boolean,
    val isPublic: Boolean,
    val name: String,
    val summary: String,
    val category: CampaignCategory,
    val platforms: List<Platform>,
) {
    companion object {
        fun from(serviceProfile: ServiceProfile): ServiceProfileResponse {
            return ServiceProfileResponse(
                serviceProfileId = requireNotNull(serviceProfile.id),
                teamId = serviceProfile.teamId,
                active = serviceProfile.active,
                isPublic = serviceProfile.isPublic,
                name = serviceProfile.name,
                summary = serviceProfile.summary,
                category = serviceProfile.category,
                platforms = serviceProfile.platforms,
            )
        }
    }
}

data class CampaignResponse(
    val campaignId: Long,
    val serviceProfileId: Long,
    val title: String,
    val targetTeamCount: Int,
    val deadline: OffsetDateTime,
    val reciprocalAvailable: Boolean,
    val status: CampaignStatus,
) {
    companion object {
        fun from(campaign: BetaCampaign): CampaignResponse {
            return CampaignResponse(
                campaignId = requireNotNull(campaign.id).value,
                serviceProfileId = campaign.serviceProfileId,
                title = campaign.title,
                targetTeamCount = campaign.targetTeamCount,
                deadline = campaign.deadline,
                reciprocalAvailable = campaign.reciprocalAvailable,
                status = campaign.status,
            )
        }
    }
}

data class CampaignSearchItemResponse(
    val campaignId: Long,
    val teamId: TeamId,
    val teamName: String,
    val serviceName: String,
    val serviceSummary: String,
    val category: CampaignCategory,
    val platforms: List<Platform>,
    val reciprocalAvailable: Boolean,
    val deadline: OffsetDateTime,
    val status: CampaignStatus,
) {
    companion object {
        fun from(result: CampaignSearchResult): CampaignSearchItemResponse {
            return CampaignSearchItemResponse(
                campaignId = requireNotNull(result.campaign.id).value,
                teamId = result.campaign.teamId,
                teamName = result.teamName,
                serviceName = result.serviceProfile.name,
                serviceSummary = result.serviceProfile.summary,
                category = result.serviceProfile.category,
                platforms = result.serviceProfile.platforms,
                reciprocalAvailable = result.campaign.reciprocalAvailable,
                deadline = result.campaign.deadline,
                status = result.campaign.status,
            )
        }
    }
}
