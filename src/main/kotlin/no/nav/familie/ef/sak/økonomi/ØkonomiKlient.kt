package no.nav.familie.ef.sak.økonomi

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.net.URI
import java.time.LocalDateTime

const val FAGSYSTEM = "EF"

@Service
class ØkonomiKlient(
        @Value("\${FAMILIE_OPPDRAG_API_URL}")
        private val familieOppdragUri: String,
        val azure: RestOperations) {

    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): ResponseEntity<Ressurs<String>> {
        return azure.exchange(
                URI.create("$familieOppdragUri/oppdrag"),
                HttpMethod.POST,
                HttpEntity(utbetalingsoppdrag))
    }

    fun hentStatus(oppdragId: OppdragId): ResponseEntity<Ressurs<OppdragStatus>> {
        return azure.exchange(
                URI.create("$familieOppdragUri/status"),
                HttpMethod.POST,
                HttpEntity(oppdragId))
    }

    fun grensesnittavstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime): ResponseEntity<Ressurs<String>> {
        return azure.exchange(
                URI.create("$familieOppdragUri/grensesnittavstemming/$FAGSYSTEM/?fom=$fraDato&tom=$tilDato"),
                HttpMethod.POST)
    }
}
