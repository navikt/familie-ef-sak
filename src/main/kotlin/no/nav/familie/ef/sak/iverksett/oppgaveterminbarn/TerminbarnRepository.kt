package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.BarnTilUtplukkForOppgave
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository interface TerminbarnRepository : RepositoryInterface<TerminbarnOppgave, UUID>,
                                             InsertUpdateRepository<TerminbarnOppgave> {

    @Query
    public fun existsByFagsakIdAndTermindato(fagsakId: UUID, termindato: LocalDate): Boolean

    // language=PostgreSQL
    @Query("""
        SELECT b.id behandling_id, pi.ident fodselsnummer_soker, bb.person_ident fodselsnummer_barn, 
          bb.fodsel_termindato termindato_barn, FALSE AS fra_migrering
        FROM gjeldende_iverksatte_behandlinger b
         JOIN fagsak f ON f.id = b.fagsak_id
         JOIN (SELECT DISTINCT ON(pi.fagsak_person_id) * FROM person_ident pi ORDER BY pi.fagsak_person_id, pi.opprettet_tid DESC) pi ON pi.fagsak_person_id = f.fagsak_person_id
         JOIN behandling_barn bb ON bb.behandling_id = b.id
        WHERE bb.person_ident IS NULL AND b.stonadstype=:stønadstype AND EXISTS(SELECT 1 FROM andel_tilkjent_ytelse aty
            JOIN tilkjent_ytelse ty ON aty.tilkjent_ytelse = ty.id
            WHERE ty.id = aty.tilkjent_ytelse 
            AND ty.behandling_id = b.id
          AND aty.stonad_tom >= :dato
          AND aty.belop > 0)
        """)
    fun finnBarnAvGjeldendeIverksatteBehandlingerKunTerminbarn(stønadstype: StønadType,
                                                               dato: LocalDate): List<BarnTilUtplukkForOppgave>
}
