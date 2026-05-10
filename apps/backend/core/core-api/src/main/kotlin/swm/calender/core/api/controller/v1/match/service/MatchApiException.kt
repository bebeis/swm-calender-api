package swm.calender.core.api.controller.v1.match.service

import swm.calender.core.support.error.ErrorType

class MatchApiException(
    val errorType: ErrorType,
    override val message: String,
) : RuntimeException(message) {
    companion object {
        fun badRequest(message: String): MatchApiException = MatchApiException(ErrorType.VALIDATION_ERROR, message)
    }
}
