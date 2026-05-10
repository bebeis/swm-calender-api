package swm.calender.storage.db.core.match

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.Platform
import swm.calender.match.domain.CampaignSearchFilter
import swm.calender.match.domain.CampaignSearchResult
import swm.calender.match.domain.CampaignSearchSort
import swm.calender.match.domain.MatchCampaignRepository
import swm.calender.match.domain.ReleasedServiceProfile
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.domain.model.ServiceProfile
import swm.calender.storage.db.core.team.TeamTable

@Repository
class MatchCampaignExposedRepository : MatchCampaignRepository {
    override fun saveServiceProfile(serviceProfile: ServiceProfile): ServiceProfile {
        val savedProfileId = serviceProfile.id?.takeIf { serviceProfileRowExists(it) }
            ?.also { updateServiceProfile(it, serviceProfile) }
            ?: insertServiceProfile(serviceProfile)

        replacePlatforms(savedProfileId, serviceProfile.platforms)
        replaceScreenshots(savedProfileId, serviceProfile.screenshotUrls)
        syncActiveServiceProfile(savedProfileId, serviceProfile)

        return requireNotNull(findServiceProfileById(savedProfileId))
    }

    override fun findActiveServiceProfileByTeamId(teamId: TeamId): ServiceProfile? {
        val serviceProfileId = ActiveServiceProfileTable
            .selectAll()
            .where { ActiveServiceProfileTable.teamId eq teamId.value }
            .singleOrNull()
            ?.get(ActiveServiceProfileTable.serviceProfileId)

        return serviceProfileId?.let(::findServiceProfileById)
    }

    override fun countServiceProfilesByTeamId(teamId: TeamId): Int {
        return ServiceProfileTable
            .selectAll()
            .where { ServiceProfileTable.teamId eq teamId.value }
            .count()
            .toInt()
    }

    override fun saveCampaign(campaign: BetaCampaign): BetaCampaign {
        val savedCampaignId = campaign.id?.value?.takeIf { campaignRowExists(it) }
            ?.also { updateCampaign(it, campaign) }
            ?: insertCampaign(campaign)

        return requireNotNull(findCampaignById(CampaignId(savedCampaignId)))
    }

    override fun findCampaignById(campaignId: CampaignId): BetaCampaign? {
        return BetaCampaignTable
            .selectAll()
            .where { BetaCampaignTable.id eq campaignId.value }
            .singleOrNull()
            ?.toBetaCampaignEntity()
            ?.toDomain()
    }

    override fun searchOpenCampaigns(filter: CampaignSearchFilter): List<CampaignSearchResult> {
        val serviceProfileIdsByPlatform = filter.platform?.let(::findServiceProfileIdsByPlatform)
        if (serviceProfileIdsByPlatform != null && serviceProfileIdsByPlatform.isEmpty()) {
            return emptyList()
        }

        var condition: Op<Boolean> =
            (BetaCampaignTable.status eq CampaignStatus.OPEN) and (ServiceProfileTable.isPublic eq true)
        filter.category?.let {
            condition = condition and (ServiceProfileTable.category eq it)
        }
        filter.reciprocalAvailable?.let {
            condition = condition and (BetaCampaignTable.reciprocalAvailable eq it)
        }
        serviceProfileIdsByPlatform?.let {
            condition = condition and (ServiceProfileTable.id inList it)
        }

        val rows = campaignSearchJoin()
            .selectAll()
            .where { condition }
            .orderBy(*filter.sort.toOrderBy())
            .toList()
        if (rows.isEmpty()) {
            return emptyList()
        }

        val profileIds = rows.map { it[ServiceProfileTable.id] }
        val platformsByProfileId = loadServiceProfilePlatforms(profileIds)
        val screenshotsByProfileId = loadServiceProfileScreenshots(profileIds)
        val activeProfileIds = loadActiveProfileIds(profileIds)

        return rows.map { row ->
            val serviceProfileId = row[ServiceProfileTable.id]
            CampaignSearchResult(
                campaign = row.toBetaCampaignEntity().toDomain(),
                teamName = row[TeamTable.name],
                serviceProfile = row.toServiceProfileEntity(
                    platforms = platformsByProfileId.getValue(serviceProfileId),
                    screenshotUrls = screenshotsByProfileId[serviceProfileId].orEmpty(),
                    active = serviceProfileId in activeProfileIds,
                ).toDomain(),
            )
        }
    }

