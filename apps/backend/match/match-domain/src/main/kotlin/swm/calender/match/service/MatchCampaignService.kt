package swm.calender.match.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import swm.calender.core.common.id.UserId
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.domain.CampaignSearchFilter
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import swm.calender.match.implement.MatchCampaignReader
import swm.calender.match.implement.MatchCampaignWriter
import swm.calender.match.service.request.CampaignCreateRequest
import swm.calender.match.service.request.CampaignSearchRequest
import swm.calender.match.service.request.CampaignStatusChangeRequest
import swm.calender.match.service.request.ServiceProfileCreateRequest
import swm.calender.match.service.response.CampaignResponse
import swm.calender.match.service.response.CampaignSearchItemResponse
import swm.calender.match.service.response.ServiceProfileResponse
import java.time.Clock
import java.time.Instant

@Service
class MatchCampaignService(
    private val teamReader: TeamReader,
    private val matchCampaignReader: MatchCampaignReader,
    private val matchCampaignWriter: MatchCampaignWriter,
    private val serviceProfilePivotService: ServiceProfilePivotService,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun createServiceProfile(request: ServiceProfileCreateRequest): ServiceProfileResponse {
        return serviceProfilePivotService.replaceActiveProfile(request)
    }

    @Transactional
    fun createCampaign(request: CampaignCreateRequest): CampaignResponse {
        val team = getMatchEnabledTeam(request.actorUserId)
        requireOwner(team, request.actorUserId)
        val teamId = team.requireId()
        val activeProfile = matchCampaignReader.getActiveServiceProfile(teamId)

        val savedCampaign = matchCampaignWriter.saveCampaign(
            BetaCampaign.createOpen(
                teamId = teamId,
                serviceProfileId = requireNotNull(activeProfile.id),
                title = request.title,
                description = request.description,
                targetTeamCount = request.targetTeamCount,
                deadline = request.deadline,
                reciprocalAvailable = request.reciprocalAvailable,
                requirements = request.requirements,
                createdAt = now(),
            ),
        )

        return CampaignResponse.from(savedCampaign)
    }

    @Transactional
    fun changeCampaignStatus(request: CampaignStatusChangeRequest): CampaignResponse {
        val team = getMatchEnabledTeam(request.actorUserId)
        requireOwner(team, request.actorUserId)
        val campaign = matchCampaignReader.getCampaign(request.campaignId)
        if (campaign.teamId != team.requireId()) {
            throw MatchDomainException(MatchErrorMessage.CAMPAIGN_NOT_FOUND)
        }

        return CampaignResponse.from(
            matchCampaignWriter.saveCampaign(
                campaign.changeStatus(
                    status = request.status,
                    now = now(),
                ),
            ),
        )
    }

    @Transactional(readOnly = true)
    fun searchCampaigns(request: CampaignSearchRequest): List<CampaignSearchItemResponse> {
        return matchCampaignReader.searchOpenCampaigns(
            CampaignSearchFilter(
                category = request.category,
                platform = request.platform,
                reciprocalAvailable = request.reciprocalAvailable,
                sort = request.sort,
            ),
        ).map(CampaignSearchItemResponse::from)
    }

    private fun getMatchEnabledTeam(actorUserId: UserId): Team {
        val team = teamReader.getActiveByUserId(actorUserId)
        team.requireMember(actorUserId)

        if (!team.subServiceActivation.matchEnabled) {
            throw MatchDomainException(MatchErrorMessage.MATCH_SUB_SERVICE_DISABLED)
        }

        return team
    }

    private fun requireOwner(
        team: Team,
        actorUserId: UserId,
    ) {
        if (!team.isOwner(actorUserId)) {
            throw TeamDomainException(TeamErrorMessage.TEAM_OWNER_REQUIRED)
        }
    }

    private fun now(): Instant = Instant.now(clock)
}
