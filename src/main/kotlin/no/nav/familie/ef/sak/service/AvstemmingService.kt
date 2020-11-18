package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.økonomi.tilKlassifisering
import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AvstemmingService(val oppdragClient: OppdragClient) {

    fun grensesnittavstemOppdrag(fraTidspunkt: LocalDateTime, tilTidspunkt: LocalDateTime, stønadstype: Stønadstype) {
        val grensesnittavstemmingRequest = GrensesnittavstemmingRequest(stønadstype.tilKlassifisering(), fraTidspunkt, tilTidspunkt)
        oppdragClient.grensesnittavstemming(grensesnittavstemmingRequest)
    }

}
