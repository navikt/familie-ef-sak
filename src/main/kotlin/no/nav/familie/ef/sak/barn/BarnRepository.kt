package no.nav.familie.ef.sak.barn

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BarnRepository : RepositoryInterface<BehandlingBarn, UUID>, InsertUpdateRepository<BehandlingBarn> {

    fun findByBehandlingId(behandlingId: UUID): List<BehandlingBarn>

}