    override fun findReleasedServiceProfiles(): List<ReleasedServiceProfile> {
        val rows = ServiceProfileTable
            .join(
                otherTable = ActiveServiceProfileTable,
                joinType = JoinType.INNER,
                onColumn = ServiceProfileTable.id,
                otherColumn = ActiveServiceProfileTable.serviceProfileId,
            )
            .selectAll()
            .where { ServiceProfileTable.isPublic eq true }
            .orderBy(ServiceProfileTable.id to SortOrder.ASC)
            .toList()
        if (rows.isEmpty()) {
            return emptyList()
        }

        val profileIds = rows.map { it[ServiceProfileTable.id] }
        val platformsByProfileId = loadServiceProfilePlatforms(profileIds)
        val screenshotsByProfileId = loadServiceProfileScreenshots(profileIds)
        val descriptionsByProfileId = loadOpenCampaignDescriptions(profileIds)

        return rows.map { row ->
            val serviceProfileId = row[ServiceProfileTable.id]
            ReleasedServiceProfile(
                serviceProfile = row.toServiceProfileEntity(
                    platforms = platformsByProfileId.getValue(serviceProfileId),
                    screenshotUrls = screenshotsByProfileId[serviceProfileId].orEmpty(),
                    active = true,
                ).toDomain(),
                openCampaignDescriptions = descriptionsByProfileId[serviceProfileId].orEmpty(),
            )
        }
    }

    private fun insertServiceProfile(serviceProfile: ServiceProfile): Long {
        return ServiceProfileTable.insert {
            it[teamId] = serviceProfile.teamId.value
            it[version] = serviceProfile.version
            it[isPublic] = serviceProfile.isPublic
            it[serviceName] = serviceProfile.name
            it[summary] = serviceProfile.summary
            it[description] = serviceProfile.description
            it[category] = serviceProfile.category
            it[demoUrl] = serviceProfile.demoUrl
            it[createdAt] = serviceProfile.createdAt.toLocalDateTime()
            it[updatedAt] = serviceProfile.updatedAt.toLocalDateTime()
        }[ServiceProfileTable.id]
    }

    private fun updateServiceProfile(
        serviceProfileId: Long,
        serviceProfile: ServiceProfile,
    ) {
        ServiceProfileTable.update(
            where = { ServiceProfileTable.id eq serviceProfileId },
        ) {
            it[teamId] = serviceProfile.teamId.value
            it[version] = serviceProfile.version
            it[isPublic] = serviceProfile.isPublic
            it[serviceName] = serviceProfile.name
            it[summary] = serviceProfile.summary
            it[description] = serviceProfile.description
            it[category] = serviceProfile.category
            it[demoUrl] = serviceProfile.demoUrl
            it[createdAt] = serviceProfile.createdAt.toLocalDateTime()
            it[updatedAt] = serviceProfile.updatedAt.toLocalDateTime()
        }
    }

    private fun replacePlatforms(
        serviceProfileId: Long,
        platforms: List<Platform>,
    ) {
        ServiceProfilePlatformTable.deleteWhere {
            ServiceProfilePlatformTable.serviceProfileId eq serviceProfileId
        }
        platforms.distinct().forEachIndexed { index, platform ->
            ServiceProfilePlatformTable.insert {
                it[ServiceProfilePlatformTable.serviceProfileId] = serviceProfileId
                it[ServiceProfilePlatformTable.platform] = platform
                it[sortOrder] = index
            }
        }
    }

