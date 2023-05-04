package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OppgaverForOpprettelseRepository :
    RepositoryInterface<OppgaverForOpprettelse, UUID>,
    InsertUpdateRepository<OppgaverForOpprettelse>
