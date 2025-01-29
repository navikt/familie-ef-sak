package no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OppgaverForFerdigstillingRepository :
    InsertUpdateRepository<OppgaverForFerdigstilling>,
    RepositoryInterface<OppgaverForFerdigstilling, UUID>
