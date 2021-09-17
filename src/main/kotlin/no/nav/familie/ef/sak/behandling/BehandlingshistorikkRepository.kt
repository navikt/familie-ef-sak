package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.behandling.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingshistorikkRepository : RepositoryInterface<Behandlingshistorikk, UUID>,
                                           InsertUpdateRepository<Behandlingshistorikk> {

    fun findByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): List<Behandlingshistorikk>

    fun findTopByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): Behandlingshistorikk

    fun findTopByBehandlingIdAndStegOrderByEndretTidDesc(behandlingId: UUID, steg: StegType): Behandlingshistorikk?

}