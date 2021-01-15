package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.repository.BehandlingshistorikkRepository
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import org.springframework.stereotype.Service
import java.util.*

@Service
class BehandlingshistorikkService(val behandlingshistorikkRepository: BehandlingshistorikkRepository) {

    fun finnBehandlingshistorikk(behandlingId: UUID) : List<Behandlingshistorikk> {
        return behandlingshistorikkRepository.findByBehandlingId(behandlingId)
    }

    fun opprettHistorikkInnslag(behandlingshistorikk: Behandlingshistorikk) {
        behandlingshistorikkRepository.insert(behandlingshistorikk)
    }
}