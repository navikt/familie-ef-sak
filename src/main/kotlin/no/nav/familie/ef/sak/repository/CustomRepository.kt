package no.nav.familie.ef.sak.repository

import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.stereotype.Component

@Component
class CustomRepository<T>(val jdbcAggregateOperations: JdbcAggregateOperations) {

    fun persist(t: T): T {
        return jdbcAggregateOperations.insert(t)
    }
}
