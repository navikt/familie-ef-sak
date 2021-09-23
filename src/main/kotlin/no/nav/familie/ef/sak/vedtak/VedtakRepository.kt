package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VedtakRepository : RepositoryInterface<Vedtak, UUID>, InsertUpdateRepository<Vedtak> {

}
