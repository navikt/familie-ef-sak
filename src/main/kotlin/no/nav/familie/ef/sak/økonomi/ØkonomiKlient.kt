package no.nav.familie.ef.sak.økonomi

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.net.URI
import java.time.LocalDateTime

const val FAGSYSTEM = "EF"

@Service
class ØkonomiKlient(@Value("\${FAMILIE_OPPDRAG_API_URL}")
                    private val familieOppdragUri: URI,
                    azure: RestOperations) : AbstractRestClient(azure, "familie.oppdrag") {

    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): Ressurs<String> {
        return postForEntity(URI.create("$familieOppdragUri/oppdrag"), utbetalingsoppdrag)
    }

    fun hentStatus(oppdragId: OppdragId): Ressurs<OppdragStatus> {
        return postForEntity(URI.create("$familieOppdragUri/status"), oppdragId)
    }

    fun grensesnittavstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime): ResponseEntity<Ressurs<String>> {
        return operations.exchange(URI.create("$familieOppdragUri/grensesnittavstemming/$FAGSYSTEM/?fom=$fraDato&tom=$tilDato"),
                                   HttpMethod.POST)
    }
}
