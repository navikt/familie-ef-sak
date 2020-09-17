package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import org.springframework.data.repository.CrudRepository
import java.util.*

interface TilkjentYtelseRepository : CrudRepository<TilkjentYtelse, UUID> {

    fun findByPersonident(personident: String): TilkjentYtelse?

}
