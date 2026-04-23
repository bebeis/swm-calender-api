package swm.calender.storage.db.core

import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class ExampleQueryRepository(
    private val jpaQueryFactory: JPAQueryFactory,
) {
    fun findByExampleColumn(exampleColumn: String): ExampleEntity? {
        val exampleEntity = QExampleEntity.exampleEntity

        return jpaQueryFactory
            .selectFrom(exampleEntity)
            .where(exampleEntity.exampleColumn.eq(exampleColumn))
            .fetchOne()
    }
}
