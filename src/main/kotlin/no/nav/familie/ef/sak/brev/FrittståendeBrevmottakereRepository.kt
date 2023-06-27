package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.domain.BrevmottakereFrittståendeBrev
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FrittståendeBrevmottakereRepository :
    RepositoryInterface<BrevmottakereFrittståendeBrev, UUID>,
    InsertUpdateRepository<BrevmottakereFrittståendeBrev> {

    fun findByFagsakIdAndSaksbehandlerIdent(fagsakId: UUID, saksbehandlerIdent: String): BrevmottakereFrittståendeBrev?
}
