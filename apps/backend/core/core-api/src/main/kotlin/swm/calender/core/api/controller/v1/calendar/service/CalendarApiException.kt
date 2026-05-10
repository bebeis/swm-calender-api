package swm.calender.core.api.controller.v1.calendar.service

import swm.calender.core.support.error.ErrorType

class CalendarApiException(
    val errorType: ErrorType,
    override val message: String,
) : RuntimeException(message) {
    companion object {
        fun badRequest(message: String): CalendarApiException {
            return CalendarApiException(ErrorType.VALIDATION_ERROR, message)
        }
    }
}
