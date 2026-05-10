package swm.calender.calendar.exception

class CalendarDomainException(
    val errorMessage: CalendarErrorMessage,
) : RuntimeException(errorMessage.message)
