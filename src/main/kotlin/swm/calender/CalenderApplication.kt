package swm.calender

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CalenderApplication

fun main(args: Array<String>) {
	runApplication<CalenderApplication>(*args)
}
