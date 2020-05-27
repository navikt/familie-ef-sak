package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface TilkjentYtelseRepository : CrudRepository<TilkjentYtelse, UUID> {

    fun findByPersonident(personident: String): TilkjentYtelse?

}
