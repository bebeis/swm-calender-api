package swm.calender.match.exception

class MatchDomainException(
    val errorMessage: MatchErrorMessage,
) : RuntimeException(errorMessage.message)
