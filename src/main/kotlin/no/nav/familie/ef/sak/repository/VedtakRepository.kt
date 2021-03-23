package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Vedtak
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VedtakRepository : RepositoryInterface<Vedtak, UUID>, InsertUpdateRepository<Vedtak> {

}