    private fun replaceScreenshots(
        serviceProfileId: Long,
        screenshotUrls: List<String>,
    ) {
        ServiceProfileScreenshotTable.deleteWhere {
            ServiceProfileScreenshotTable.serviceProfileId eq serviceProfileId
        }
        screenshotUrls.forEachIndexed { index, screenshotUrl ->
            ServiceProfileScreenshotTable.insert {
                it[ServiceProfileScreenshotTable.serviceProfileId] = serviceProfileId
                it[ServiceProfileScreenshotTable.screenshotUrl] = screenshotUrl
                it[sortOrder] = index
            }
        }
    }

    private fun syncActiveServiceProfile(
        serviceProfileId: Long,
        serviceProfile: ServiceProfile,
    ) {
        if (!serviceProfile.active) {
            ActiveServiceProfileTable.deleteWhere {
                ActiveServiceProfileTable.serviceProfileId eq serviceProfileId
            }
            return
        }

        val activeRowExists = ActiveServiceProfileTable
            .selectAll()
            .where { ActiveServiceProfileTable.teamId eq serviceProfile.teamId.value }
            .limit(1)
            .any()
        if (activeRowExists) {
            ActiveServiceProfileTable.update(
                where = { ActiveServiceProfileTable.teamId eq serviceProfile.teamId.value },
            ) {
                it[ActiveServiceProfileTable.serviceProfileId] = serviceProfileId
            }
        } else {
            ActiveServiceProfileTable.insert {
                it[teamId] = serviceProfile.teamId.value
                it[ActiveServiceProfileTable.serviceProfileId] = serviceProfileId
            }
        }
    }

    private fun findServiceProfileById(serviceProfileId: Long): ServiceProfile? {
        val row = ServiceProfileTable
            .selectAll()
            .where { ServiceProfileTable.id eq serviceProfileId }
            .singleOrNull()
            ?: return null

        return row.toServiceProfileEntity(
            platforms = loadServiceProfilePlatforms(listOf(serviceProfileId)).getValue(serviceProfileId),
            screenshotUrls = loadServiceProfileScreenshots(listOf(serviceProfileId))[serviceProfileId].orEmpty(),
            active = serviceProfileId in loadActiveProfileIds(listOf(serviceProfileId)),
        ).toDomain()
    }

    private fun insertCampaign(campaign: BetaCampaign): Long {
        return BetaCampaignTable.insert {
            it[teamId] = campaign.teamId.value
            it[serviceProfileId] = campaign.serviceProfileId
            it[title] = campaign.title
            it[description] = campaign.description
            it[targetTeamCount] = campaign.targetTeamCount
            it[deadline] = campaign.deadline.toUtcLocalDateTime()
            it[reciprocalAvailable] = campaign.reciprocalAvailable
            it[requirements] = campaign.requirements
            it[status] = campaign.status
            it[createdAt] = campaign.createdAt.toLocalDateTime()
            it[updatedAt] = campaign.updatedAt.toLocalDateTime()
        }[BetaCampaignTable.id]
    }

    private fun updateCampaign(
        campaignId: Long,
        campaign: BetaCampaign,
    ) {
        BetaCampaignTable.update(
            where = { BetaCampaignTable.id eq campaignId },
        ) {
            it[teamId] = campaign.teamId.value
            it[serviceProfileId] = campaign.serviceProfileId
            it[title] = campaign.title
            it[description] = campaign.description
            it[targetTeamCount] = campaign.targetTeamCount
            it[deadline] = campaign.deadline.toUtcLocalDateTime()
            it[reciprocalAvailable] = campaign.reciprocalAvailable
            it[requirements] = campaign.requirements
            it[status] = campaign.status
            it[createdAt] = campaign.createdAt.toLocalDateTime()
            it[updatedAt] = campaign.updatedAt.toLocalDateTime()
        }
    }

    private fun serviceProfileRowExists(serviceProfileId: Long): Boolean {
        return ServiceProfileTable
            .selectAll()
            .where { ServiceProfileTable.id eq serviceProfileId }
            .limit(1)
            .any()
    }

    private fun campaignRowExists(campaignId: Long): Boolean {
        return BetaCampaignTable
            .selectAll()
            .where { BetaCampaignTable.id eq campaignId }
            .limit(1)
            .any()
    }

