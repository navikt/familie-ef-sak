package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.BehandlingsHistorikk
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingHistorikkRepository : RepositoryInterface<BehandlingsHistorikk, UUID>, InsertUpdateRepository<BehandlingsHistorikk>{
    fun findByBehandlingId(behandlingId: UUID): List<BehandlingsHistorikk>
}