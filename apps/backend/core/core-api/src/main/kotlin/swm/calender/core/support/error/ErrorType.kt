package swm.calender.core.support.error

import org.springframework.boot.logging.LogLevel
import org.springframework.http.HttpStatus

enum class ErrorType(
    val status: HttpStatus,
    val code: ErrorCode,
    val message: String,
    val logLevel: LogLevel,
) {
    VALIDATION_ERROR(
        HttpStatus.BAD_REQUEST,
        ErrorCode.E400,
        "The request is invalid.",
        LogLevel.INFO,
    ),
    AUTHENTICATION_REQUIRED(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.E401,
        "Authentication is required.",
        LogLevel.INFO,
    ),
    FORBIDDEN(
        HttpStatus.FORBIDDEN,
        ErrorCode.E403,
        "You do not have permission to perform this action.",
        LogLevel.INFO,
    ),
    RESOURCE_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        ErrorCode.E404,
        "The requested resource was not found.",
        LogLevel.INFO,
    ),
    DUPLICATE_RESOURCE(
        HttpStatus.CONFLICT,
        ErrorCode.E409,
        "The resource already exists.",
        LogLevel.INFO,
    ),
    INVALID_TEAM_STATE(
        HttpStatus.CONFLICT,
        ErrorCode.E409,
        "The team state does not allow this action.",
        LogLevel.INFO,
    ),
    TEAM_MEMBERSHIP_REQUIRED(
        HttpStatus.FORBIDDEN,
        ErrorCode.E403,
        "Active team membership is required.",
        LogLevel.INFO,
    ),
    SUB_SERVICE_DISABLED(
        HttpStatus.CONFLICT,
        ErrorCode.E409,
        "The requested sub-service is disabled.",
        LogLevel.INFO,
    ),
    DEFAULT_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.E500,
        "An unexpected error has occurred.",
        LogLevel.ERROR,
    ),
}
