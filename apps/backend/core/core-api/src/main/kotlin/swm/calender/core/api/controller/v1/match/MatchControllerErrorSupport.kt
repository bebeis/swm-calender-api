package swm.calender.core.api.controller.v1.match

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import swm.calender.core.api.controller.v1.match.service.MatchApiException
import swm.calender.core.support.error.ErrorType
import swm.calender.core.support.response.ApiResponse
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage

internal fun <T> handleMatchAction(action: () -> T): ResponseEntity<ApiResponse<T>> {
    return try {
        ResponseEntity.ok(ApiResponse.success(action()))
    } catch (e: MatchApiException) {
        ResponseEntity
            .status(e.errorType.status)
            .body(ApiResponse.error(e.errorType, mapOf("reason" to e.message)))
    } catch (e: MatchDomainException) {
        val errorType = e.errorMessage.toErrorType()
        ResponseEntity
            .status(errorType.status)
            .body(ApiResponse.error(errorType, mapOf("reason" to e.message)))
    } catch (e: TeamDomainException) {
        val errorType = e.errorMessage.toErrorType()
        ResponseEntity
            .status(errorType.status)
            .body(ApiResponse.error(errorType, mapOf("reason" to e.message)))
    }
}

internal fun <T> matchUnauthorized(): ResponseEntity<ApiResponse<T>> {
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(ErrorType.AUTHENTICATION_REQUIRED))
}

private fun MatchErrorMessage.toErrorType(): ErrorType {
    return when (this) {
        MatchErrorMessage.MATCH_SUB_SERVICE_DISABLED -> ErrorType.SUB_SERVICE_DISABLED
        MatchErrorMessage.SERVICE_PROFILE_NOT_FOUND,
        MatchErrorMessage.CAMPAIGN_NOT_FOUND,
        MatchErrorMessage.CANDIDATE_IDEA_NOT_FOUND,
        MatchErrorMessage.DUPLICATE_ANALYSIS_NOT_FOUND,
        MatchErrorMessage.MATCH_REQUEST_NOT_FOUND,
        MatchErrorMessage.ASSIGNMENT_NOT_FOUND,
        MatchErrorMessage.FEEDBACK_NOT_FOUND,
        -> ErrorType.RESOURCE_NOT_FOUND

        MatchErrorMessage.MATCH_REQUEST_DUPLICATED,
        MatchErrorMessage.FEEDBACK_DUPLICATED,
        -> ErrorType.DUPLICATE_RESOURCE

        MatchErrorMessage.CAMPAIGN_DEADLINE_MUST_BE_FUTURE,
        MatchErrorMessage.MATCH_REQUEST_SELF_REQUEST_NOT_ALLOWED,
        MatchErrorMessage.MATCH_REQUEST_RECIPROCAL_CAMPAIGN_REQUIRED,
        MatchErrorMessage.MATCH_REQUEST_RECIPROCAL_UNAVAILABLE,
        MatchErrorMessage.MATCH_REQUEST_STATUS_CHANGE_UNSUPPORTED,
        MatchErrorMessage.MATCH_REQUEST_FINAL_STATUS,
        MatchErrorMessage.ASSIGNMENT_REQUEST_NOT_ACCEPTED,
        MatchErrorMessage.ASSIGNMENT_SELF_ASSIGNMENT_NOT_ALLOWED,
        MatchErrorMessage.ASSIGNMENT_FEEDBACK_UNAVAILABLE,
        MatchErrorMessage.FEEDBACK_SUBMITTER_NOT_TESTER_TEAM,
        -> ErrorType.INVALID_TEAM_STATE

        MatchErrorMessage.SERVICE_PROFILE_NAME_REQUIRED,
        MatchErrorMessage.SERVICE_PROFILE_SUMMARY_REQUIRED,
        MatchErrorMessage.SERVICE_PROFILE_DESCRIPTION_REQUIRED,
        MatchErrorMessage.SERVICE_PROFILE_PLATFORMS_REQUIRED,
        MatchErrorMessage.SERVICE_PROFILE_DEMO_URL_INVALID,
        MatchErrorMessage.CAMPAIGN_TITLE_REQUIRED,
        MatchErrorMessage.CAMPAIGN_DESCRIPTION_REQUIRED,
        MatchErrorMessage.CAMPAIGN_TARGET_TEAM_COUNT_INVALID,
        MatchErrorMessage.CAMPAIGN_DEADLINE_REQUIRED,
        MatchErrorMessage.CANDIDATE_IDEA_TITLE_REQUIRED,
        MatchErrorMessage.CANDIDATE_IDEA_SUMMARY_REQUIRED,
        MatchErrorMessage.CANDIDATE_IDEA_PROBLEM_REQUIRED,
        MatchErrorMessage.CANDIDATE_IDEA_TARGET_USERS_REQUIRED,
        MatchErrorMessage.CANDIDATE_IDEA_SOLUTION_REQUIRED,
        MatchErrorMessage.CANDIDATE_IDEA_PLATFORMS_REQUIRED,
        MatchErrorMessage.DUPLICATE_ANALYSIS_MATCH_DIMENSIONS_REQUIRED,
        MatchErrorMessage.DUPLICATE_ANALYSIS_PRIVATE_SOURCE_REDACTION_REQUIRED,
        MatchErrorMessage.MATCH_REQUEST_MESSAGE_TOO_LONG,
        MatchErrorMessage.FEEDBACK_SCORE_INVALID,
        MatchErrorMessage.FEEDBACK_SUMMARY_LENGTH_INVALID,
        MatchErrorMessage.FEEDBACK_IMPROVEMENT_SUGGESTION_TOO_LONG,
        MatchErrorMessage.NOTIFICATION_MESSAGE_REQUIRED,
        -> ErrorType.VALIDATION_ERROR
    }
}

private fun TeamErrorMessage.toErrorType(): ErrorType {
    return when (this) {
        TeamErrorMessage.TEAM_OWNER_REQUIRED,
        TeamErrorMessage.TEAM_MEMBER_REQUIRED,
        -> ErrorType.FORBIDDEN

        TeamErrorMessage.TEAM_NOT_FOUND,
        TeamErrorMessage.INVALID_INVITE_CODE,
        TeamErrorMessage.TEAM_MEMBER_NOT_FOUND,
        -> ErrorType.RESOURCE_NOT_FOUND

        TeamErrorMessage.TEAM_MEMBER_ALREADY_EXISTS,
        TeamErrorMessage.TEAM_ALREADY_EXISTS_FOR_USER,
        -> ErrorType.DUPLICATE_RESOURCE

        TeamErrorMessage.TEAM_NAME_REQUIRED,
        TeamErrorMessage.INVITE_CODE_REQUIRED,
        TeamErrorMessage.TEAM_ACTIVE_OWNER_REQUIRED,
        TeamErrorMessage.TEAM_MEMBER_NAME_REQUIRED,
        TeamErrorMessage.TEAM_MEMBER_EMAIL_REQUIRED,
        TeamErrorMessage.TEAM_MEMBER_INACTIVE,
        TeamErrorMessage.TEAM_MEMBER_HISTORY_ROLE_REQUIRED,
        TeamErrorMessage.TEAM_NOT_PERSISTED,
        -> ErrorType.VALIDATION_ERROR
    }
}
