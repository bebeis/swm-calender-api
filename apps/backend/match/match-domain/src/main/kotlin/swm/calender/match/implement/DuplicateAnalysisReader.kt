package swm.calender.match.implement

import org.springframework.stereotype.Component
import swm.calender.core.common.id.DuplicateAnalysisId
import swm.calender.match.domain.DuplicateAnalysisRepository
import swm.calender.match.domain.model.DuplicateAnalysis
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage

@Component
class DuplicateAnalysisReader(
    private val duplicateAnalysisRepository: DuplicateAnalysisRepository,
) {
    fun getById(duplicateAnalysisId: DuplicateAnalysisId): DuplicateAnalysis {
        return duplicateAnalysisRepository.findById(duplicateAnalysisId)
            ?: throw MatchDomainException(MatchErrorMessage.DUPLICATE_ANALYSIS_NOT_FOUND)
    }
}
