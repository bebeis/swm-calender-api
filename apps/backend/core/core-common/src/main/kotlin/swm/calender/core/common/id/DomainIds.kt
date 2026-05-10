package swm.calender.core.common.id

@JvmInline
value class UserId(val value: Long) {
    init {
        require(value > 0) { "UserId must be positive." }
    }
}

@JvmInline
value class TeamId(val value: Long) {
    init {
        require(value > 0) { "TeamId must be positive." }
    }
}

@JvmInline
value class TeamMemberId(val value: Long) {
    init {
        require(value > 0) { "TeamMemberId must be positive." }
    }
}

@JvmInline
value class CampaignId(val value: Long) {
    init {
        require(value > 0) { "CampaignId must be positive." }
    }
}

@JvmInline
value class CandidateIdeaId(val value: Long) {
    init {
        require(value > 0) { "CandidateIdeaId must be positive." }
    }
}

@JvmInline
value class DuplicateAnalysisId(val value: Long) {
    init {
        require(value > 0) { "DuplicateAnalysisId must be positive." }
    }
}

@JvmInline
value class RequestId(val value: Long) {
    init {
        require(value > 0) { "RequestId must be positive." }
    }
}

@JvmInline
value class AssignmentId(val value: Long) {
    init {
        require(value > 0) { "AssignmentId must be positive." }
    }
}

@JvmInline
value class FeedbackId(val value: Long) {
    init {
        require(value > 0) { "FeedbackId must be positive." }
    }
}
