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
        SELECT behandling_id FROM (
            SELECT b.id as behandling_id, row_number() over (PARTITION BY b.fagsak_id ORDER BY ty.opprettet_tid DESC) rn
                FROM behandling b
                    JOIN fagsak f on b.fagsak_id = f.id
                    JOIN tilkjent_ytelse ty on b.id = ty.behandling_id -- att man har en tilkjent_ytelse
                WHERE b.status = 'FERDIGSTILT' AND f.stonadstype = :stønadstype
        ) q WHERE rn=1
        """)
    fun finnSisteBehandlingForFagsak(stønadstype: Stønadstype): List<UUID>

    // language=PostgreSQL
    @Query("""
        SELECT DISTINCT be.id as first, aty.periode_id as second
        FROM andel_tilkjent_ytelse aty
            JOIN tilkjent_ytelse t on t.id = aty.tilkjent_ytelse
            JOIN behandling b on b.id = t.behandling_id
            JOIN behandling_ekstern be ON be.behandling_id = aty.kilde_behandling_id
        WHERE t.behandling_id IN (:sisteBehandlinger)
            AND aty.stonad_tom >= :datoForAvstemming
    """)
    fun finnKildeBehandlingIdFraAndelTilkjentYtelse(datoForAvstemming: LocalDate, sisteBehandlinger: List<UUID>): List<Pair<Long, Long>>

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
