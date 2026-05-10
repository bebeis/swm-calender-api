package swm.calender.core.team.domain.model

import swm.calender.core.enums.SubService
import java.time.Instant

data class SubServiceActivation(
    val calendarEnabled: Boolean,
    val matchEnabled: Boolean,
    val calendarEnabledAt: Instant? = null,
    val matchEnabledAt: Instant? = null,
    val calendarDisabledAt: Instant? = null,
    val matchDisabledAt: Instant? = null,
) {
    fun change(
        subService: SubService,
        enabled: Boolean,
        occurredAt: Instant,
    ): SubServiceActivation {
        return when (subService) {
            SubService.CALENDAR -> changeCalendar(enabled, occurredAt)
            SubService.MATCH -> changeMatch(enabled, occurredAt)
        }
    }

    private fun changeCalendar(
        enabled: Boolean,
        occurredAt: Instant,
    ): SubServiceActivation {
        return if (enabled) {
            if (calendarEnabled) {
                this
            } else {
                copy(
                    calendarEnabled = true,
                    calendarEnabledAt = occurredAt,
                )
            }
        } else {
            if (!calendarEnabled) {
                this
            } else {
                copy(
                    calendarEnabled = false,
                    calendarDisabledAt = occurredAt,
                )
            }
        }
    }

    private fun changeMatch(
        enabled: Boolean,
        occurredAt: Instant,
    ): SubServiceActivation {
        return if (enabled) {
            if (matchEnabled) {
                this
            } else {
                copy(
                    matchEnabled = true,
                    matchEnabledAt = occurredAt,
                )
            }
        } else {
            if (!matchEnabled) {
                this
            } else {
                copy(
                    matchEnabled = false,
                    matchDisabledAt = occurredAt,
                )
            }
        }
    }

    companion object {
        fun inactive(): SubServiceActivation {
            return SubServiceActivation(
                calendarEnabled = false,
                matchEnabled = false,
            )
        }
    }
}
