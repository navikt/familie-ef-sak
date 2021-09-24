package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VedtaksbrevRepository : RepositoryInterface<Vedtaksbrev, UUID>, InsertUpdateRepository<Vedtaksbrev>
