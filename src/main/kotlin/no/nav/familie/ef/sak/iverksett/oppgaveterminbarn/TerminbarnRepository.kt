package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
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
        SELECT b.id behandling_id, b.fagsak_id, fe.id, bb.person_ident fodselsnummer_barn, 
          bb.fodsel_termindato termindato_barn
        FROM gjeldende_iverksatte_behandlinger b
         JOIN fagsak_ekstern fe ON fe.fagsak_id = b.fagsak_id
         JOIN behandling_barn bb ON bb.behandling_id = b.id
        WHERE bb.person_ident IS NULL AND bb.fodsel_termindato < date(NOW() - INTERVAL '4 week') 
        """)
    fun finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn(): List<TerminbarnTilUtplukkForOppgave>
}
