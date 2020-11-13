package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.FAGSYSTEM_OVERGANGSSTØNAD
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AvstemmingService(val oppdragClient: OppdragClient) {

    fun grensesnittavstemOppdrag(fraTidspunkt: LocalDateTime, tilTidspunkt: LocalDateTime, stønadstype: Stønadstype) {
        val grensesnittavstemmingRequest = when (stønadstype) {
            Stønadstype.OVERGANGSSTØNAD -> GrensesnittavstemmingRequest(FAGSYSTEM_OVERGANGSSTØNAD, fraTidspunkt, tilTidspunkt)
            else -> throw Error("Grensesnittavstemming for ${stønadstype} er ikke implementert")
        }
        oppdragClient.grensesnittavstemming(grensesnittavstemmingRequest)
    }

}
