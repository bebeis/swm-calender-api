package swm.calender.storage.db.core.match

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CandidateIdeaVisibility
import swm.calender.core.enums.Platform
import swm.calender.storage.db.core.team.TeamTable

object CandidateIdeaTable : Table("candidate_idea") {
    val id = long("id").autoIncrement()
    val teamId = long("team_id").references(TeamTable.id)
    val title = varchar("idea_title", 100)
    val summary = varchar("summary", 300)
    val problem = text("problem")
    val targetUsers = text("target_users")
    val solution = text("solution")
    val category = enumerationByName<CampaignCategory>("category", 30)
    val visibility = enumerationByName<CandidateIdeaVisibility>("visibility", 20)
    val createdByUserId = long("created_by_user_id")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        index("ix_candidate_idea_team_id_created_at", false, teamId, createdAt, id)
    }

    override val primaryKey = PrimaryKey(id)
}

object CandidateIdeaPlatformTable : Table("candidate_idea_platform") {
    val candidateIdeaId = long("candidate_idea_id").references(CandidateIdeaTable.id)
    val platform = enumerationByName<Platform>("platform", 30)
    val sortOrder = integer("sort_order")

    override val primaryKey = PrimaryKey(candidateIdeaId, platform)
}

internal fun ResultRow.toCandidateIdeaEntity(
    platforms: List<Platform>,
): CandidateIdeaEntity = CandidateIdeaEntity(
    id = this[CandidateIdeaTable.id],
    teamId = this[CandidateIdeaTable.teamId],
    title = this[CandidateIdeaTable.title],
    summary = this[CandidateIdeaTable.summary],
    problem = this[CandidateIdeaTable.problem],
    targetUsers = this[CandidateIdeaTable.targetUsers],
    solution = this[CandidateIdeaTable.solution],
    category = this[CandidateIdeaTable.category],
    platforms = platforms,
    visibility = this[CandidateIdeaTable.visibility],
    createdByUserId = this[CandidateIdeaTable.createdByUserId],
    createdAt = this[CandidateIdeaTable.createdAt],
    updatedAt = this[CandidateIdeaTable.updatedAt],
)
