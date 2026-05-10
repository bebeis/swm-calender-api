package swm.calender.core.team.exception

class TeamDomainException(
    val errorMessage: TeamErrorMessage,
) : RuntimeException(errorMessage.message)
