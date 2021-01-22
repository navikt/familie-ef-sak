package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.TotrinnskontrollDto
import no.nav.familie.ef.sak.repository.BehandlingshistorikkRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.repository.domain.StegUtfall
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

    fun opprettHistorikkInnslag(behandlingshistorikk: Behandlingshistorikk) {
        behandlingshistorikkRepository.insert(behandlingshistorikk)
    }

    fun opprettHistorikkInnslag(behandling: Behandling, utfall: StegUtfall?, metadata: Any?) {
        opprettHistorikkInnslag(Behandlingshistorikk(
                  behandlingId = behandling.id,
                  steg = behandling.steg,
                  utfall = utfall,
                  metadata = metadata?.let {
                      objectMapper.writeValueAsString(it)
                  }))
      }

}