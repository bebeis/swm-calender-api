package swm.calender.calendar.exception

enum class CalendarErrorMessage(
    val message: String,
) {
    CALENDAR_SUB_SERVICE_DISABLED("Calendar sub-service is disabled for this team."),
    TEAM_CALENDAR_NOT_FOUND("Team calendar binding not found."),
    TEAM_CALENDAR_ID_REQUIRED("Google Calendar id is required for an active team calendar."),
    TEAM_CALENDAR_AUTH_REQUIRED("Team calendar authorization is required."),
    MENTORING_SCHEDULE_EXTERNAL_SOURCE_ID_REQUIRED("Mentoring schedule external source id is required."),
    MENTORING_SCHEDULE_TITLE_REQUIRED("Mentoring schedule title is required."),
    GOOGLE_EVENT_ID_REQUIRED("Google event id is required."),
    WHEN2MEET_URL_REQUIRED("When2meet URL is required."),
    WHEN2MEET_URL_INVALID("When2meet URL must use HTTPS and an allowed host."),
    WHEN2MEET_FAILURE_REASON_REQUIRED("When2meet failure reason is required when status is FAILED."),
    AVAILABILITY_COUNT_NEGATIVE("Availability counts must be zero or positive."),
}
