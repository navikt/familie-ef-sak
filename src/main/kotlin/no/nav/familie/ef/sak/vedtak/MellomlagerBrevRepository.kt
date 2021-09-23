package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.vedtak.domain.MellomlagretBrev
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MellomlagerBrevRepository : RepositoryInterface<MellomlagretBrev, UUID>, InsertUpdateRepository<MellomlagretBrev> {


}
