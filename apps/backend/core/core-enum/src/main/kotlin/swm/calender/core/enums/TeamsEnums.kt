package swm.calender.core.enums

enum class TeamMemberRole {
    OWNER,
    MEMBER,
}

enum class SubService {
    CALENDAR,
    MATCH,
}

enum class CalendarStatus {
    ACTIVE,
    AUTH_REQUIRED,
    DISABLED,
}

enum class When2meetLinkStatus {
    PENDING,
    PARSED,
    FAILED,
}

enum class AvailabilitySource {
    GOOGLE_CALENDAR,
    WHEN2MEET,
}

enum class CampaignCategory {
    PRODUCTIVITY,
    EDUCATION,
    COMMUNITY,
    HEALTH,
    FINANCE,
    DEVELOPER_TOOL,
    ENTERTAINMENT,
    LIFESTYLE,
    OTHER,
}

enum class Platform {
    WEB,
    ANDROID,
    IOS,
    CHROME_EXTENSION,
    DESKTOP,
    API,
    OTHER,
}

enum class CandidateIdeaVisibility {
    PRIVATE,
}

enum class DuplicateAnalysisStatus {
    COMPLETED,
    FAILED,
}

enum class DuplicateAnalysisSourceType {
    RELEASED_SERVICE,
    PRIVATE_CANDIDATE_IDEA,
}

enum class SourceDisclosure {
    PUBLIC,
    OWN_TEAM,
    REDACTED,
}

enum class SimilarityLevel {
    LOW,
    MEDIUM,
    HIGH,
}

enum class CampaignStatus {
    DRAFT,
    OPEN,
    CLOSED,
}

enum class MatchRequestType {
    ONE_WAY,
    RECIPROCAL,
}

enum class MatchRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELED,
}

enum class AssignmentStatus {
    ASSIGNED,
    FEEDBACK_SUBMITTED,
    CLOSED,
}

enum class FeedbackScoreType {
    USABILITY,
    VALUE,
    RELIABILITY,
    RECOMMENDATION,
}

enum class NotificationType {
    REQUEST_RECEIVED,
    REQUEST_ACCEPTED,
    REQUEST_REJECTED,
    REQUEST_CANCELED,
    FEEDBACK_REQUESTED,
    FEEDBACK_SUBMITTED,
}
