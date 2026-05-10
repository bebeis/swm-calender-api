package swm.calender.core.team.service.response

import swm.calender.core.team.domain.model.Team

data class SubServiceActivationResponse(
    val teamId: Long,
    val calendarEnabled: Boolean,
    val matchEnabled: Boolean,
) {
    companion object {
        fun from(team: Team): SubServiceActivationResponse {
            return SubServiceActivationResponse(
                teamId = team.requireId().value,
                calendarEnabled = team.subServiceActivation.calendarEnabled,
                matchEnabled = team.subServiceActivation.matchEnabled,
            )
        }
    }
}
