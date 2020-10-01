package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import java.util.*

interface TilkjentYtelseRepository : RepositoryInterface<TilkjentYtelse, UUID> {

    fun findByPersonident(personident: String): TilkjentYtelse?

}
