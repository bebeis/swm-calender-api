package swm.calender.match.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CandidateIdeaVisibility
import swm.calender.core.enums.Platform
import swm.calender.match.domain.model.CandidateIdea
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import java.time.Instant

class CandidateIdeaTest :
    FunSpec({
        test("createPrivate creates a team-scoped private candidate idea") {
            // given
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")

            // when
            val candidateIdea = CandidateIdea.createPrivate(
                teamId = TeamId(1L),
                title = " Idea ",
                summary = " Summary ",
                problem = " Problem ",
                targetUsers = " Students ",
                solution = " Solution ",
                category = CampaignCategory.EDUCATION,
                platforms = listOf(Platform.WEB, Platform.API, Platform.WEB),
                createdByUserId = UserId(10L),
                createdAt = createdAt,
            )

            // then
            candidateIdea.visibility shouldBe CandidateIdeaVisibility.PRIVATE
            candidateIdea.title shouldBe "Idea"
            candidateIdea.platforms shouldBe listOf(Platform.WEB, Platform.API)
        }

        test("candidate idea rejects a blank solution") {
            // when
            val exception = shouldThrow<MatchDomainException> {
                CandidateIdea.createPrivate(
                    teamId = TeamId(1L),
                    title = "Idea",
                    summary = "Summary",
                    problem = "Problem",
                    targetUsers = "Students",
                    solution = " ",
                    category = CampaignCategory.EDUCATION,
                    platforms = listOf(Platform.WEB),
                    createdByUserId = UserId(10L),
                    createdAt = Instant.parse("2026-05-10T00:00:00Z"),
                )
            }

            // then
            exception.errorMessage shouldBe MatchErrorMessage.CANDIDATE_IDEA_SOLUTION_REQUIRED
        }
    })
