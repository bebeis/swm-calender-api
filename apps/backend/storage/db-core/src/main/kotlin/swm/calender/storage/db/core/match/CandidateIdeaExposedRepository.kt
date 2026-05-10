package swm.calender.storage.db.core.match

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import swm.calender.core.common.id.CandidateIdeaId
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.Platform
import swm.calender.match.domain.CandidateIdeaRepository
import swm.calender.match.domain.model.CandidateIdea

@Repository
class CandidateIdeaExposedRepository : CandidateIdeaRepository {
    override fun save(candidateIdea: CandidateIdea): CandidateIdea {
        val savedCandidateIdeaId = candidateIdea.id?.value?.takeIf { candidateIdeaRowExists(it) }
            ?.also { updateCandidateIdea(it, candidateIdea) }
            ?: insertCandidateIdea(candidateIdea)

        replacePlatforms(savedCandidateIdeaId, candidateIdea.platforms)

        return requireNotNull(findById(CandidateIdeaId(savedCandidateIdeaId)))
    }

    override fun findById(candidateIdeaId: CandidateIdeaId): CandidateIdea? {
        val row = CandidateIdeaTable
            .selectAll()
            .where { CandidateIdeaTable.id eq candidateIdeaId.value }
            .singleOrNull()
            ?: return null

        return row.toCandidateIdeaEntity(
            platforms = loadPlatforms(listOf(candidateIdeaId.value)).getValue(candidateIdeaId.value),
        ).toDomain()
    }

    override fun findByTeamId(teamId: TeamId): List<CandidateIdea> {
        val rows = CandidateIdeaTable
            .selectAll()
            .where { CandidateIdeaTable.teamId eq teamId.value }
            .orderBy(
                CandidateIdeaTable.createdAt to SortOrder.DESC,
                CandidateIdeaTable.id to SortOrder.DESC,
            )
            .toList()
        if (rows.isEmpty()) {
            return emptyList()
        }

        val platformsByIdeaId = loadPlatforms(rows.map { it[CandidateIdeaTable.id] })
        return rows.map { row ->
            val candidateIdeaId = row[CandidateIdeaTable.id]
            row.toCandidateIdeaEntity(
                platforms = platformsByIdeaId.getValue(candidateIdeaId),
            ).toDomain()
        }
    }

    override fun findAll(): List<CandidateIdea> {
        val rows = CandidateIdeaTable
            .selectAll()
            .orderBy(CandidateIdeaTable.id to SortOrder.ASC)
            .toList()
        if (rows.isEmpty()) {
            return emptyList()
        }

        val platformsByIdeaId = loadPlatforms(rows.map { it[CandidateIdeaTable.id] })
        return rows.map { row ->
            val candidateIdeaId = row[CandidateIdeaTable.id]
            row.toCandidateIdeaEntity(
                platforms = platformsByIdeaId.getValue(candidateIdeaId),
            ).toDomain()
        }
    }

    private fun insertCandidateIdea(candidateIdea: CandidateIdea): Long {
        return CandidateIdeaTable.insert {
            it[teamId] = candidateIdea.teamId.value
            it[title] = candidateIdea.title
            it[summary] = candidateIdea.summary
            it[problem] = candidateIdea.problem
            it[targetUsers] = candidateIdea.targetUsers
            it[solution] = candidateIdea.solution
            it[category] = candidateIdea.category
            it[visibility] = candidateIdea.visibility
            it[createdByUserId] = candidateIdea.createdByUserId.value
            it[createdAt] = candidateIdea.createdAt.toLocalDateTime()
            it[updatedAt] = candidateIdea.updatedAt.toLocalDateTime()
        }[CandidateIdeaTable.id]
    }

    private fun updateCandidateIdea(
        candidateIdeaId: Long,
        candidateIdea: CandidateIdea,
    ) {
        CandidateIdeaTable.update(
            where = { CandidateIdeaTable.id eq candidateIdeaId },
        ) {
            it[teamId] = candidateIdea.teamId.value
            it[title] = candidateIdea.title
            it[summary] = candidateIdea.summary
            it[problem] = candidateIdea.problem
            it[targetUsers] = candidateIdea.targetUsers
            it[solution] = candidateIdea.solution
            it[category] = candidateIdea.category
            it[visibility] = candidateIdea.visibility
            it[createdByUserId] = candidateIdea.createdByUserId.value
            it[createdAt] = candidateIdea.createdAt.toLocalDateTime()
            it[updatedAt] = candidateIdea.updatedAt.toLocalDateTime()
        }
    }

    private fun replacePlatforms(
        candidateIdeaId: Long,
        platforms: List<Platform>,
    ) {
        CandidateIdeaPlatformTable.deleteWhere {
            CandidateIdeaPlatformTable.candidateIdeaId eq candidateIdeaId
        }
        platforms.distinct().forEachIndexed { index, platform ->
            CandidateIdeaPlatformTable.insert {
                it[CandidateIdeaPlatformTable.candidateIdeaId] = candidateIdeaId
                it[CandidateIdeaPlatformTable.platform] = platform
                it[sortOrder] = index
            }
        }
    }

    private fun candidateIdeaRowExists(candidateIdeaId: Long): Boolean {
        return CandidateIdeaTable
            .selectAll()
            .where { CandidateIdeaTable.id eq candidateIdeaId }
            .limit(1)
            .any()
    }

    private fun loadPlatforms(candidateIdeaIds: Collection<Long>): Map<Long, List<Platform>> {
        if (candidateIdeaIds.isEmpty()) {
            return emptyMap()
        }

        return CandidateIdeaPlatformTable
            .selectAll()
            .where { CandidateIdeaPlatformTable.candidateIdeaId inList candidateIdeaIds.distinct() }
            .orderBy(
                CandidateIdeaPlatformTable.candidateIdeaId to SortOrder.ASC,
                CandidateIdeaPlatformTable.sortOrder to SortOrder.ASC,
            )
            .groupBy(
                keySelector = { it[CandidateIdeaPlatformTable.candidateIdeaId] },
                valueTransform = { it[CandidateIdeaPlatformTable.platform] },
            )
    }
}
