package swm.calender.client.google.calendar

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import swm.calender.core.common.time.DateTimeRange
import java.util.UUID

interface GoogleCalendarClient {
    fun createEvent(request: GoogleCalendarCreateEventRequest): GoogleCalendarCreateEventResponse
}

data class GoogleCalendarCreateEventRequest(
    val googleCalendarId: String,
    val externalSourceId: String,
    val title: String,
    val range: DateTimeRange,
    val location: String? = null,
    val description: String? = null,
)

data class GoogleCalendarCreateEventResponse(
    val googleEventId: String,
)

@Configuration
class GoogleCalendarClientConfiguration {
    @Bean
    @ConditionalOnMissingBean(GoogleCalendarClient::class)
    fun googleCalendarClient(): GoogleCalendarClient {
        return MvpGoogleCalendarClient()
    }
}

private class MvpGoogleCalendarClient : GoogleCalendarClient {
    override fun createEvent(request: GoogleCalendarCreateEventRequest): GoogleCalendarCreateEventResponse {
        return GoogleCalendarCreateEventResponse(
            googleEventId = UUID.nameUUIDFromBytes(
                "${request.googleCalendarId}:${request.externalSourceId}".toByteArray(),
            ).toString(),
        )
    }
}
