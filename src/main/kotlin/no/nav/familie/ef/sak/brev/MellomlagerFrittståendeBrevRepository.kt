package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.domain.MellomlagretFrittståendeBrev
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MellomlagerFrittståendeBrevRepository : RepositoryInterface<MellomlagretFrittståendeBrev, UUID>,
                                                  InsertUpdateRepository<MellomlagretFrittståendeBrev> {

    fun findByFagsakIdAndSaksbehandlerIdent(fagsakId: UUID, saksehandlerIdent: String): MellomlagretFrittståendeBrev?

}
