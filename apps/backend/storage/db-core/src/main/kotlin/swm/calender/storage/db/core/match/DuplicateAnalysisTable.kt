package swm.calender.storage.db.core.match

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import swm.calender.core.enums.DuplicateAnalysisSourceType
import swm.calender.core.enums.DuplicateAnalysisStatus
import swm.calender.core.enums.OverlapDimension
import swm.calender.core.enums.SimilarityLevel
import swm.calender.core.enums.SourceDisclosure
import swm.calender.storage.db.core.team.TeamTable

object DuplicateAnalysisTable : Table("duplicate_analysis") {
    val id = long("id").autoIncrement()
    val candidateIdeaId = long("candidate_idea_id").references(CandidateIdeaTable.id)
    val requestedByTeamId = long("requested_by_team_id").references(TeamTable.id)
    val requestedByUserId = long("requested_by_user_id")
    val status = enumerationByName<DuplicateAnalysisStatus>("analysis_status", 20)
    val scannedReleasedServiceCount = integer("scanned_released_service_count")
    val scannedCandidateIdeaCount = integer("scanned_candidate_idea_count")
    val failureReason = varchar("failure_reason", 500).nullable()
    val generatedAt = datetime("generated_at")

    override val primaryKey = PrimaryKey(id)
}

object DuplicateAnalysisMatchTable : Table("duplicate_analysis_match") {
    val id = long("id").autoIncrement()
    val analysisId = long("analysis_id").references(DuplicateAnalysisTable.id)
    val matchOrder = integer("match_order")
    val sourceType = enumerationByName<DuplicateAnalysisSourceType>("source_type", 40)
    val sourceId = long("source_id").nullable()
    val sourceTeamId = long("source_team_id").nullable()
    val sourceTitle = varchar("source_title", 200).nullable()
    val sourceDisclosure = enumerationByName<SourceDisclosure>("source_disclosure", 20)
    val similarityLevel = enumerationByName<SimilarityLevel>("similarity_level", 20)
    val overlapSummary = varchar("overlap_summary", 1000)

    init {
        uniqueIndex("ux_duplicate_analysis_match_analysis_id_match_order", analysisId, matchOrder)
    }

    override val primaryKey = PrimaryKey(id)
}

object DuplicateAnalysisMatchDimensionTable : Table("duplicate_analysis_match_dimension") {
    val matchId = long("match_id").references(DuplicateAnalysisMatchTable.id)
    val dimension = enumerationByName<OverlapDimension>("dimension", 30)
    val sortOrder = integer("sort_order")

    override val primaryKey = PrimaryKey(matchId, dimension)
}

internal fun ResultRow.toDuplicateAnalysisEntity(
    matches: List<DuplicateAnalysisMatchEntity>,
): DuplicateAnalysisEntity = DuplicateAnalysisEntity(
    id = this[DuplicateAnalysisTable.id],
    candidateIdeaId = this[DuplicateAnalysisTable.candidateIdeaId],
    requestedByTeamId = this[DuplicateAnalysisTable.requestedByTeamId],
    requestedByUserId = this[DuplicateAnalysisTable.requestedByUserId],
    status = this[DuplicateAnalysisTable.status],
    scannedReleasedServiceCount = this[DuplicateAnalysisTable.scannedReleasedServiceCount],
    scannedCandidateIdeaCount = this[DuplicateAnalysisTable.scannedCandidateIdeaCount],
    matches = matches,
    failureReason = this[DuplicateAnalysisTable.failureReason],
    generatedAt = this[DuplicateAnalysisTable.generatedAt],
)

internal fun ResultRow.toDuplicateAnalysisMatchEntity(
    dimensions: List<OverlapDimension>,
): DuplicateAnalysisMatchEntity = DuplicateAnalysisMatchEntity(
    id = this[DuplicateAnalysisMatchTable.id],
    sourceType = this[DuplicateAnalysisMatchTable.sourceType],
    sourceId = this[DuplicateAnalysisMatchTable.sourceId],
    sourceTeamId = this[DuplicateAnalysisMatchTable.sourceTeamId],
    sourceTitle = this[DuplicateAnalysisMatchTable.sourceTitle],
    sourceDisclosure = this[DuplicateAnalysisMatchTable.sourceDisclosure],
    similarityLevel = this[DuplicateAnalysisMatchTable.similarityLevel],
    overlapDimensions = dimensions,
    overlapSummary = this[DuplicateAnalysisMatchTable.overlapSummary],
)
