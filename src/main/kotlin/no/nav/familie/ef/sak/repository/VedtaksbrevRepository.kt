package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VedtaksbrevRepository : RepositoryInterface<Vedtaksbrev, UUID>, InsertUpdateRepository<Vedtaksbrev> {

    fun findByBehandlingId(behandlingId: UUID): Vedtaksbrev?

}
