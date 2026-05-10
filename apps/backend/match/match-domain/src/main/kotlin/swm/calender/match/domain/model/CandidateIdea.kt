package swm.calender.match.domain.model

import swm.calender.core.common.id.CandidateIdeaId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CandidateIdeaVisibility
import swm.calender.core.enums.Platform
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import java.time.Instant

data class CandidateIdea(
    val id: CandidateIdeaId? = null,
    val teamId: TeamId,
    val title: String,
    val summary: String,
    val problem: String,
    val targetUsers: String,
    val solution: String,
    val category: CampaignCategory,
    val platforms: List<Platform>,
    val visibility: CandidateIdeaVisibility,
    val createdByUserId: UserId,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        validateTitle(title)
        validateSummary(summary)
        validateProblem(problem)
        validateTargetUsers(targetUsers)
        validateSolution(solution)
        validatePlatforms(platforms)
    }

    fun requireOwnedBy(teamId: TeamId): CandidateIdea {
        if (this.teamId != teamId) {
            throw MatchDomainException(MatchErrorMessage.CANDIDATE_IDEA_NOT_FOUND)
        }
        return this
    }

    companion object {
        fun createPrivate(
            teamId: TeamId,
            title: String,
            summary: String,
            problem: String,
            targetUsers: String,
            solution: String,
            category: CampaignCategory,
            platforms: List<Platform>,
            createdByUserId: UserId,
            createdAt: Instant,
        ): CandidateIdea {
            return CandidateIdea(
                teamId = teamId,
                title = title.trim(),
                summary = summary.trim(),
                problem = problem.trim(),
                targetUsers = targetUsers.trim(),
                solution = solution.trim(),
                category = category,
                platforms = platforms.distinct(),
                visibility = CandidateIdeaVisibility.PRIVATE,
                createdByUserId = createdByUserId,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }

        private fun validateTitle(title: String) {
            if (title.isBlank()) {
                throw MatchDomainException(MatchErrorMessage.CANDIDATE_IDEA_TITLE_REQUIRED)
            }
        }

        private fun validateSummary(summary: String) {
            if (summary.isBlank()) {
                throw MatchDomainException(MatchErrorMessage.CANDIDATE_IDEA_SUMMARY_REQUIRED)
            }
        }

        private fun validateProblem(problem: String) {
            if (problem.isBlank()) {
                throw MatchDomainException(MatchErrorMessage.CANDIDATE_IDEA_PROBLEM_REQUIRED)
            }
        }

        private fun validateTargetUsers(targetUsers: String) {
            if (targetUsers.isBlank()) {
                throw MatchDomainException(MatchErrorMessage.CANDIDATE_IDEA_TARGET_USERS_REQUIRED)
            }
        }

        private fun validateSolution(solution: String) {
            if (solution.isBlank()) {
                throw MatchDomainException(MatchErrorMessage.CANDIDATE_IDEA_SOLUTION_REQUIRED)
            }
        }

        private fun validatePlatforms(platforms: List<Platform>) {
            if (platforms.isEmpty()) {
                throw MatchDomainException(MatchErrorMessage.CANDIDATE_IDEA_PLATFORMS_REQUIRED)
            }
        }
    }
}
