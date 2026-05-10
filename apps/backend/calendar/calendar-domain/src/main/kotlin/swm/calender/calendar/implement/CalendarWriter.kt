package swm.calender.calendar.implement

import org.springframework.stereotype.Component
import swm.calender.calendar.domain.CalendarRepository
import swm.calender.calendar.domain.model.MentoringSchedule
import swm.calender.calendar.domain.model.TeamCalendar
import swm.calender.calendar.domain.model.When2meetLink

@Component
class CalendarWriter(
    private val calendarRepository: CalendarRepository,
) {
    fun saveTeamCalendar(teamCalendar: TeamCalendar): TeamCalendar {
        return calendarRepository.saveTeamCalendar(teamCalendar)
    }

    fun saveMentoringSchedules(schedules: List<MentoringSchedule>): List<MentoringSchedule> {
        if (schedules.isEmpty()) {
            return emptyList()
        }

        return calendarRepository.saveMentoringSchedules(schedules)
    }

    fun saveWhen2meetLink(when2meetLink: When2meetLink): When2meetLink {
        return calendarRepository.saveWhen2meetLink(when2meetLink)
    }
}
