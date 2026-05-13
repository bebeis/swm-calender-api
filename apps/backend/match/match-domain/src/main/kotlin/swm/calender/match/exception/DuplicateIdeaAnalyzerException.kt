package swm.calender.match.exception

class DuplicateIdeaAnalyzerException(
    val errorMessage: MatchErrorMessage,
    cause: Throwable? = null,
) : RuntimeException(errorMessage.message, cause)
