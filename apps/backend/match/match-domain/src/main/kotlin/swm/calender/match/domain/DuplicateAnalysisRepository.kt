package swm.calender.match.domain

import swm.calender.core.common.id.DuplicateAnalysisId
import swm.calender.match.domain.model.DuplicateAnalysis

interface DuplicateAnalysisRepository {
    fun save(duplicateAnalysis: DuplicateAnalysis): DuplicateAnalysis

    fun findById(duplicateAnalysisId: DuplicateAnalysisId): DuplicateAnalysis?
}
