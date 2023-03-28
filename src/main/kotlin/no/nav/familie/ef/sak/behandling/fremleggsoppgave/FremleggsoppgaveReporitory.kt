package no.nav.familie.ef.sak.behandling.fremleggsoppgave

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FremleggsoppgaveReporitory :
    RepositoryInterface<OpprettFremleggsoppgave, UUID>,
    InsertUpdateRepository<OpprettFremleggsoppgave>
