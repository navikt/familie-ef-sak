package no.nav.familie.ef.sak.repository

import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.stereotype.Component

@Component
class CustomRepository(val jdbcAggregateOperations: JdbcAggregateOperations) {

    fun <T> persist(t: T): T {
        return jdbcAggregateOperations.insert(t)
    }
}
