package swm.calender.calendar.domain.model

import swm.calender.calendar.exception.CalendarDomainException
import swm.calender.calendar.exception.CalendarErrorMessage
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.When2meetLinkStatus
import java.net.URI
import java.time.Instant

data class When2meetLink(
    val id: Long? = null,
    val teamId: TeamId,
    val url: String,
    val status: When2meetLinkStatus,
    val failureReason: String? = null,
    val lastParsedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        validateUrl(url)
        if (status == When2meetLinkStatus.FAILED && failureReason.isNullOrBlank()) {
            throw CalendarDomainException(CalendarErrorMessage.WHEN2MEET_FAILURE_REASON_REQUIRED)
        }
    }

    fun replace(
        url: String,
        replacedAt: Instant,
    ): When2meetLink {
        validateUrl(url)

        return copy(
            url = url.trim(),
            status = When2meetLinkStatus.PENDING,
            failureReason = null,
            lastParsedAt = null,
            updatedAt = replacedAt,
        )
    }

    fun markParsed(parsedAt: Instant): When2meetLink {
        return copy(
            status = When2meetLinkStatus.PARSED,
            failureReason = null,
            lastParsedAt = parsedAt,
            updatedAt = parsedAt,
        )
    }

    fun markFailed(
        reason: String,
        failedAt: Instant,
    ): When2meetLink {
        if (reason.isBlank()) {
            throw CalendarDomainException(CalendarErrorMessage.WHEN2MEET_FAILURE_REASON_REQUIRED)
        }

        return copy(
            status = When2meetLinkStatus.FAILED,
            failureReason = reason.trim(),
            lastParsedAt = failedAt,
            updatedAt = failedAt,
        )
    }

    companion object {
        fun create(
            teamId: TeamId,
            url: String,
            createdAt: Instant,
        ): When2meetLink {
            validateUrl(url)

            return When2meetLink(
                teamId = teamId,
                url = url.trim(),
                status = When2meetLinkStatus.PENDING,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }

        private fun validateUrl(url: String) {
            if (url.isBlank()) {
                throw CalendarDomainException(CalendarErrorMessage.WHEN2MEET_URL_REQUIRED)
            }

            val uri = runCatching { URI(url.trim()) }
                .getOrElse { throw CalendarDomainException(CalendarErrorMessage.WHEN2MEET_URL_INVALID) }

            if (
                uri.scheme != ALLOWED_SCHEME ||
                !uri.host.equals(ALLOWED_HOST, ignoreCase = true) ||
                uri.userInfo != null ||
                (uri.port != -1 && uri.port != HTTPS_PORT)
            ) {
                throw CalendarDomainException(CalendarErrorMessage.WHEN2MEET_URL_INVALID)
            }
        }

        private const val ALLOWED_SCHEME = "https"
        private const val ALLOWED_HOST = "when2meet.com"
        private const val HTTPS_PORT = 443
    }
}
