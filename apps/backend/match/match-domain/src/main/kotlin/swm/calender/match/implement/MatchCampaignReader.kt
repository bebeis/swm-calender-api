package swm.calender.match.implement

import org.springframework.stereotype.Component
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.TeamId
import swm.calender.match.domain.CampaignSearchFilter
import swm.calender.match.domain.CampaignSearchResult
import swm.calender.match.domain.MatchCampaignRepository
import swm.calender.match.domain.ReleasedServiceProfile
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.domain.model.ServiceProfile
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage

@Component
class MatchCampaignReader(
    private val matchCampaignRepository: MatchCampaignRepository,
) {
    fun getActiveServiceProfile(teamId: TeamId): ServiceProfile {
        return matchCampaignRepository.findActiveServiceProfileByTeamId(teamId)
            ?: throw MatchDomainException(MatchErrorMessage.SERVICE_PROFILE_NOT_FOUND)
    }

    fun getNextServiceProfileVersion(teamId: TeamId): Int {
        return matchCampaignRepository.countServiceProfilesByTeamId(teamId) + 1
    }

    fun getCampaign(campaignId: CampaignId): BetaCampaign {
        return matchCampaignRepository.findCampaignById(campaignId)
            ?: throw MatchDomainException(MatchErrorMessage.CAMPAIGN_NOT_FOUND)
    }

    fun getOpenPublicCampaign(campaignId: CampaignId): BetaCampaign {
        return matchCampaignRepository.findOpenPublicCampaignById(campaignId)
            ?: throw MatchDomainException(MatchErrorMessage.CAMPAIGN_NOT_FOUND)
    }

    fun hasOpenPublicCampaign(teamId: TeamId): Boolean {
        return matchCampaignRepository.existsOpenPublicCampaignByTeamId(teamId)
    }

    fun searchOpenCampaigns(filter: CampaignSearchFilter): List<CampaignSearchResult> {
        return matchCampaignRepository.searchOpenCampaigns(filter)
    }

    fun getReleasedServiceProfiles(): List<ReleasedServiceProfile> {
        return matchCampaignRepository.findReleasedServiceProfiles()
    }
}
