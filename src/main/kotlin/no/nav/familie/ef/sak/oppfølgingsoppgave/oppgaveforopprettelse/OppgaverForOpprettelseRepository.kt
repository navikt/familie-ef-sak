package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.OppgaverForOpprettelse
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OppgaverForOpprettelseRepository :
    RepositoryInterface<OppgaverForOpprettelse, UUID>,
    InsertUpdateRepository<OppgaverForOpprettelse> {
    @Modifying
    @Query("DELETE from oppgaver_for_opprettelse where behandling_id = :behandlingId")
    fun deleteByBehandlingId(behandlingId: UUID)
}
