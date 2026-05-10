package swm.calender.match.implement

import org.springframework.stereotype.Component
import swm.calender.match.domain.DuplicateAnalysisRepository
import swm.calender.match.domain.model.DuplicateAnalysis

@Component
class DuplicateAnalysisWriter(
    private val duplicateAnalysisRepository: DuplicateAnalysisRepository,
) {
    fun save(duplicateAnalysis: DuplicateAnalysis): DuplicateAnalysis {
        return duplicateAnalysisRepository.save(duplicateAnalysis)
    }
}
