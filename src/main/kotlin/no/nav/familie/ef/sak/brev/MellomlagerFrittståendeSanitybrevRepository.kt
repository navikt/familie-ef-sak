package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.domain.MellomlagretFrittståendeSanitybrev
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MellomlagerFrittståendeSanitybrevRepository :
    RepositoryInterface<MellomlagretFrittståendeSanitybrev, UUID>,
    InsertUpdateRepository<MellomlagretFrittståendeSanitybrev> {
    fun findByFagsakIdAndSaksbehandlerIdent(
        fagsakId: UUID,
        saksehandlerIdent: String,
    ): MellomlagretFrittståendeSanitybrev?
}
