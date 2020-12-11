package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.BehandlingHistorikk
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingHistorikkRepository : RepositoryInterface<BehandlingHistorikk, UUID>,
                                          InsertUpdateRepository<BehandlingHistorikk> {

    fun findByBehandlingId(behandlingId: UUID): List<BehandlingHistorikk>
}