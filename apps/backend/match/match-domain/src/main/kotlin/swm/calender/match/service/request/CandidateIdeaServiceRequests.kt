package swm.calender.match.service.request

import swm.calender.core.common.id.CandidateIdeaId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.Platform

data class CandidateIdeaCreateRequest(
    val actorUserId: UserId,
    val title: String,
    val summary: String,
    val problem: String,
    val targetUsers: String,
    val solution: String,
    val category: CampaignCategory,
    val platforms: List<Platform>,
)

data class DuplicateAnalysisRunRequest(
    val actorUserId: UserId,
    val candidateIdeaId: CandidateIdeaId,
)
