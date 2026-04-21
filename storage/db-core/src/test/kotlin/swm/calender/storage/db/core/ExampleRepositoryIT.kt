package swm.calender.storage.db.core

import swm.calender.storage.db.CoreDbTestApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(classes = [CoreDbTestApplication::class])
class ExampleRepositoryIT {
    @Autowired
    private lateinit var exampleRepository: ExampleRepository

    @Test
    fun testShouldBeSavedAndFound() {
        val saved = exampleRepository.save(ExampleEntity("SPRING_BOOT"))
        assertThat(saved.exampleColumn).isEqualTo("SPRING_BOOT")

        val found = exampleRepository.findById(saved.id).get()
        assertThat(found.exampleColumn).isEqualTo("SPRING_BOOT")
    }
}
