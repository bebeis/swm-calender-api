package swm.calender.core.api.controller.v1.calendar.service

import jakarta.validation.Validator
import org.springframework.stereotype.Component
import swm.calender.core.api.controller.v1.calendar.request.MentoringScheduleBulkPushRequest
import swm.calender.core.api.controller.v1.calendar.request.When2meetLinkRequest
import swm.calender.core.common.time.DateTimeRange
import java.net.URI
import java.time.OffsetDateTime

@Component
class CalendarRequestValidator(
    private val validator: Validator,
) {
    fun validateBulkPush(request: MentoringScheduleBulkPushRequest) {
        val schedules = request.schedules
            ?: throw CalendarApiException.badRequest("schedules is required.")
        if (schedules.isEmpty()) {
            throw CalendarApiException.badRequest("schedules must not be empty.")
        }
        validateBean(request)

        schedules.forEachIndexed { index, schedule ->
            if (schedule.externalSourceId.isNullOrBlank()) {
                throw CalendarApiException.badRequest("schedules[$index].externalSourceId must not be blank.")
            }
            if (schedule.title.isNullOrBlank()) {
                throw CalendarApiException.badRequest("schedules[$index].title must not be blank.")
            }
            val startsAt = schedule.startsAt
                ?: throw CalendarApiException.badRequest("schedules[$index].startsAt is required.")
            val endsAt = schedule.endsAt
                ?: throw CalendarApiException.badRequest("schedules[$index].endsAt is required.")
            validateRange(startsAt, endsAt, "schedules[$index]")
        }
    }

    fun validateWhen2meetLink(request: When2meetLinkRequest) {
        validateBean(request)
        val url = request.url
        if (url.isNullOrBlank()) {
            throw CalendarApiException.badRequest("url must not be blank.")
        }

        validateAllowedWhen2meetUrl(url)
    }

    fun parseRange(
        startsAt: String?,
        endsAt: String?,
    ): Pair<OffsetDateTime, OffsetDateTime> {
        val parsedStartsAt = parseDateTime(startsAt, "startsAt")
        val parsedEndsAt = parseDateTime(endsAt, "endsAt")
        validateRange(parsedStartsAt, parsedEndsAt, "availability range")
        return parsedStartsAt to parsedEndsAt
    }

    private fun parseDateTime(
        value: String?,
        fieldName: String,
    ): OffsetDateTime {
        if (value.isNullOrBlank()) {
            throw CalendarApiException.badRequest("$fieldName is required.")
        }

        return runCatching { OffsetDateTime.parse(value) }
            .getOrElse { throw CalendarApiException.badRequest("$fieldName must be an ISO-8601 date-time.") }
    }

    private fun validateRange(
        startsAt: OffsetDateTime,
        endsAt: OffsetDateTime,
        context: String,
    ) {
        runCatching { DateTimeRange(startsAt = startsAt, endsAt = endsAt) }
            .getOrElse { throw CalendarApiException.badRequest("$context endsAt must be after startsAt.") }
    }

    private fun validateBean(request: Any) {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw CalendarApiException.badRequest(violations.first().message)
        }
    }

    private fun validateAllowedWhen2meetUrl(url: String) {
        val uri = runCatching { URI(url.trim()) }
            .getOrElse { throw CalendarApiException.badRequest("url must be a valid When2meet URL.") }

        if (
            uri.scheme != ALLOWED_WHEN2MEET_SCHEME ||
            !uri.host.equals(ALLOWED_WHEN2MEET_HOST, ignoreCase = true) ||
            uri.userInfo != null ||
            (uri.port != -1 && uri.port != HTTPS_PORT)
        ) {
            throw CalendarApiException.badRequest("url must be an https://when2meet.com URL.")
        }
    }

    companion object {
        private const val ALLOWED_WHEN2MEET_SCHEME = "https"
        private const val ALLOWED_WHEN2MEET_HOST = "when2meet.com"
        private const val HTTPS_PORT = 443
    }
}