    private fun loadServiceProfilePlatforms(profileIds: Collection<Long>): Map<Long, List<Platform>> {
        if (profileIds.isEmpty()) {
            return emptyMap()
        }

        return ServiceProfilePlatformTable
            .selectAll()
            .where { ServiceProfilePlatformTable.serviceProfileId inList profileIds.distinct() }
            .orderBy(
                ServiceProfilePlatformTable.serviceProfileId to SortOrder.ASC,
                ServiceProfilePlatformTable.sortOrder to SortOrder.ASC,
            )
            .groupBy(
                keySelector = { it[ServiceProfilePlatformTable.serviceProfileId] },
                valueTransform = { it[ServiceProfilePlatformTable.platform] },
            )
    }

    private fun loadServiceProfileScreenshots(profileIds: Collection<Long>): Map<Long, List<String>> {
        if (profileIds.isEmpty()) {
            return emptyMap()
        }

        return ServiceProfileScreenshotTable
            .selectAll()
            .where { ServiceProfileScreenshotTable.serviceProfileId inList profileIds.distinct() }
            .orderBy(
                ServiceProfileScreenshotTable.serviceProfileId to SortOrder.ASC,
                ServiceProfileScreenshotTable.sortOrder to SortOrder.ASC,
            )
            .groupBy(
                keySelector = { it[ServiceProfileScreenshotTable.serviceProfileId] },
                valueTransform = { it[ServiceProfileScreenshotTable.screenshotUrl] },
            )
    }

    private fun loadActiveProfileIds(profileIds: Collection<Long>): Set<Long> {
        if (profileIds.isEmpty()) {
            return emptySet()
        }

        return ActiveServiceProfileTable
            .selectAll()
            .where { ActiveServiceProfileTable.serviceProfileId inList profileIds.distinct() }
            .map { it[ActiveServiceProfileTable.serviceProfileId] }
            .toSet()
    }

    private fun loadOpenCampaignDescriptions(profileIds: Collection<Long>): Map<Long, List<String>> {
        if (profileIds.isEmpty()) {
            return emptyMap()
        }

        return BetaCampaignTable
            .selectAll()
            .where {
                (BetaCampaignTable.serviceProfileId inList profileIds.distinct()) and
                    (BetaCampaignTable.status eq CampaignStatus.OPEN)
            }
            .orderBy(BetaCampaignTable.id to SortOrder.ASC)
            .groupBy(
                keySelector = { it[BetaCampaignTable.serviceProfileId] },
                valueTransform = { it[BetaCampaignTable.description] },
            )
    }

    private fun findServiceProfileIdsByPlatform(platform: Platform): List<Long> {
        return ServiceProfilePlatformTable
            .selectAll()
            .where { ServiceProfilePlatformTable.platform eq platform }
            .map { it[ServiceProfilePlatformTable.serviceProfileId] }
            .distinct()
    }

    private fun CampaignSearchSort.toOrderBy(): Array<Pair<org.jetbrains.exposed.v1.core.Expression<*>, SortOrder>> {
        return when (this) {
            CampaignSearchSort.LATEST -> arrayOf(
                BetaCampaignTable.createdAt to SortOrder.DESC,
                BetaCampaignTable.id to SortOrder.DESC,
            )

            CampaignSearchSort.DEADLINE -> arrayOf(
                BetaCampaignTable.deadline to SortOrder.ASC,
                BetaCampaignTable.id to SortOrder.ASC,
            )
        }
    }

    private fun campaignSearchJoin() = BetaCampaignTable
        .join(
            otherTable = ServiceProfileTable,
            joinType = JoinType.INNER,
            onColumn = BetaCampaignTable.serviceProfileId,
            otherColumn = ServiceProfileTable.id,
        )
        .join(
            otherTable = ActiveServiceProfileTable,
            joinType = JoinType.INNER,
            onColumn = ServiceProfileTable.id,
            otherColumn = ActiveServiceProfileTable.serviceProfileId,
        )
        .join(
            otherTable = TeamTable,
            joinType = JoinType.INNER,
            onColumn = BetaCampaignTable.teamId,
            otherColumn = TeamTable.id,
        )
}
