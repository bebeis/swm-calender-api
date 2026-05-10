package swm.calender.core.api.controller.v1.team

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import swm.calender.core.api.controller.v1.team.service.TeamApiException
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.support.error.ErrorType
import swm.calender.core.support.response.ApiResponse
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage

internal fun Long.toTeamIdParameter(): TeamId {
    return try {
        TeamId(this)
    } catch (_: IllegalArgumentException) {
        throw TeamApiException.badRequest("teamId must be positive.")
    }
}

internal fun Long.toTeamMemberIdParameter(): TeamMemberId {
    return try {
        TeamMemberId(this)
    } catch (_: IllegalArgumentException) {
        throw TeamApiException.badRequest("memberId must be positive.")
    }
}

internal fun <T> handleTeamAction(action: () -> T): ResponseEntity<ApiResponse<T>> {
    return try {
        ResponseEntity.ok(ApiResponse.success(action()))
    } catch (e: TeamApiException) {
        ResponseEntity
            .status(e.errorType.status)
            .body(ApiResponse.error(e.errorType, mapOf("reason" to e.message)))
    } catch (e: TeamDomainException) {
        val errorType = e.errorMessage.toErrorType()
        ResponseEntity
            .status(errorType.status)
            .body(ApiResponse.error(errorType, mapOf("reason" to e.message)))
    }
}

internal fun <T> teamUnauthorized(): ResponseEntity<ApiResponse<T>> {
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(ErrorType.AUTHENTICATION_REQUIRED))
}

private fun TeamErrorMessage.toErrorType(): ErrorType {
    return when (this) {
        TeamErrorMessage.TEAM_NAME_REQUIRED,
        TeamErrorMessage.INVITE_CODE_REQUIRED,
        TeamErrorMessage.TEAM_ACTIVE_OWNER_REQUIRED,
        TeamErrorMessage.TEAM_MEMBER_NAME_REQUIRED,
        TeamErrorMessage.TEAM_MEMBER_EMAIL_REQUIRED,
        TeamErrorMessage.TEAM_MEMBER_INACTIVE,
        TeamErrorMessage.TEAM_MEMBER_HISTORY_ROLE_REQUIRED,
        TeamErrorMessage.TEAM_NOT_PERSISTED,
        -> ErrorType.VALIDATION_ERROR

        TeamErrorMessage.TEAM_MEMBER_ALREADY_EXISTS,
        TeamErrorMessage.TEAM_ALREADY_EXISTS_FOR_USER,
        -> ErrorType.DUPLICATE_RESOURCE

        TeamErrorMessage.TEAM_OWNER_REQUIRED,
        TeamErrorMessage.TEAM_MEMBER_REQUIRED,
        -> ErrorType.FORBIDDEN

        TeamErrorMessage.TEAM_NOT_FOUND,
        TeamErrorMessage.INVALID_INVITE_CODE,
        TeamErrorMessage.TEAM_MEMBER_NOT_FOUND,
        -> ErrorType.RESOURCE_NOT_FOUND
    }
}
