package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import org.springframework.data.jdbc.repository.query.Query
import java.time.LocalDate
import java.util.UUID

interface TilkjentYtelseRepository : RepositoryInterface<TilkjentYtelse, UUID>, InsertUpdateRepository<TilkjentYtelse> {

    fun findByPersonident(personident: String): TilkjentYtelse?

    fun findByBehandlingId(behandlingId: UUID): TilkjentYtelse?

    // language=PostgreSQL
    @Query("""
        SELECT ty.*
        FROM tilkjent_ytelse ty 
            JOIN behandling b ON b.id = ty.behandling_id
        WHERE ty.behandling_id IN (:behandlingIder) 
         AND EXISTS (SELECT 1 FROM andel_tilkjent_ytelse aty 
                        WHERE ty.id = aty.tilkjent_ytelse AND aty.stonad_tom >= :datoForAvstemming)
          """)
    fun finnTilkjentYtelserTilKonsistensavstemming(behandlingIder: Set<UUID>, datoForAvstemming: LocalDate): List<TilkjentYtelse>

}
