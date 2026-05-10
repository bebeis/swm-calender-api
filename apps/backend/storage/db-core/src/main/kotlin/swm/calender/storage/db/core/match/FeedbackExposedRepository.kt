package swm.calender.storage.db.core.match

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.match.domain.FeedbackRepository
import swm.calender.match.domain.TeamTestHistoryItem
import swm.calender.match.domain.model.Feedback

@Repository
class FeedbackExposedRepository : FeedbackRepository {
    override fun save(feedback: Feedback): Feedback {
        val savedFeedbackId = feedback.id?.value?.takeIf { feedbackRowExists(it) }
            ?.also { updateFeedback(it, feedback) }
            ?: insertFeedback(feedback)

        return requireNotNull(findById(savedFeedbackId))
    }

    override fun findByAssignmentId(assignmentId: AssignmentId): Feedback? {
        return FeedbackTable
            .selectAll()
            .where { FeedbackTable.assignmentId eq assignmentId.value }
            .singleOrNull()
            ?.toFeedbackEntity()
            ?.toDomain()
    }

    override fun existsByAssignmentId(assignmentId: AssignmentId): Boolean {
        return FeedbackTable
            .selectAll()
            .where { FeedbackTable.assignmentId eq assignmentId.value }
            .limit(1)
            .any()
    }

    override fun findTeamTestHistoryByTeamId(teamId: TeamId): List<TeamTestHistoryItem> {
        val teamCondition = (AssignmentTable.testerTeamId eq teamId.value) or
            (AssignmentTable.targetTeamId eq teamId.value)
        val statusCondition = AssignmentTable.assignmentStatus inList HISTORY_ASSIGNMENT_STATUSES
        val rows = teamTestHistoryJoin()
            .selectAll()
            .where { teamCondition and statusCondition }
            .orderBy(
                AssignmentTable.updatedAt to SortOrder.DESC,
                AssignmentTable.id to SortOrder.DESC,
            )
            .toList()
        if (rows.isEmpty()) {
            return emptyList()
        }

        val assignmentIds = rows.map { it[AssignmentTable.id] }
        val feedbacksByAssignmentId = loadFeedbacksByAssignmentId(assignmentIds)
        return rows.map { row ->
            val assignment = row.toAssignmentEntity().toDomain()
            TeamTestHistoryItem(
                assignment = assignment,
                campaignId = CampaignId(row[MatchRequestTable.campaignId]),
                serviceName = row[ServiceProfileTable.serviceName],
                feedback = feedbacksByAssignmentId[assignment.requireId().value],
            )
        }
    }

    private fun insertFeedback(feedback: Feedback): Long {
        return FeedbackTable.insert {
            it[assignmentId] = feedback.assignmentId.value
            it[submittedByTeamId] = feedback.submittedByTeamId.value
            it[submittedByUserId] = feedback.submittedByUserId.value
            it[usabilityScore] = feedback.scores.usability
            it[valueScore] = feedback.scores.value
            it[reliabilityScore] = feedback.scores.reliability
            it[recommendationScore] = feedback.scores.recommendation
            it[summary] = feedback.summary
            it[improvementSuggestion] = feedback.improvementSuggestion
            it[submittedAt] = feedback.submittedAt.toLocalDateTime()
        }[FeedbackTable.id]
    }

    private fun updateFeedback(
        feedbackId: Long,
        feedback: Feedback,
    ) {
        FeedbackTable.update(
            where = { FeedbackTable.id eq feedbackId },
        ) {
            it[assignmentId] = feedback.assignmentId.value
            it[submittedByTeamId] = feedback.submittedByTeamId.value
            it[submittedByUserId] = feedback.submittedByUserId.value
            it[usabilityScore] = feedback.scores.usability
            it[valueScore] = feedback.scores.value
            it[reliabilityScore] = feedback.scores.reliability
            it[recommendationScore] = feedback.scores.recommendation
            it[summary] = feedback.summary
            it[improvementSuggestion] = feedback.improvementSuggestion
            it[submittedAt] = feedback.submittedAt.toLocalDateTime()
        }
    }

    private fun findById(feedbackId: Long): Feedback? {
        return FeedbackTable
            .selectAll()
            .where { FeedbackTable.id eq feedbackId }
            .singleOrNull()
            ?.toFeedbackEntity()
            ?.toDomain()
    }

    private fun feedbackRowExists(feedbackId: Long): Boolean {
        return FeedbackTable
            .selectAll()
            .where { FeedbackTable.id eq feedbackId }
            .limit(1)
            .any()
    }

    private fun loadFeedbacksByAssignmentId(assignmentIds: Collection<Long>): Map<Long, Feedback> {
        if (assignmentIds.isEmpty()) {
            return emptyMap()
        }

        return FeedbackTable
            .selectAll()
            .where { FeedbackTable.assignmentId inList assignmentIds.distinct() }
            .associate { row ->
                val feedback = row.toFeedbackEntity().toDomain()
                feedback.assignmentId.value to feedback
            }
    }

    private fun teamTestHistoryJoin() = AssignmentTable
        .join(
            otherTable = MatchRequestTable,
            joinType = JoinType.INNER,
            onColumn = AssignmentTable.requestId,
            otherColumn = MatchRequestTable.id,
        )
        .join(
            otherTable = BetaCampaignTable,
            joinType = JoinType.INNER,
            onColumn = MatchRequestTable.campaignId,
            otherColumn = BetaCampaignTable.id,
        )
        .join(
            otherTable = ServiceProfileTable,
            joinType = JoinType.INNER,
            onColumn = BetaCampaignTable.serviceProfileId,
            otherColumn = ServiceProfileTable.id,
        )

    companion object {
        private val HISTORY_ASSIGNMENT_STATUSES = listOf(
            AssignmentStatus.FEEDBACK_SUBMITTED,
            AssignmentStatus.CLOSED,
        )
    }
}
