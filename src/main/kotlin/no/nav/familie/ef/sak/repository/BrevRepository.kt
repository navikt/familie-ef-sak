package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Brev
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BrevRepository : RepositoryInterface<Brev, UUID>, InsertUpdateRepository<Brev> {

}
