package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.FAGSYSTEM
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AvstemmingService(val oppdragClient: OppdragClient) {

    fun grensesnittavstemOppdrag(fraTidspunkt: LocalDateTime, tilTidspunkt: LocalDateTime) {
        val grensesnittavstemmingRequest = GrensesnittavstemmingRequest(FAGSYSTEM, fraTidspunkt, tilTidspunkt)

        oppdragClient.grensesnittavstemming(grensesnittavstemmingRequest)

    }

}
