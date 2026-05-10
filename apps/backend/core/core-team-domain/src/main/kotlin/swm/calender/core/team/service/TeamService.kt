package swm.calender.core.team.service

import org.springframework.stereotype.Service
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.TeamMemberRole
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

    fun changeMemberRole(
        teamId: TeamId,
        memberId: TeamMemberId,
        role: TeamMemberRole,
        actorUserId: UserId,
    ): TeamMemberResponse {
        val team = teamReader.getById(teamId)
            .changeMemberRole(
                memberId = memberId,
                role = role,
                actorUserId = actorUserId,
                occurredAt = now(),
            )

        return TeamMemberResponse.from(
            teamWriter.save(team)
                .members
                .single { it.id == memberId },
        )
    }

    private fun now(): Instant = Instant.now(clock)
}
