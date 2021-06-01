package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Stønadstype
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
            WHERE b.fagsak_id = :fagsakId AND b.status = 'FERDIGSTILT' AND b.type IN ('FØRSTEGANGSBEHANDLING', 'REVURDERING')
            ORDER BY b.opprettet_tid DESC
            LIMIT 1""")
    fun finnSisteTilkjentYtelse(fagsakId: UUID): TilkjentYtelse?

    // language=PostgreSQL
    @Query("""
        SELECT ty.*
        FROM tilkjent_ytelse ty 
            JOIN andel_tilkjent_ytelse aty ON ty.id = aty.tilkjent_ytelse
            JOIN behandling b ON b.id = ty.behandling_id
            JOIN fagsak f ON b.fagsak_id = f.id
        WHERE f.stonadstype = :stønadstype  AND b.status = 'FERDIGSTILT' AND b.type IN ('FØRSTEGANGSBEHANDLING', 'REVURDERING') AND aty.stonad_fom >= :datoForAvstemming
          """)
    fun finnTilkjentYtelserTilKonsistensavstemming(stønadstype: Stønadstype, datoForAvstemming: LocalDate): List<TilkjentYtelse>

}
