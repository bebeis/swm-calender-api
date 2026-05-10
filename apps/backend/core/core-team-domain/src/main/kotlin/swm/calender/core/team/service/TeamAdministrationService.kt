package swm.calender.core.team.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import swm.calender.core.team.domain.model.TeamMemberHistory
import swm.calender.core.team.implement.TeamReader
import swm.calender.core.team.implement.TeamWriter
import swm.calender.core.team.service.request.TeamMemberRemovalRequest
import swm.calender.core.team.service.request.TeamMemberRoleChangeRequest
import swm.calender.core.team.service.response.TeamMemberResponse
import java.time.Clock
import java.time.Instant

@Service
class TeamAdministrationService(
    private val teamReader: TeamReader,
    private val teamWriter: TeamWriter,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Transactional
    fun changeMemberRole(request: TeamMemberRoleChangeRequest): TeamMemberResponse {
        val team = teamReader.getById(request.teamId)
        val targetMember = team.getMember(request.memberId)
        val occurredAt = now()
        val updatedTeam = team.changeMemberRole(
            memberId = request.memberId,
            role = request.role,
            actorUserId = request.actorUserId,
            occurredAt = occurredAt,
        )
        val savedTeam = teamWriter.save(updatedTeam)

        teamWriter.saveMemberHistory(
            TeamMemberHistory.roleChanged(
                teamId = request.teamId,
                memberId = request.memberId,
                actorUserId = request.actorUserId,
                previousRole = targetMember.role,
                changedRole = request.role,
                occurredAt = occurredAt,
            ),
        )

        return TeamMemberResponse.from(savedTeam.getMember(request.memberId))
    }

    @Transactional
    fun removeMember(request: TeamMemberRemovalRequest): TeamMemberResponse {
        val team = teamReader.getById(request.teamId)
        val targetMember = team.getMember(request.memberId)
        val occurredAt = now()
        val updatedTeam = team.removeMember(
            memberId = request.memberId,
            actorUserId = request.actorUserId,
            occurredAt = occurredAt,
        )
        val savedTeam = teamWriter.save(updatedTeam)

        teamWriter.saveMemberHistory(
            TeamMemberHistory.memberRemoved(
                teamId = request.teamId,
                memberId = request.memberId,
                actorUserId = request.actorUserId,
                previousRole = targetMember.role,
                occurredAt = occurredAt,
            ),
        )

        return TeamMemberResponse.from(savedTeam.getMember(request.memberId))
    }

    private fun now(): Instant = Instant.now(clock)
}
