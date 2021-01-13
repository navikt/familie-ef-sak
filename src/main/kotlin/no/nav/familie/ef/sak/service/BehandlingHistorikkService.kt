package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.repository.BehandlingHistorikkRepository
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import org.springframework.stereotype.Service
import java.util.*

@Service
class BehandlingHistorikkService(val behandlingHistorikkRepository: BehandlingHistorikkRepository) {

    fun finnBehandlingHistorikk(behandlingId: UUID) : List<Behandlingshistorikk> {
        return behandlingHistorikkRepository.findByBehandlingId(behandlingId)
    }

    fun opprettHistorikkInnslag(behandlingshistorikk: Behandlingshistorikk) {
        behandlingHistorikkRepository.insert(behandlingshistorikk)
    }
}