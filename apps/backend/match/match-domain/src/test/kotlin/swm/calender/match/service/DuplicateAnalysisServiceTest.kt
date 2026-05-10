package swm.calender.match.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import swm.calender.core.common.id.CandidateIdeaId
import swm.calender.core.common.id.DuplicateAnalysisId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.Platform
import swm.calender.core.enums.SourceDisclosure
import swm.calender.core.enums.SubService
import swm.calender.core.team.domain.model.SubServiceActivation
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.domain.model.CandidateIdea
import swm.calender.match.domain.model.DuplicateAnalysis
import swm.calender.match.implement.CandidateIdeaReader
import swm.calender.match.implement.DuplicateAnalysisWriter
import swm.calender.match.implement.KeywordDuplicateIdeaAnalyzer
import swm.calender.match.implement.MatchCampaignReader
import swm.calender.match.service.request.DuplicateAnalysisRunRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DuplicateAnalysisServiceTest :
    FunSpec({
        val fixedInstant = Instant.parse("2026-05-10T00:00:00Z")
        val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

        lateinit var teamReader: TeamReader
        lateinit var candidateIdeaReader: CandidateIdeaReader
        lateinit var duplicateAnalysisWriter: DuplicateAnalysisWriter
        lateinit var matchCampaignReader: MatchCampaignReader
        lateinit var duplicateAnalysisService: DuplicateAnalysisService

        beforeTest {
            teamReader = mockk()
            candidateIdeaReader = mockk()
            duplicateAnalysisWriter = mockk()
            matchCampaignReader = mockk()
            duplicateAnalysisService = DuplicateAnalysisService(
                teamReader = teamReader,
                candidateIdeaReader = candidateIdeaReader,
                duplicateAnalysisWriter = duplicateAnalysisWriter,
                matchCampaignReader = matchCampaignReader,
                duplicateIdeaAnalyzer = KeywordDuplicateIdeaAnalyzer(),
                clock = fixedClock,
            )
        }

        test("runDuplicateAnalysis redacts other team's private candidate idea match") {
            // given
            val ownerUserId = UserId(10L)
            val teamId = TeamId(1L)
            val targetIdea = candidateIdea(
                id = CandidateIdeaId(1L),
                teamId = teamId,
                title = "Study helper",
                problem = "students lose study plan",
                solution = "study plan automation",
            )
            val otherTeamIdea = candidateIdea(
                id = CandidateIdeaId(2L),
                teamId = TeamId(2L),
                title = "Private competitor",
                problem = "students lose study plan",
                solution = "study plan automation",
            )
            val analysisSlot = slot<DuplicateAnalysis>()
            every { teamReader.getActiveByUserId(ownerUserId) } returns matchEnabledTeam(teamId, ownerUserId)
            every { candidateIdeaReader.getById(CandidateIdeaId(1L)) } returns targetIdea
            every { candidateIdeaReader.getAll() } returns listOf(targetIdea, otherTeamIdea)
            every { matchCampaignReader.getReleasedServiceProfiles() } returns emptyList()
            every { duplicateAnalysisWriter.save(capture(analysisSlot)) } answers {
                analysisSlot.captured.copy(id = DuplicateAnalysisId(9L))
            }

            // when
            val response = duplicateAnalysisService.runDuplicateAnalysis(
                DuplicateAnalysisRunRequest(
                    actorUserId = ownerUserId,
                    candidateIdeaId = CandidateIdeaId(1L),
                ),
            )

            // then
            response.analysisId shouldBe 9L
            response.scannedCandidateIdeaCount shouldBe 2
            response.matches.single().sourceDisclosure shouldBe SourceDisclosure.REDACTED
            response.matches.single().sourceId shouldBe null
            response.matches.single().sourceTeamId shouldBe null
            response.matches.single().sourceTitle shouldBe null
        }
    }) {
    companion object {
        private val baseInstant: Instant = Instant.parse("2026-05-09T23:00:00Z")

        private fun matchEnabledTeam(
            teamId: TeamId,
            ownerUserId: UserId,
        ): Team {
            val team = Team.create(
                name = "Team",
                description = "Description",
                inviteCode = "INVITE123",
                ownerUserId = ownerUserId,
                ownerName = "Owner",
                ownerEmail = "owner@swm.app",
                createdAt = baseInstant,
            ).changeSubServiceActivation(
                subService = SubService.MATCH,
                enabled = true,
                actorUserId = ownerUserId,
                occurredAt = baseInstant.plusSeconds(60),
            )

            return team.copy(
                id = teamId,
                members = team.members.map {
                    it.copy(id = TeamMemberId(1L), teamId = teamId)
                },
                subServiceActivation = SubServiceActivation(
                    calendarEnabled = false,
                    matchEnabled = true,
                    matchEnabledAt = baseInstant.plusSeconds(60),
                ),
            )
        }

        private fun candidateIdea(
            id: CandidateIdeaId,
            teamId: TeamId,
            title: String,
            problem: String,
            solution: String,
        ): CandidateIdea {
            return CandidateIdea.createPrivate(
                teamId = teamId,
                title = title,
                summary = "Summary",
                problem = problem,
                targetUsers = "students",
                solution = solution,
                category = CampaignCategory.EDUCATION,
                platforms = listOf(Platform.WEB),
                createdByUserId = UserId(10L),
                createdAt = baseInstant,
            ).copy(id = id)
        }
    }
}
