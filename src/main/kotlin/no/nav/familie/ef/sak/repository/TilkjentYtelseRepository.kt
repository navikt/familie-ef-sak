package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import java.util.*

interface TilkjentYtelseRepository : RepositoryInterface<TilkjentYtelse, UUID>, InsertUpdateRepository<TilkjentYtelse> {

    fun findByPersonident(personident: String): TilkjentYtelse?

    fun findByBehandlingId(behandlingId: UUID): TilkjentYtelse?
}
