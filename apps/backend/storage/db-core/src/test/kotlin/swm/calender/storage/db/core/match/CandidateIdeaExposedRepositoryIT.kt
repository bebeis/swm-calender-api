package swm.calender.storage.db.core.match

import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.beans.factory.annotation.Autowired
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.DuplicateAnalysisSourceType
import swm.calender.core.enums.OverlapDimension
import swm.calender.core.enums.Platform
import swm.calender.core.enums.SimilarityLevel
import swm.calender.core.enums.SourceDisclosure
import swm.calender.match.domain.model.CandidateIdea
import swm.calender.match.domain.model.DuplicateAnalysis
import swm.calender.match.domain.model.DuplicateAnalysisMatch
import swm.calender.storage.db.core.RepositoryTestSupport
import swm.calender.storage.db.core.team.SubServiceActivationTable
import swm.calender.storage.db.core.team.TeamMemberTable
import swm.calender.storage.db.core.team.TeamTable
import java.time.Instant

class CandidateIdeaExposedRepositoryIT : RepositoryTestSupport() {
    @Autowired
    private lateinit var candidateIdeaExposedRepository: CandidateIdeaExposedRepository

    @Autowired
    private lateinit var duplicateAnalysisExposedRepository: DuplicateAnalysisExposedRepository

    init {
        extension(SpringExtension())

        val baseInstant = Instant.parse("2026-05-10T00:00:00Z")

        beforeTest {
            cleanMatchTables()
        }

        test("save and findByTeamId keep candidate ideas scoped to the owning team") {
            // given
            val teamId = createTeam("Idea Team", "IDEA-TEAM")
            val otherTeamId = createTeam("Other Idea Team", "OTHER-IDEA-TEAM")
            val savedIdea = candidateIdeaExposedRepository.save(
                candidateIdea(teamId, "Study helper", baseInstant),
            )
            candidateIdeaExposedRepository.save(
                candidateIdea(otherTeamId, "Other helper", baseInstant.plusSeconds(60)),
            )

            // when
            val foundIdeas = candidateIdeaExposedRepository.findByTeamId(teamId)
            val foundIdea = candidateIdeaExposedRepository.findById(requireNotNull(savedIdea.id))

            // then
            foundIdeas.shouldHaveSize(1)
            foundIdeas.single().teamId shouldBe teamId
            foundIdea?.title shouldBe "Study helper"
            foundIdea?.platforms shouldBe listOf(Platform.WEB, Platform.API)
        }

        test("save duplicate analysis stores redacted private-source matches without identifiers") {
            // given
            val teamId = createTeam("Analysis Team", "ANALYSIS-TEAM")
            val savedIdea = candidateIdeaExposedRepository.save(
                candidateIdea(teamId, "Study helper", baseInstant),
            )

            // when
            val savedAnalysis = duplicateAnalysisExposedRepository.save(
                DuplicateAnalysis.completed(
                    candidateIdeaId = requireNotNull(savedIdea.id),
                    requestedByTeamId = teamId,
                    requestedByUserId = UserId(10L),
                    scannedReleasedServiceCount = 0,
                    scannedCandidateIdeaCount = 2,
                    matches = listOf(
                        DuplicateAnalysisMatch(
                            sourceType = DuplicateAnalysisSourceType.PRIVATE_CANDIDATE_IDEA,
                            sourceId = null,
                            sourceTeamId = null,
                            sourceTitle = null,
                            sourceDisclosure = SourceDisclosure.REDACTED,
                            similarityLevel = SimilarityLevel.HIGH,
                            overlapDimensions = listOf(OverlapDimension.PROBLEM, OverlapDimension.SOLUTION),
                            overlapSummary = "Another team's private candidate idea has non-identifying overlap.",
                        ),
                    ),
                    generatedAt = baseInstant.plusSeconds(120),
                ),
            )
            val foundAnalysis = duplicateAnalysisExposedRepository.findById(requireNotNull(savedAnalysis.id))

            // then
            val matches = requireNotNull(foundAnalysis).matches
            matches.shouldHaveSize(1)
            matches.single().sourceDisclosure shouldBe SourceDisclosure.REDACTED
            matches.single().sourceId shouldBe null
            matches.single().sourceTeamId shouldBe null
            matches.single().sourceTitle shouldBe null
        }
    }

    private fun cleanMatchTables() {
        transaction {
            MatchNotificationTable.deleteAll()
            FeedbackTable.deleteAll()
            AssignmentTable.deleteAll()
            MatchRequestStatusHistoryTable.deleteAll()
            MatchRequestTable.deleteAll()
            DuplicateAnalysisMatchDimensionTable.deleteAll()
            DuplicateAnalysisMatchTable.deleteAll()
            DuplicateAnalysisTable.deleteAll()
            CandidateIdeaPlatformTable.deleteAll()
            CandidateIdeaTable.deleteAll()
            BetaCampaignTable.deleteAll()
            ActiveServiceProfileTable.deleteAll()
            ServiceProfileScreenshotTable.deleteAll()
            ServiceProfilePlatformTable.deleteAll()
            ServiceProfileTable.deleteAll()
            TeamMemberTable.deleteAll()
            SubServiceActivationTable.deleteAll()
            TeamTable.deleteAll()
        }
    }

    private fun createTeam(
        name: String,
        inviteCode: String,
    ): TeamId = transaction {
        val now = Instant.parse("2026-05-10T00:00:00Z")
        val savedTeamId = TeamTable.insert {
            it[TeamTable.name] = name
            it[description] = "$name description"
            it[TeamTable.inviteCode] = inviteCode
            it[createdAt] = now.toLocalDateTime()
            it[updatedAt] = now.toLocalDateTime()
        }[TeamTable.id]

        SubServiceActivationTable.insert {
            it[teamId] = savedTeamId
            it[calendarEnabled] = false
            it[matchEnabled] = true
            it[calendarEnabledAt] = null
            it[matchEnabledAt] = now.toLocalDateTime()
            it[calendarDisabledAt] = null
            it[matchDisabledAt] = null
        }

        TeamId(savedTeamId)
    }

    private fun candidateIdea(
        teamId: TeamId,
        title: String,
        createdAt: Instant,
    ): CandidateIdea {
        return CandidateIdea.createPrivate(
            teamId = teamId,
            title = title,
            summary = "$title summary",
            problem = "Students lose study plan",
            targetUsers = "Students",
            solution = "Study plan automation",
            category = CampaignCategory.EDUCATION,
            platforms = listOf(Platform.WEB, Platform.API),
            createdByUserId = UserId(10L),
            createdAt = createdAt,
        )
    }
}
