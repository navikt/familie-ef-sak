package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksbrev
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VedtaksbrevRepository : RepositoryInterface<Vedtaksbrev, UUID>, InsertUpdateRepository<Vedtaksbrev> {


}
