package swm.calender.storage.db.core.match

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.Platform
import swm.calender.storage.db.core.team.TeamTable

object ServiceProfileTable : Table("service_profile") {
    val id = long("id").autoIncrement()
    val teamId = long("team_id").references(TeamTable.id)
    val version = integer("profile_version")
    val isPublic = bool("is_public")
    val serviceName = varchar("service_name", 80)
    val summary = varchar("summary", 120)
    val description = text("description")
    val category = enumerationByName<CampaignCategory>("category", 30)
    val demoUrl = varchar("demo_url", 2048).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        uniqueIndex("ux_service_profile_team_id_profile_version", teamId, version)
        index("ix_service_profile_public_category", false, isPublic, category)
    }

    override val primaryKey = PrimaryKey(id)
}

object ServiceProfilePlatformTable : Table("service_profile_platform") {
    val serviceProfileId = long("service_profile_id").references(ServiceProfileTable.id)
    val platform = enumerationByName<Platform>("platform", 30)
    val sortOrder = integer("sort_order")

    init {
        index("ix_service_profile_platform_platform", false, platform)
    }

    override val primaryKey = PrimaryKey(serviceProfileId, platform)
}

object ServiceProfileScreenshotTable : Table("service_profile_screenshot") {
    val id = long("id").autoIncrement()
    val serviceProfileId = long("service_profile_id").references(ServiceProfileTable.id)
    val screenshotUrl = varchar("screenshot_url", 2048)
    val sortOrder = integer("sort_order")

    override val primaryKey = PrimaryKey(id)
}

object ActiveServiceProfileTable : Table("active_service_profile") {
    val teamId = long("team_id").references(TeamTable.id).uniqueIndex()
    val serviceProfileId = long("service_profile_id").references(ServiceProfileTable.id).uniqueIndex()

    override val primaryKey = PrimaryKey(teamId)
}

object BetaCampaignTable : Table("beta_campaign") {
    val id = long("id").autoIncrement()
    val teamId = long("team_id").references(TeamTable.id)
    val serviceProfileId = long("service_profile_id").references(ServiceProfileTable.id)
    val title = varchar("campaign_title", 100)
    val description = text("description")
    val targetTeamCount = integer("target_team_count")
    val deadline = datetime("deadline")
    val reciprocalAvailable = bool("reciprocal_available")
    val requirements = text("requirements").nullable()
    val status = enumerationByName<CampaignStatus>("campaign_status", 20)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        index("ix_beta_campaign_status_created_at", false, status, createdAt)
        index("ix_beta_campaign_status_deadline", false, status, deadline)
        index("ix_beta_campaign_reciprocal_status", false, reciprocalAvailable, status)
    }

    override val primaryKey = PrimaryKey(id)
}

internal fun ResultRow.toServiceProfileEntity(
    platforms: List<Platform>,
    screenshotUrls: List<String>,
    active: Boolean,
): ServiceProfileEntity = ServiceProfileEntity(
    id = this[ServiceProfileTable.id],
    teamId = this[ServiceProfileTable.teamId],
    version = this[ServiceProfileTable.version],
    active = active,
    isPublic = this[ServiceProfileTable.isPublic],
    name = this[ServiceProfileTable.serviceName],
    summary = this[ServiceProfileTable.summary],
    description = this[ServiceProfileTable.description],
    category = this[ServiceProfileTable.category],
    platforms = platforms,
    screenshotUrls = screenshotUrls,
    demoUrl = this[ServiceProfileTable.demoUrl],
    createdAt = this[ServiceProfileTable.createdAt],
    updatedAt = this[ServiceProfileTable.updatedAt],
)

internal fun ResultRow.toBetaCampaignEntity(): BetaCampaignEntity = BetaCampaignEntity(
    id = this[BetaCampaignTable.id],
    teamId = this[BetaCampaignTable.teamId],
    serviceProfileId = this[BetaCampaignTable.serviceProfileId],
    title = this[BetaCampaignTable.title],
    description = this[BetaCampaignTable.description],
    targetTeamCount = this[BetaCampaignTable.targetTeamCount],
    deadline = this[BetaCampaignTable.deadline],
    reciprocalAvailable = this[BetaCampaignTable.reciprocalAvailable],
    requirements = this[BetaCampaignTable.requirements],
    status = this[BetaCampaignTable.status],
    createdAt = this[BetaCampaignTable.createdAt],
    updatedAt = this[BetaCampaignTable.updatedAt],
)
