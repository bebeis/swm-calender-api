package swm.calender.match.domain

import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.TeamId
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.Feedback

data class TeamTestHistoryItem(
    val assignment: Assignment,
    val campaignId: CampaignId,
    val serviceName: String,
    val feedback: Feedback?,
)

interface FeedbackRepository {
    fun save(feedback: Feedback): Feedback

    fun findByAssignmentId(assignmentId: AssignmentId): Feedback?

    fun existsByAssignmentId(assignmentId: AssignmentId): Boolean

    fun findTeamTestHistoryByTeamId(teamId: TeamId): List<TeamTestHistoryItem>
}
