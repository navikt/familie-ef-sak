package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.net.URI
import java.time.LocalDateTime

private const val OAUTH2_CLIENT_CONFIG_KEY = "familie-oppdrag-clientcredentials"
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

    fun hentStatus(statusFraOppdragDTO: StatusFraOppdragDTO): ResponseEntity<Ressurs<OppdragProtokollStatus>> {
        return azure.exchange(
                URI.create("$familieOppdragUri/status"),
                HttpMethod.POST,
                HttpEntity(statusFraOppdragDTO))
    }

    fun grensesnittavstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime): ResponseEntity<Ressurs<String>> {
        return azure.exchange(
                URI.create("$familieOppdragUri/grensesnittavstemming/$FAGSYSTEM/?fom=$fraDato&tom=$tilDato"),
                HttpMethod.POST)
    }
}
