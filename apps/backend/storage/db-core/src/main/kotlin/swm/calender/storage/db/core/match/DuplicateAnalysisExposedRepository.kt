package swm.calender.storage.db.core.match

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import swm.calender.core.common.id.DuplicateAnalysisId
import swm.calender.core.enums.OverlapDimension
import swm.calender.match.domain.DuplicateAnalysisRepository
import swm.calender.match.domain.model.DuplicateAnalysis
import swm.calender.match.domain.model.DuplicateAnalysisMatch

@Repository
class DuplicateAnalysisExposedRepository : DuplicateAnalysisRepository {
    override fun save(duplicateAnalysis: DuplicateAnalysis): DuplicateAnalysis {
        val savedAnalysisId = duplicateAnalysis.id?.value?.takeIf { duplicateAnalysisRowExists(it) }
            ?.also { updateDuplicateAnalysis(it, duplicateAnalysis) }
            ?: insertDuplicateAnalysis(duplicateAnalysis)

        replaceMatches(savedAnalysisId, duplicateAnalysis.matches)

        return requireNotNull(findById(DuplicateAnalysisId(savedAnalysisId)))
    }

    override fun findById(duplicateAnalysisId: DuplicateAnalysisId): DuplicateAnalysis? {
        val row = DuplicateAnalysisTable
            .selectAll()
            .where { DuplicateAnalysisTable.id eq duplicateAnalysisId.value }
            .singleOrNull()
            ?: return null

        return row.toDuplicateAnalysisEntity(
            matches = loadMatchEntities(duplicateAnalysisId.value),
        ).toDomain()
    }

    private fun insertDuplicateAnalysis(duplicateAnalysis: DuplicateAnalysis): Long {
        return DuplicateAnalysisTable.insert {
            it[candidateIdeaId] = duplicateAnalysis.candidateIdeaId.value
            it[requestedByTeamId] = duplicateAnalysis.requestedByTeamId.value
            it[requestedByUserId] = duplicateAnalysis.requestedByUserId.value
            it[status] = duplicateAnalysis.status
            it[scannedReleasedServiceCount] = duplicateAnalysis.scannedReleasedServiceCount
            it[scannedCandidateIdeaCount] = duplicateAnalysis.scannedCandidateIdeaCount
            it[failureReason] = duplicateAnalysis.failureReason
            it[generatedAt] = duplicateAnalysis.generatedAt.toLocalDateTime()
        }[DuplicateAnalysisTable.id]
    }

    private fun updateDuplicateAnalysis(
        duplicateAnalysisId: Long,
        duplicateAnalysis: DuplicateAnalysis,
    ) {
        DuplicateAnalysisTable.update(
            where = { DuplicateAnalysisTable.id eq duplicateAnalysisId },
        ) {
            it[candidateIdeaId] = duplicateAnalysis.candidateIdeaId.value
            it[requestedByTeamId] = duplicateAnalysis.requestedByTeamId.value
            it[requestedByUserId] = duplicateAnalysis.requestedByUserId.value
            it[status] = duplicateAnalysis.status
            it[scannedReleasedServiceCount] = duplicateAnalysis.scannedReleasedServiceCount
            it[scannedCandidateIdeaCount] = duplicateAnalysis.scannedCandidateIdeaCount
            it[failureReason] = duplicateAnalysis.failureReason
            it[generatedAt] = duplicateAnalysis.generatedAt.toLocalDateTime()
        }
    }

    private fun replaceMatches(
        duplicateAnalysisId: Long,
        matches: List<DuplicateAnalysisMatch>,
    ) {
        deleteMatches(duplicateAnalysisId)
        matches.forEachIndexed { index, match ->
            val matchId = DuplicateAnalysisMatchTable.insert {
                it[analysisId] = duplicateAnalysisId
                it[matchOrder] = index
                it[sourceType] = match.sourceType
                it[sourceId] = match.sourceId
                it[sourceTeamId] = match.sourceTeamId?.value
                it[sourceTitle] = match.sourceTitle
                it[sourceDisclosure] = match.sourceDisclosure
                it[similarityLevel] = match.similarityLevel
                it[overlapSummary] = match.overlapSummary
            }[DuplicateAnalysisMatchTable.id]

            match.overlapDimensions.distinct().forEachIndexed { dimensionIndex, dimension ->
                DuplicateAnalysisMatchDimensionTable.insert {
                    it[DuplicateAnalysisMatchDimensionTable.matchId] = matchId
                    it[DuplicateAnalysisMatchDimensionTable.dimension] = dimension
                    it[sortOrder] = dimensionIndex
                }
            }
        }
    }

    private fun deleteMatches(duplicateAnalysisId: Long) {
        val matchIds = DuplicateAnalysisMatchTable
            .selectAll()
            .where { DuplicateAnalysisMatchTable.analysisId eq duplicateAnalysisId }
            .map { it[DuplicateAnalysisMatchTable.id] }
        if (matchIds.isNotEmpty()) {
            DuplicateAnalysisMatchDimensionTable.deleteWhere {
                DuplicateAnalysisMatchDimensionTable.matchId inList matchIds
            }
        }
        DuplicateAnalysisMatchTable.deleteWhere {
            DuplicateAnalysisMatchTable.analysisId eq duplicateAnalysisId
        }
    }

    private fun loadMatchEntities(duplicateAnalysisId: Long): List<DuplicateAnalysisMatchEntity> {
        val matchRows = DuplicateAnalysisMatchTable
            .selectAll()
            .where { DuplicateAnalysisMatchTable.analysisId eq duplicateAnalysisId }
            .orderBy(DuplicateAnalysisMatchTable.matchOrder to SortOrder.ASC)
            .toList()
        if (matchRows.isEmpty()) {
            return emptyList()
        }

        val dimensionsByMatchId = loadDimensions(matchRows.map { it[DuplicateAnalysisMatchTable.id] })
        return matchRows.map { row ->
            val matchId = row[DuplicateAnalysisMatchTable.id]
            row.toDuplicateAnalysisMatchEntity(
                dimensions = dimensionsByMatchId.getValue(matchId),
            )
        }
    }

    private fun loadDimensions(matchIds: Collection<Long>): Map<Long, List<OverlapDimension>> {
        if (matchIds.isEmpty()) {
            return emptyMap()
        }

        return DuplicateAnalysisMatchDimensionTable
            .selectAll()
            .where { DuplicateAnalysisMatchDimensionTable.matchId inList matchIds.distinct() }
            .orderBy(
                DuplicateAnalysisMatchDimensionTable.matchId to SortOrder.ASC,
                DuplicateAnalysisMatchDimensionTable.sortOrder to SortOrder.ASC,
            )
            .groupBy(
                keySelector = { it[DuplicateAnalysisMatchDimensionTable.matchId] },
                valueTransform = { it[DuplicateAnalysisMatchDimensionTable.dimension] },
            )
    }

    private fun duplicateAnalysisRowExists(duplicateAnalysisId: Long): Boolean {
        return DuplicateAnalysisTable
            .selectAll()
            .where { DuplicateAnalysisTable.id eq duplicateAnalysisId }
            .limit(1)
            .any()
    }
}
