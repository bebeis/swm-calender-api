package swm.calender.match.domain

import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.Platform
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.domain.model.ServiceProfile

interface MatchCampaignRepository {
    fun saveServiceProfile(serviceProfile: ServiceProfile): ServiceProfile

    fun findActiveServiceProfileByTeamId(teamId: TeamId): ServiceProfile?

    fun countServiceProfilesByTeamId(teamId: TeamId): Int

    fun saveCampaign(campaign: BetaCampaign): BetaCampaign

    fun findCampaignById(campaignId: CampaignId): BetaCampaign?

    fun searchOpenCampaigns(filter: CampaignSearchFilter): List<CampaignSearchResult>

    fun findReleasedServiceProfiles(): List<ReleasedServiceProfile>
}

data class CampaignSearchFilter(
    val category: CampaignCategory? = null,
    val platform: Platform? = null,
    val reciprocalAvailable: Boolean? = null,
    val sort: CampaignSearchSort = CampaignSearchSort.LATEST,
)

enum class CampaignSearchSort {
    LATEST,
    DEADLINE,
}

data class CampaignSearchResult(
    val campaign: BetaCampaign,
    val teamName: String,
    val serviceProfile: ServiceProfile,
)

data class ReleasedServiceProfile(
    val serviceProfile: ServiceProfile,
    val openCampaignDescriptions: List<String>,
)
