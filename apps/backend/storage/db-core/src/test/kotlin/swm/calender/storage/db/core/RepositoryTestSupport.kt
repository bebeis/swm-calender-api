package swm.calender.storage.db.core

import io.kotest.core.spec.style.FunSpec
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import swm.calender.storage.db.CoreDbTestApplication

@ActiveProfiles("test")
@SpringBootTest(classes = [CoreDbTestApplication::class])
abstract class RepositoryTestSupport : FunSpec()
