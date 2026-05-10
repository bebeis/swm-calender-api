package swm.calender.storage.db.core.match

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import swm.calender.storage.db.core.team.TeamTable

object FeedbackTable : Table("match_feedback") {
    val id = long("id").autoIncrement()
    val assignmentId = long("assignment_id").references(AssignmentTable.id).uniqueIndex()
    val submittedByTeamId = long("submitted_by_team_id").references(TeamTable.id)
    val submittedByUserId = long("submitted_by_user_id")
    val usabilityScore = integer("usability_score")
    val valueScore = integer("value_score")
    val reliabilityScore = integer("reliability_score")
    val recommendationScore = integer("recommendation_score")
    val summary = varchar("summary", 1000)
    val improvementSuggestion = varchar("improvement_suggestion", 1000).nullable()
    val submittedAt = datetime("submitted_at")

    init {
        index("ix_match_feedback_submitted_by_team_submitted_at", false, submittedByTeamId, submittedAt)
    }

    override val primaryKey = PrimaryKey(id)
}

internal fun ResultRow.toFeedbackEntity(): FeedbackEntity = FeedbackEntity(
    id = this[FeedbackTable.id],
    assignmentId = this[FeedbackTable.assignmentId],
    submittedByTeamId = this[FeedbackTable.submittedByTeamId],
    submittedByUserId = this[FeedbackTable.submittedByUserId],
    usabilityScore = this[FeedbackTable.usabilityScore],
    valueScore = this[FeedbackTable.valueScore],
    reliabilityScore = this[FeedbackTable.reliabilityScore],
    recommendationScore = this[FeedbackTable.recommendationScore],
    summary = this[FeedbackTable.summary],
    improvementSuggestion = this[FeedbackTable.improvementSuggestion],
    submittedAt = this[FeedbackTable.submittedAt],
)
