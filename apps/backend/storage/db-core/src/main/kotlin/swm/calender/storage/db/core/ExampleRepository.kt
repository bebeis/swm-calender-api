package swm.calender.storage.db.core

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

@Repository
class ExampleRepository {
    fun save(exampleColumn: String): ExampleEntity = transaction {
        val savedId = ExampleTable.insert {
            it[ExampleTable.exampleColumn] = exampleColumn
        }[ExampleTable.id]

        ExampleTable
            .selectAll()
            .where { ExampleTable.id eq savedId }
            .single()
            .toExampleEntity()
    }

    fun findById(id: Long): ExampleEntity? = transaction {
        ExampleTable
            .selectAll()
            .where { ExampleTable.id eq id }
            .singleOrNull()
            ?.toExampleEntity()
    }
}
