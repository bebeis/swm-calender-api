package swm.calender.storage.db.core.match

import swm.calender.core.common.id.AssignmentId
import swm.calender.core.common.id.CampaignId
import swm.calender.core.common.id.CandidateIdeaId
import swm.calender.core.common.id.DuplicateAnalysisId
import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.CandidateIdeaVisibility
import swm.calender.core.enums.DuplicateAnalysisSourceType
import swm.calender.core.enums.DuplicateAnalysisStatus
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType
import swm.calender.core.enums.NotificationType
import swm.calender.core.enums.OverlapDimension
import swm.calender.core.enums.Platform
import swm.calender.core.enums.SimilarityLevel
import swm.calender.core.enums.SourceDisclosure
import swm.calender.match.domain.model.Assignment
import swm.calender.match.domain.model.BetaCampaign
import swm.calender.match.domain.model.CandidateIdea
import swm.calender.match.domain.model.DuplicateAnalysis
import swm.calender.match.domain.model.DuplicateAnalysisMatch
import swm.calender.match.domain.model.MatchRequest
import swm.calender.match.domain.model.MatchRequestStatusHistory
import swm.calender.match.domain.model.Notification
import swm.calender.match.domain.model.NotificationReferenceType
import swm.calender.match.domain.model.ServiceProfile
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal data class ServiceProfileEntity(
    val id: Long,
    val teamId: Long,
    val version: Int,
    val active: Boolean,
    val isPublic: Boolean,
    val name: String,
    val summary: String,
    val description: String,
    val category: CampaignCategory,
    val platforms: List<Platform>,
    val screenshotUrls: List<String>,
    val demoUrl: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun toDomain(): ServiceProfile {
        return ServiceProfile(
            id = id,
            teamId = TeamId(teamId),
            version = version,
            active = active,
            isPublic = isPublic,
            name = name,
            summary = summary,
            description = description,
            category = category,
            platforms = platforms,
            screenshotUrls = screenshotUrls,
            demoUrl = demoUrl,
            createdAt = createdAt.toInstant(),
            updatedAt = updatedAt.toInstant(),
        )
    }
}

internal data class BetaCampaignEntity(
    val id: Long,
    val teamId: Long,
    val serviceProfileId: Long,
    val title: String,
    val description: String,
    val targetTeamCount: Int,
    val deadline: LocalDateTime,
    val reciprocalAvailable: Boolean,
    val requirements: String?,
    val status: CampaignStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun toDomain(): BetaCampaign {
        return BetaCampaign(
            id = CampaignId(id),
            teamId = TeamId(teamId),
            serviceProfileId = serviceProfileId,
            title = title,
            description = description,
            targetTeamCount = targetTeamCount,
            deadline = deadline.toOffsetDateTime(),
            reciprocalAvailable = reciprocalAvailable,
            requirements = requirements,
            status = status,
            createdAt = createdAt.toInstant(),
            updatedAt = updatedAt.toInstant(),
        )
    }
}

internal data class CandidateIdeaEntity(
    val id: Long,
    val teamId: Long,
    val title: String,
    val summary: String,
    val problem: String,
    val targetUsers: String,
    val solution: String,
    val category: CampaignCategory,
    val platforms: List<Platform>,
    val visibility: CandidateIdeaVisibility,
    val createdByUserId: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun toDomain(): CandidateIdea {
        return CandidateIdea(
            id = CandidateIdeaId(id),
            teamId = TeamId(teamId),
            title = title,
            summary = summary,
            problem = problem,
            targetUsers = targetUsers,
            solution = solution,
            category = category,
            platforms = platforms,
            visibility = visibility,
            createdByUserId = UserId(createdByUserId),
            createdAt = createdAt.toInstant(),
            updatedAt = updatedAt.toInstant(),
        )
    }
}

