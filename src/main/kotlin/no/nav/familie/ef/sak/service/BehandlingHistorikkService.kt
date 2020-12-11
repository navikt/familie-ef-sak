package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.repository.BehandlingHistorikkRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingHistorikk
import org.springframework.stereotype.Service
import java.util.*

@Service
class BehandlingHistorikkService(val behandlingHistorikkRepository: BehandlingHistorikkRepository) {

    fun finnBehandlingHistorikk(behandlingId: UUID) : List<BehandlingHistorikk> {
        return behandlingHistorikkRepository.findByBehandlingId(behandlingId)
    }

}