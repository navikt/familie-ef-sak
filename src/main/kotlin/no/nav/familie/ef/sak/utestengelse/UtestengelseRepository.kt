package no.nav.familie.ef.sak.utestengelse

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UtestengelseRepository : RepositoryInterface<Utestengelse, UUID>, InsertUpdateRepository<Utestengelse> {

    fun findAllByFagsakPersonId(fagsakPersonId: UUID): List<Utestengelse>

}