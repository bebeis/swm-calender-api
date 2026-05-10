package swm.calender.core.team.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.implement.TeamReader
import swm.calender.core.team.implement.TeamWriter
import swm.calender.core.team.service.request.TeamCreateRequest
import swm.calender.core.team.service.request.TeamJoinRequest
import swm.calender.core.team.service.request.TeamSubServiceActivationRequest
import swm.calender.core.team.service.response.SubServiceActivationResponse
import swm.calender.core.team.service.response.TeamMemberResponse
import swm.calender.core.team.service.response.TeamResponse
import java.time.Clock
import java.time.Instant

@Service
class TeamService(
    private val teamReader: TeamReader,
    private val teamWriter: TeamWriter,
    private val teamInviteCodeGenerator: TeamInviteCodeGenerator,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Transactional
    fun createTeam(request: TeamCreateRequest): TeamResponse {
        teamReader.ensureUserHasNoActiveTeam(request.ownerUserId)

        val createdTeam = Team.create(
            name = request.name,
            description = request.description,
            inviteCode = teamInviteCodeGenerator.generate(),
            ownerUserId = request.ownerUserId,
            ownerName = request.ownerName,
            ownerEmail = request.ownerEmail,
            createdAt = now(),
        )

        return TeamResponse.from(teamWriter.save(createdTeam))
    }

    @Transactional
    fun joinTeam(request: TeamJoinRequest): TeamResponse {
        teamReader.ensureUserHasNoActiveTeam(request.userId)

        val joinedTeam = teamReader.getByInviteCode(request.inviteCode)
            .addMember(
                userId = request.userId,
                name = request.name,
                email = request.email,
                joinedAt = now(),
            )

        return TeamResponse.from(teamWriter.save(joinedTeam))
    }

    @Transactional
    fun changeSubServiceActivation(request: TeamSubServiceActivationRequest): SubServiceActivationResponse {
        val team = teamReader.getById(request.teamId)
            .changeSubServiceActivation(
                subService = request.subService,
                enabled = request.enabled,
                actorUserId = request.actorUserId,
                occurredAt = now(),
            )

        return SubServiceActivationResponse.from(teamWriter.save(team))
    }

    @Transactional(readOnly = true)
    fun getMembers(
        teamId: TeamId,
        actorUserId: UserId,
    ): List<TeamMemberResponse> {
        val team = teamReader.getById(teamId)
        team.requireMember(actorUserId)
        return team.members
            .filter { it.isActive() }
            .map(TeamMemberResponse::from)
    }

    private fun now(): Instant = Instant.now(clock)
}