internal data class DuplicateAnalysisEntity(
    val id: Long,
    val candidateIdeaId: Long,
    val requestedByTeamId: Long,
    val requestedByUserId: Long,
    val status: DuplicateAnalysisStatus,
    val scannedReleasedServiceCount: Int,
    val scannedCandidateIdeaCount: Int,
    val matches: List<DuplicateAnalysisMatchEntity>,
    val failureReason: String?,
    val generatedAt: LocalDateTime,
) {
    fun toDomain(): DuplicateAnalysis {
        return DuplicateAnalysis(
            id = DuplicateAnalysisId(id),
            candidateIdeaId = CandidateIdeaId(candidateIdeaId),
            requestedByTeamId = TeamId(requestedByTeamId),
            requestedByUserId = UserId(requestedByUserId),
            status = status,
            scannedReleasedServiceCount = scannedReleasedServiceCount,
            scannedCandidateIdeaCount = scannedCandidateIdeaCount,
            matches = matches.map(DuplicateAnalysisMatchEntity::toDomain),
            failureReason = failureReason,
            generatedAt = generatedAt.toInstant(),
        )
    }
}

internal data class DuplicateAnalysisMatchEntity(
    val id: Long,
    val sourceType: DuplicateAnalysisSourceType,
    val sourceId: Long?,
    val sourceTeamId: Long?,
    val sourceTitle: String?,
    val sourceDisclosure: SourceDisclosure,
    val similarityLevel: SimilarityLevel,
    val overlapDimensions: List<OverlapDimension>,
    val overlapSummary: String,
) {
    fun toDomain(): DuplicateAnalysisMatch {
        return DuplicateAnalysisMatch(
            sourceType = sourceType,
            sourceId = sourceId,
            sourceTeamId = sourceTeamId?.let(::TeamId),
            sourceTitle = sourceTitle,
            sourceDisclosure = sourceDisclosure,
            similarityLevel = similarityLevel,
            overlapDimensions = overlapDimensions,
            overlapSummary = overlapSummary,
        )
    }
}

internal data class MatchRequestEntity(
    val id: Long,
    val campaignId: Long,
    val requestingTeamId: Long,
    val targetTeamId: Long,
    val type: MatchRequestType,
    val status: MatchRequestStatus,
    val message: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun toDomain(): MatchRequest {
        return MatchRequest(
            id = RequestId(id),
            campaignId = CampaignId(campaignId),
            requestingTeamId = TeamId(requestingTeamId),
            targetTeamId = TeamId(targetTeamId),
            type = type,
            status = status,
            message = message,
            createdAt = createdAt.toInstant(),
            updatedAt = updatedAt.toInstant(),
        )
    }
}

internal data class MatchRequestStatusHistoryEntity(
    val id: Long,
    val requestId: Long,
    val fromStatus: MatchRequestStatus?,
    val toStatus: MatchRequestStatus,
    val changedByUserId: Long,
    val createdAt: LocalDateTime,
) {
    fun toDomain(): MatchRequestStatusHistory {
        return MatchRequestStatusHistory(
            id = id,
            requestId = RequestId(requestId),
            fromStatus = fromStatus,
            toStatus = toStatus,
            changedByUserId = UserId(changedByUserId),
            createdAt = createdAt.toInstant(),
        )
    }
}

internal data class AssignmentEntity(
    val id: Long,
    val requestId: Long,
    val testerTeamId: Long,
    val targetTeamId: Long,
    val status: AssignmentStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun toDomain(): Assignment {
        return Assignment(
            id = AssignmentId(id),
            requestId = RequestId(requestId),
            testerTeamId = TeamId(testerTeamId),
            targetTeamId = TeamId(targetTeamId),
            status = status,
            createdAt = createdAt.toInstant(),
            updatedAt = updatedAt.toInstant(),
        )
    }
}

internal data class NotificationEntity(
    val id: Long,
    val teamId: Long,
    val type: NotificationType,
    val referenceType: NotificationReferenceType,
    val referenceId: Long,
    val message: String,
    val readAt: LocalDateTime?,
    val createdAt: LocalDateTime,
) {
    fun toDomain(): Notification {
        return Notification(
            id = id,
            teamId = TeamId(teamId),
            type = type,
            referenceType = referenceType,
            referenceId = referenceId,
            message = message,
            readAt = readAt?.toInstant(),
            createdAt = createdAt.toInstant(),
        )
    }
}

internal fun Instant.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneOffset.UTC)

internal fun OffsetDateTime.toUtcLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(toInstant(), ZoneOffset.UTC)

private fun LocalDateTime.toInstant(): Instant = toInstant(ZoneOffset.UTC)

private fun LocalDateTime.toOffsetDateTime(): OffsetDateTime = atOffset(ZoneOffset.UTC)
