package swm.calender.storage.db.core

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExampleRepository {
    fun save(exampleColumn: String): ExampleEntity {
        val savedId = ExampleTable.insert {
            it[ExampleTable.exampleColumn] = exampleColumn
        }[ExampleTable.id]

        return ExampleTable
            .selectAll()
            .where { ExampleTable.id eq savedId }
            .single()
            .toExampleEntity()
    }

    fun findById(id: Long): ExampleEntity? {
        return ExampleTable
            .selectAll()
            .where { ExampleTable.id eq id }
            .singleOrNull()
            ?.toExampleEntity()
    }
}
