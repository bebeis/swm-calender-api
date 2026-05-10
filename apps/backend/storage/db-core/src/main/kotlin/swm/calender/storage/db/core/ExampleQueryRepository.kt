package swm.calender.storage.db.core

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExampleQueryRepository {
    fun findByExampleColumn(exampleColumn: String): ExampleEntity? {
        return ExampleTable
            .selectAll()
            .where { ExampleTable.exampleColumn eq exampleColumn }
            .singleOrNull()
            ?.toExampleEntity()
    }
}
