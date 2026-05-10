package swm.calender.calendar.domain

import swm.calender.calendar.domain.model.AvailabilitySlot
import swm.calender.calendar.domain.model.MentoringSchedule
import swm.calender.calendar.domain.model.TeamCalendar
import swm.calender.calendar.domain.model.When2meetLink
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.time.DateTimeRange

interface CalendarRepository {
    fun saveTeamCalendar(teamCalendar: TeamCalendar): TeamCalendar

    fun findTeamCalendarByTeamId(teamId: TeamId): TeamCalendar?

    fun saveMentoringSchedules(schedules: List<MentoringSchedule>): List<MentoringSchedule>

    fun findMentoringSchedulesByExternalSourceIds(
        teamId: TeamId,
        externalSourceIds: Collection<String>,
    ): List<MentoringSchedule>

    fun findMentoringSchedulesByTeamIdAndRange(
        teamId: TeamId,
        range: DateTimeRange,
    ): List<MentoringSchedule>

    fun saveWhen2meetLink(when2meetLink: When2meetLink): When2meetLink

    fun findWhen2meetLinkByTeamId(teamId: TeamId): When2meetLink?

    fun findAvailabilitySlotsByTeamIdAndRange(
        teamId: TeamId,
        range: DateTimeRange,
    ): List<AvailabilitySlot>
}
