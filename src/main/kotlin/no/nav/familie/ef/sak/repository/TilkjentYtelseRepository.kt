package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.kontrakter.felles.oppdrag.OppdragIdForFagsystem
import org.springframework.data.jdbc.repository.query.Query
import java.time.LocalDate
import java.util.*

interface TilkjentYtelseRepository : RepositoryInterface<TilkjentYtelse, UUID>, InsertUpdateRepository<TilkjentYtelse> {

    fun findByPersonident(personident: String): TilkjentYtelse?

    @Query("""
    SELECT ty.* from tilkjent_ytelse ty
        JOIN behandling b ON b.id = ty.behandling_id
        WHERE b.fagsak_id = :fagsakId AND b.status = 'FERDIGSTILT'
        ORDER BY b.opprettet_tid DESC
        LIMIT 1""")
    fun finnNyesteTilkjentYtelse(fagsakId: UUID): TilkjentYtelse?

    @Query("""
    WITH sisteBehandlinger AS (
    SELECT behandling_id FROM (
                      SELECT b.id as behandling_id, row_number() over (PARTITION BY b.fagsak_id ORDER BY ty.opprettet_tid DESC) rn
                      FROM behandling b
                               JOIN fagsak f on b.fagsak_id = f.id
                               JOIN tilkjent_ytelse ty on b.id = ty.behandling_id -- att man har en tilkjent_ytelse
                      WHERE b.status = 'FERDIGSTILT'
                        AND f.stonadstype = :stønadstype
                  ) q WHERE rn=1
    ) SELECT DISTINCT be.id as behandlings_id, t.personIdent as person_ident
    FROM andel_tilkjent_ytelse aty
    JOIN tilkjent_ytelse t on t.id = aty.tilkjent_ytelse
    JOIN behandling_ekstern be ON be.behandling_id = aty.ursprungsbehandling_id
    WHERE t.behandling_id IN (SELECT behandling_id FROM sisteBehandlinger)
    AND aty.stonad_tom >= :datoForAvstemming    
    """)
    fun finnAktiveBehandlinger(datoForAvstemming: LocalDate, stønadstype: Stønadstype): List<OppdragIdForFagsystem>
}
