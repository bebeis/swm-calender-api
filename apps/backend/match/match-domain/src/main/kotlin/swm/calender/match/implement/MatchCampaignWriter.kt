package swm.calender.match.implement

import org.springframework.stereotype.Component
import swm.calender.match.domain.MatchCampaignRepository
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.domain.model.ServiceProfile

@Component
class MatchCampaignWriter(
    private val matchCampaignRepository: MatchCampaignRepository,
) {
    fun saveServiceProfile(serviceProfile: ServiceProfile): ServiceProfile {
        return matchCampaignRepository.saveServiceProfile(serviceProfile)
    }

    fun saveCampaign(campaign: BetaCampaign): BetaCampaign {
        return matchCampaignRepository.saveCampaign(campaign)
    }
}
