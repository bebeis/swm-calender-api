package swm.calender.match.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import swm.calender.core.common.id.UserId
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.domain.model.ServiceProfile
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import swm.calender.match.implement.MatchCampaignReader
import swm.calender.match.implement.MatchCampaignWriter
import swm.calender.match.service.request.ServiceProfileCreateRequest
import swm.calender.match.service.response.ServiceProfileResponse
import java.time.Clock
import java.time.Instant

@Service
class ServiceProfilePivotService(
    private val teamReader: TeamReader,
    private val matchCampaignReader: MatchCampaignReader,
    private val matchCampaignWriter: MatchCampaignWriter,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Transactional
    fun replaceActiveProfile(request: ServiceProfileCreateRequest): ServiceProfileResponse {
        val team = getMatchEnabledTeam(request.actorUserId)
        requireOwner(team, request.actorUserId)
        val teamId = team.requireId()
        val now = now()
        val existingActiveProfile = matchCampaignReader.findActiveServiceProfile(teamId)

        existingActiveProfile?.let { matchCampaignWriter.saveServiceProfile(it.archive(now)) }

        val savedProfile = matchCampaignWriter.saveServiceProfile(
            ServiceProfile.createActive(
                teamId = teamId,
                nextVersion = matchCampaignReader.getNextServiceProfileVersion(teamId),
                isPublic = request.isPublic,
                name = request.name,
                summary = request.summary,
                description = request.description,
                category = request.category,
                platforms = request.platforms,
                screenshotUrls = request.screenshotUrls,
                demoUrl = request.demoUrl,
                createdAt = now,
            ),
        )

        return ServiceProfileResponse.from(savedProfile)
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
