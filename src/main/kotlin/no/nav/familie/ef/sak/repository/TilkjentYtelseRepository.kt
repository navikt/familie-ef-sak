package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TilkjentYtelseRepository : RepositoryInterface<TilkjentYtelse, UUID>,
                                     InsertUpdateRepository<TilkjentYtelse> {

    fun findByPersonident(personident: String): TilkjentYtelse?
}
