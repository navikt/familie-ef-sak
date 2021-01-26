package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.repository.BehandlingshistorikkRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.repository.domain.JsonWrapper
import no.nav.familie.ef.sak.repository.domain.StegUtfall
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service
import java.util.*

@Service
class BehandlingshistorikkService(private val behandlingshistorikkRepository: BehandlingshistorikkRepository) {

    fun finnBehandlingshistorikk(behandlingId: UUID): List<Behandlingshistorikk> {
        return behandlingshistorikkRepository.findByBehandlingId(behandlingId)
    }

    fun finnSisteBehandlingshistorikk(behandlingId: UUID): Behandlingshistorikk {
        return behandlingshistorikkRepository.findTopByBehandlingIdOrderByEndretTidDesc(behandlingId)
    }

    fun finnSisteBehandlingshistorikk(behandlingId: UUID, type: StegType): Behandlingshistorikk? =
            behandlingshistorikkRepository.findTopByBehandlingIdAndStegOrderByEndretTidDesc(behandlingId, type)

    fun opprettHistorikkInnslag(behandlingshistorikk: Behandlingshistorikk) {
        behandlingshistorikkRepository.insert(behandlingshistorikk)
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
                      JsonWrapper(objectMapper.writeValueAsString (it))
                  }))
      }

}