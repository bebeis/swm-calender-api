package swm.calender.storage.db.core

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

data class ExampleEntity(
    val id: Long,
    val exampleColumn: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

object ExampleTable : Table("example_entity") {
    val id = long("id").autoIncrement()
    val exampleColumn = varchar("example_column", 255)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}

internal fun ResultRow.toExampleEntity(): ExampleEntity = ExampleEntity(
    id = this[ExampleTable.id],
    exampleColumn = this[ExampleTable.exampleColumn],
    createdAt = this[ExampleTable.createdAt],
    updatedAt = this[ExampleTable.updatedAt],
)
