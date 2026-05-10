package swm.calender.match.exception

enum class MatchErrorMessage(
    val message: String,
) {
    MATCH_SUB_SERVICE_DISABLED("Match sub-service is disabled for this team."),
    SERVICE_PROFILE_NOT_FOUND("Active service profile not found."),
    SERVICE_PROFILE_NAME_REQUIRED("Service profile name is required."),
    SERVICE_PROFILE_SUMMARY_REQUIRED("Service profile summary is required."),
    SERVICE_PROFILE_DESCRIPTION_REQUIRED("Service profile description is required."),
    SERVICE_PROFILE_PLATFORMS_REQUIRED("Service profile platforms are required."),
    SERVICE_PROFILE_DEMO_URL_INVALID("Service profile demo URL must be valid when present."),
    CAMPAIGN_NOT_FOUND("Campaign not found."),
    CAMPAIGN_TITLE_REQUIRED("Campaign title is required."),
    CAMPAIGN_DESCRIPTION_REQUIRED("Campaign description is required."),
    CAMPAIGN_TARGET_TEAM_COUNT_INVALID("Campaign target team count must be positive."),
    CAMPAIGN_DEADLINE_REQUIRED("Campaign deadline is required."),
    CAMPAIGN_DEADLINE_MUST_BE_FUTURE("Campaign deadline must be in the future when opening."),
    CANDIDATE_IDEA_NOT_FOUND("Candidate idea not found."),
    CANDIDATE_IDEA_TITLE_REQUIRED("Candidate idea title is required."),
    CANDIDATE_IDEA_SUMMARY_REQUIRED("Candidate idea summary is required."),
    CANDIDATE_IDEA_PROBLEM_REQUIRED("Candidate idea problem is required."),
    CANDIDATE_IDEA_TARGET_USERS_REQUIRED("Candidate idea target users are required."),
    CANDIDATE_IDEA_SOLUTION_REQUIRED("Candidate idea solution is required."),
    CANDIDATE_IDEA_PLATFORMS_REQUIRED("Candidate idea platforms are required."),
    DUPLICATE_ANALYSIS_NOT_FOUND("Duplicate analysis not found."),
    DUPLICATE_ANALYSIS_MATCH_DIMENSIONS_REQUIRED("Duplicate analysis match dimensions are required."),
    DUPLICATE_ANALYSIS_PRIVATE_SOURCE_REDACTION_REQUIRED(
        "Other team's private candidate idea match must be redacted.",
    ),
}
