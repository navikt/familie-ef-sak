package no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling

import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.OppgaverForFerdigstilling
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OppgaverForFerdigstillingRepository :
    InsertUpdateRepository<OppgaverForFerdigstilling>,
    RepositoryInterface<OppgaverForFerdigstilling, UUID> {
    @Modifying
    @Query("DELETE from oppgaver_for_ferdigstilling where behandling_id = :behandlingId")
    fun deleteByBehandlingId(behandlingId: UUID)
}
