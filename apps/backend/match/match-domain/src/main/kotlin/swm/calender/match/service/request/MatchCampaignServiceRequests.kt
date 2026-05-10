package swm.calender.match.service.request

import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.Platform
import swm.calender.match.domain.CampaignSearchSort
import java.time.OffsetDateTime

data class ServiceProfileCreateRequest(
    val actorUserId: UserId,
    val name: String,
    val summary: String,
    val description: String,
    val category: CampaignCategory,
    val platforms: List<Platform>,
    val screenshotUrls: List<String> = emptyList(),
    val demoUrl: String? = null,
    val isPublic: Boolean,
)

data class CampaignCreateRequest(
    val actorUserId: UserId,
    val title: String,
    val description: String,
    val targetTeamCount: Int,
    val deadline: OffsetDateTime,
    val reciprocalAvailable: Boolean,
    val requirements: String? = null,
)

data class CampaignStatusChangeRequest(
    val actorUserId: UserId,
    val campaignId: CampaignId,
    val status: CampaignStatus,
)

data class CampaignSearchRequest(
    val category: CampaignCategory? = null,
    val platform: Platform? = null,
    val reciprocalAvailable: Boolean? = null,
    val sort: CampaignSearchSort = CampaignSearchSort.LATEST,
)
