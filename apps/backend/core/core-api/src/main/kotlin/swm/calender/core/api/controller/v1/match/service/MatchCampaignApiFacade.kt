package swm.calender.core.api.controller.v1.match.service

import org.springframework.stereotype.Component
import swm.calender.core.api.controller.v1.match.request.CampaignCreateRequest
import swm.calender.core.api.controller.v1.match.request.CampaignStatusChangeRequest
import swm.calender.core.api.controller.v1.match.request.ServiceProfileCreateRequest
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.CampaignId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.Platform
import swm.calender.match.domain.CampaignSearchSort
import swm.calender.match.service.MatchCampaignService
import swm.calender.match.service.request.CampaignSearchRequest
import java.time.OffsetDateTime
import swm.calender.match.service.response.CampaignResponse as CampaignServiceResponse
import swm.calender.match.service.response.CampaignSearchItemResponse as CampaignSearchItemServiceResponse
import swm.calender.match.service.response.ServiceProfileResponse as ServiceProfileServiceResponse

@Component
class MatchCampaignApiFacade(
    private val matchCampaignService: MatchCampaignService,
) {
    fun createServiceProfile(
        user: AuthenticatedUser,
        request: ServiceProfileCreateRequest,
    ): ServiceProfileSnapshot {
        return ServiceProfileSnapshot.from(
            matchCampaignService.createServiceProfile(
                swm.calender.match.service.request.ServiceProfileCreateRequest(
                    actorUserId = user.userId,
                    name = requireNotNull(request.name),
                    summary = requireNotNull(request.summary),
                    description = requireNotNull(request.description),
                    category = requireNotNull(request.category),
                    platforms = requireNotNull(request.platforms),
                    screenshotUrls = request.screenshotUrls.orEmpty(),
                    demoUrl = request.demoUrl,
                    isPublic = request.isPublic ?: true,
                ),
            ),
        )
    }

    fun createCampaign(
        user: AuthenticatedUser,
        request: CampaignCreateRequest,
    ): CampaignSnapshot {
        return CampaignSnapshot.from(
            matchCampaignService.createCampaign(
                swm.calender.match.service.request.CampaignCreateRequest(
                    actorUserId = user.userId,
                    title = requireNotNull(request.title),
                    description = requireNotNull(request.description),
                    targetTeamCount = requireNotNull(request.targetTeamCount),
                    deadline = requireNotNull(request.deadline),
                    reciprocalAvailable = requireNotNull(request.reciprocalAvailable),
                    requirements = request.requirements,
                ),
            ),
        )
    }

    fun changeCampaignStatus(
        user: AuthenticatedUser,
        campaignId: Long,
        request: CampaignStatusChangeRequest,
    ): CampaignSnapshot {
        return CampaignSnapshot.from(
            matchCampaignService.changeCampaignStatus(
                swm.calender.match.service.request.CampaignStatusChangeRequest(
                    actorUserId = user.userId,
                    campaignId = CampaignId(campaignId),
                    status = requireNotNull(request.status),
                ),
            ),
        )
    }

    fun searchCampaigns(
        category: CampaignCategory?,
        platform: Platform?,
        reciprocalAvailable: Boolean?,
        sort: CampaignSearchSort,
    ): List<CampaignSearchItemSnapshot> {
        return matchCampaignService.searchCampaigns(
            CampaignSearchRequest(
                category = category,
                platform = platform,
                reciprocalAvailable = reciprocalAvailable,
                sort = sort,
            ),
        ).map(CampaignSearchItemSnapshot::from)
    }
}

data class ServiceProfileSnapshot(
    val serviceProfileId: Long,
    val teamId: Long,
    val active: Boolean,
    val isPublic: Boolean,
    val name: String,
    val summary: String,
    val category: CampaignCategory,
    val platforms: List<Platform>,
) {
    companion object {
        fun from(response: ServiceProfileServiceResponse): ServiceProfileSnapshot {
            return ServiceProfileSnapshot(
                serviceProfileId = response.serviceProfileId,
                teamId = response.teamId.value,
                active = response.active,
                isPublic = response.isPublic,
                name = response.name,
                summary = response.summary,
                category = response.category,
                platforms = response.platforms,
            )
        }
    }
}

data class CampaignSnapshot(
    val campaignId: Long,
    val serviceProfileId: Long,
    val title: String,
    val targetTeamCount: Int,
    val deadline: OffsetDateTime,
    val reciprocalAvailable: Boolean,
    val status: CampaignStatus,
) {
    companion object {
        fun from(response: CampaignServiceResponse): CampaignSnapshot {
            return CampaignSnapshot(
                campaignId = response.campaignId,
                serviceProfileId = response.serviceProfileId,
                title = response.title,
                targetTeamCount = response.targetTeamCount,
                deadline = response.deadline,
                reciprocalAvailable = response.reciprocalAvailable,
                status = response.status,
            )
        }
    }
}

data class CampaignSearchItemSnapshot(
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
        fun from(response: CampaignSearchItemServiceResponse): CampaignSearchItemSnapshot {
            return CampaignSearchItemSnapshot(
                campaignId = response.campaignId,
                teamId = response.teamId.value,
                teamName = response.teamName,
                serviceName = response.serviceName,
                serviceSummary = response.serviceSummary,
                category = response.category,
                platforms = response.platforms,
                reciprocalAvailable = response.reciprocalAvailable,
                deadline = response.deadline,
                status = response.status,
            )
        }
    }
}
