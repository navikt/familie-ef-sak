package no.nav.familie.ef.sak.repository

import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Component
import java.util.*

interface SakRepository : CrudRepository<Sak, UUID>

@Component
class CustomSakRepository(val jdbcAggregateOperations: JdbcAggregateOperations) {

    fun persist(sak: Sak): Sak {
        return jdbcAggregateOperations.insert(sak)
    }
}
