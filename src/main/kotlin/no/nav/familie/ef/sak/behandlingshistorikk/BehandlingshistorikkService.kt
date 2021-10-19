package no.nav.familie.ef.sak.behandlingshistorikk

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.felles.domain.JsonWrapper
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BehandlingshistorikkService(private val behandlingshistorikkRepository: BehandlingshistorikkRepository) {

    fun finnBehandlingshistorikk(behandlingId: UUID): List<Behandlingshistorikk> {
        return behandlingshistorikkRepository.findByBehandlingIdOrderByEndretTidDesc(behandlingId)
    }

    fun finnSisteBehandlingshistorikk(behandlingId: UUID): Behandlingshistorikk {
        return behandlingshistorikkRepository.findTopByBehandlingIdOrderByEndretTidDesc(behandlingId)
    }

    fun finnSisteBehandlingshistorikk(behandlingId: UUID, type: StegType): Behandlingshistorikk? =
            behandlingshistorikkRepository.findTopByBehandlingIdAndStegOrderByEndretTidDesc(behandlingId, type)

    fun opprettHistorikkInnslag(behandlingshistorikk: Behandlingshistorikk) {
        behandlingshistorikkRepository.insert(behandlingshistorikk)
    }

    fun opprettHistorikkInnslag(behandling: Behandling) {
        behandlingshistorikkRepository.insert(Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = behandling.steg,
        ))
    }

    /**
     * @param metadata json object that will be serialized
     */
    fun opprettHistorikkInnslag(behandling: Behandling, utfall: StegUtfall?, metadata: Any?) {
        opprettHistorikkInnslag(Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = behandling.steg,
                utfall = utfall,
                metadata = metadata?.let {
                    JsonWrapper(objectMapper.writeValueAsString(it))
                }))
    }

}