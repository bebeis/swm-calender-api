package swm.calender.storage.db.core

import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import swm.calender.storage.db.CoreDbTestApplication
import kotlin.test.Test

@ActiveProfiles("test")
@SpringBootTest(classes = [CoreDbTestApplication::class])
@Transactional
class ExampleRepositoryIT {
    @Autowired
    private lateinit var exampleRepository: ExampleRepository

    @Autowired
    private lateinit var exampleQueryRepository: ExampleQueryRepository

    @Test
    fun testShouldBeSavedAndFound() {
        val saved = exampleRepository.save("SPRING_BOOT")
        assertThat(saved.exampleColumn).isEqualTo("SPRING_BOOT")

        val found = exampleRepository.findById(saved.id)
        assertThat(found).isNotNull
        assertThat(found!!.exampleColumn).isEqualTo("SPRING_BOOT")

        val foundByExposed = exampleQueryRepository.findByExampleColumn("SPRING_BOOT")
        assertThat(foundByExposed).isNotNull
        assertThat(foundByExposed!!.id).isEqualTo(saved.id)
    }
}
