package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.MellomlagretBrev
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MellomlagerBrevRepository : RepositoryInterface<MellomlagretBrev, UUID>, InsertUpdateRepository<MellomlagretBrev> {


}
