package swm.calender.calendar.implement

import org.springframework.stereotype.Component
import swm.calender.calendar.domain.CalendarRepository
import swm.calender.calendar.domain.model.AvailabilitySlot
import swm.calender.calendar.domain.model.MentoringSchedule
import swm.calender.calendar.domain.model.TeamCalendar
import swm.calender.calendar.domain.model.When2meetLink
import swm.calender.calendar.exception.CalendarDomainException
import swm.calender.calendar.exception.CalendarErrorMessage
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.time.DateTimeRange

@Component
class CalendarReader(
    private val calendarRepository: CalendarRepository,
) {
    fun getActiveTeamCalendar(teamId: TeamId): TeamCalendar {
        return calendarRepository.findTeamCalendarByTeamId(teamId)
            ?.requireActive()
            ?: throw CalendarDomainException(CalendarErrorMessage.TEAM_CALENDAR_NOT_FOUND)
    }

    fun getMentoringSchedulesByExternalSourceIds(
        teamId: TeamId,
        externalSourceIds: Collection<String>,
    ): List<MentoringSchedule> {
        if (externalSourceIds.isEmpty()) {
            return emptyList()
        }

        return calendarRepository.findMentoringSchedulesByExternalSourceIds(teamId, externalSourceIds)
    }

    fun getMentoringSchedules(
        teamId: TeamId,
        range: DateTimeRange,
    ): List<MentoringSchedule> {
        return calendarRepository.findMentoringSchedulesByTeamIdAndRange(teamId, range)
    }

    fun getWhen2meetLink(teamId: TeamId): When2meetLink? {
        return calendarRepository.findWhen2meetLinkByTeamId(teamId)
    }

    fun getAvailabilitySlots(
        teamId: TeamId,
        range: DateTimeRange,
    ): List<AvailabilitySlot> {
        return calendarRepository.findAvailabilitySlotsByTeamIdAndRange(teamId, range)
    }
}
