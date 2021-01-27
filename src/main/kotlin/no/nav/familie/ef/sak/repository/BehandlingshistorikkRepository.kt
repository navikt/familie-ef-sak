package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.service.steg.StegType
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingshistorikkRepository : RepositoryInterface<Behandlingshistorikk, UUID>,
                                           InsertUpdateRepository<Behandlingshistorikk> {

    fun findByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): List<Behandlingshistorikk>

    fun findTopByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): Behandlingshistorikk

    fun findTopByBehandlingIdAndStegOrderByEndretTidDesc(behandlingId: UUID, steg: StegType): Behandlingshistorikk?

}