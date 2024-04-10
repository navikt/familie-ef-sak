package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TerminbarnRepository :
    RepositoryInterface<TerminbarnOppgave, UUID>,
    InsertUpdateRepository<TerminbarnOppgave> {

    // language=PostgreSQL
    @Query(
        """
        SELECT b.id behandling_id, b.fagsak_id, fe.id ekstern_fagsak_id, bb.fodsel_termindato termindato_barn
        FROM gjeldende_iverksatte_behandlinger b
         JOIN fagsak_ekstern fe ON fe.fagsak_id = b.fagsak_id
         JOIN behandling_barn bb ON bb.behandling_id = b.id
        WHERE b.stonadstype=:stønadType AND bb.person_ident IS NULL AND bb.fodsel_termindato < date(NOW() - INTERVAL '4 week') 
        AND NOT EXISTS(SELECT 1 FROM terminbarn_oppgave WHERE fagsak_id = b.fagsak_id AND termindato = bb.fodsel_termindato)
        """,
    )
    fun finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn(stønadType: StønadType): List<TerminbarnTilUtplukkForOppgave>
}
