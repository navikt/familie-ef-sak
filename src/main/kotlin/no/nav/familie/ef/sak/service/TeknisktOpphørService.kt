package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TeknisktOpphørService(val behandlingService: BehandlingService, val behandlingRepository: BehandlingRepository) {

    fun håndterTeknisktOpphør(personIdent: String) {
        val behandling = behandlingRepository.finnSisteBehandling(Stønadstype.OVERGANGSSTØNAD, setOf(personIdent))
        require(behandling != null) { throw Feil("Finner ikke behandling med stønadstype overgangsstønad for personen") }
        val tekniskt = behandling.copy(id = UUID.randomUUID(),
                                       type = BehandlingType.TEKNISK_OPPHØR,
                                       status = BehandlingStatus.UTREDES)
    }
}