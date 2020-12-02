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
    SELECT DISTINCT be.id AS behandlings_id, ty.personIdent as person_ident FROM BEHANDLING b 
        JOIN behandling_ekstern be ON be.behandling_id = b.id
        JOIN tilkjent_ytelse ty on b.id = ty.behandling_id
        JOIN fagsak f on b.fagsak_id = f.id
    WHERE (ty.opphor_fom IS NULL OR ty.opphor_fom > :datoForAvstemming) AND f.stonadstype = :stønadstype AND ty.stonad_tom >= :datoForAvstemming""")
    fun finnAktiveBehandlinger(datoForAvstemming: LocalDate, stønadstype: Stønadstype): List<OppdragIdForFagsystem>
}
