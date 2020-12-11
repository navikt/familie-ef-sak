package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.repository.BehandlingHistorikkRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingsHistorikk
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class BehandlingHistorikkService(val behandlingHistorikkRepository: BehandlingHistorikkRepository) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun finnBehandling(behandlingId: UUID) : List<BehandlingsHistorikk> {
        return behandlingHistorikkRepository.findByBehandlingId(behandlingId)
    }

}