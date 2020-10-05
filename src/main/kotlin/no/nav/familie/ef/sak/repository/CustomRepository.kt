package no.nav.familie.ef.sak.repository

import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

//TODO fjern/flytt til test scope
@Component
class CustomRepository(val jdbcAggregateOperations: JdbcAggregateOperations) {

    fun <T> persist(t: T): T {
        return jdbcAggregateOperations.insert(t)
    }

    @Transactional
    fun <T> persistAll(list: List<T>): List<T> {
        return list.map(::persist)
    }
}
