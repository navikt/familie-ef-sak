package no.nav.familie.ef.sak.behandlingshistorikk

import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingshistorikkRepository : RepositoryInterface<Behandlingshistorikk, UUID>,
                                           InsertUpdateRepository<Behandlingshistorikk> {

    fun findByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): List<Behandlingshistorikk>

    fun findByBehandlingIdOrderByEndretTidAsc(behandlingId: UUID): List<Behandlingshistorikk>

    fun findTopByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): Behandlingshistorikk

    fun findTopByBehandlingIdAndStegOrderByEndretTidDesc(behandlingId: UUID, steg: StegType): Behandlingshistorikk?

}