package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingshistorikkRepository : RepositoryInterface<Behandlingshistorikk, UUID>,
                                           InsertUpdateRepository<Behandlingshistorikk> {

    fun findByBehandlingId(behandlingId: UUID): List<Behandlingshistorikk>

    fun findTopByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): Behandlingshistorikk

}