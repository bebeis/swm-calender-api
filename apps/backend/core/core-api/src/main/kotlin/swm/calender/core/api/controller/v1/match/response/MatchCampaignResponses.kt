package swm.calender.core.api.controller.v1.match.response

import com.fasterxml.jackson.annotation.JsonProperty
import swm.calender.core.api.controller.v1.match.service.CampaignSearchItemSnapshot
import swm.calender.core.api.controller.v1.match.service.CampaignSnapshot
import swm.calender.core.api.controller.v1.match.service.ServiceProfileSnapshot
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.Platform
import java.time.OffsetDateTime

data class ServiceProfileResponse(
    val serviceProfileId: Long,
    val teamId: Long,
    val active: Boolean,
    @get:JsonProperty("public")
    val isPublic: Boolean,
    val name: String,
    val summary: String,
    val category: CampaignCategory,
    val platforms: List<Platform>,
) {
    companion object {
        fun from(snapshot: ServiceProfileSnapshot): ServiceProfileResponse {
            return ServiceProfileResponse(
                serviceProfileId = snapshot.serviceProfileId,
                teamId = snapshot.teamId,
                active = snapshot.active,
                isPublic = snapshot.isPublic,
                name = snapshot.name,
                summary = snapshot.summary,
                category = snapshot.category,
                platforms = snapshot.platforms,
            )
        }
    }
}

data class CampaignResponse(
    val campaignId: Long,
    val serviceProfileId: Long,
    val title: String,
    val targetTeamCount: Int,
    val deadline: OffsetDateTime,
    val reciprocalAvailable: Boolean,
    val status: CampaignStatus,
) {
    companion object {
        fun from(snapshot: CampaignSnapshot): CampaignResponse {
            return CampaignResponse(
                campaignId = snapshot.campaignId,
                serviceProfileId = snapshot.serviceProfileId,
                title = snapshot.title,
                targetTeamCount = snapshot.targetTeamCount,
                deadline = snapshot.deadline,
                reciprocalAvailable = snapshot.reciprocalAvailable,
                status = snapshot.status,
            )
        }
    }
}

data class CampaignSearchResponse(
    val items: List<CampaignSearchItemResponse>,
) {
    companion object {
        fun from(snapshots: List<CampaignSearchItemSnapshot>): CampaignSearchResponse {
            return CampaignSearchResponse(
                items = snapshots.map(CampaignSearchItemResponse::from),
            )
        }
    }
}

data class CampaignSearchItemResponse(
    val campaignId: Long,
    val teamId: Long,
    val teamName: String,
    val serviceName: String,
    val serviceSummary: String,
    val category: CampaignCategory,
    val platforms: List<Platform>,
    val reciprocalAvailable: Boolean,
    val deadline: OffsetDateTime,
    val status: CampaignStatus,
) {
    companion object {
        fun from(snapshot: CampaignSearchItemSnapshot): CampaignSearchItemResponse {
            return CampaignSearchItemResponse(
                campaignId = snapshot.campaignId,
                teamId = snapshot.teamId,
                teamName = snapshot.teamName,
                serviceName = snapshot.serviceName,
                serviceSummary = snapshot.serviceSummary,
                category = snapshot.category,
                platforms = snapshot.platforms,
                reciprocalAvailable = snapshot.reciprocalAvailable,
                deadline = snapshot.deadline,
                status = snapshot.status,
            )
        }
    }
}
