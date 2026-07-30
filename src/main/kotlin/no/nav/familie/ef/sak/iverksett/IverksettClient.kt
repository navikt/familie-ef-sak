package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.felles.util.medContentTypeJsonUTF8
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto
import no.nav.familie.kontrakter.ef.felles.PeriodiskAktivitetspliktBrevDto
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsstatistikkDto
import no.nav.familie.kontrakter.ef.iverksett.IverksettDto
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingDto
import no.nav.familie.kontrakter.ef.iverksett.SimuleringDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.UUID

@Component
class IverksettClient(
    @Value("\${FAMILIE_EF_IVERKSETT_URL}")
    private val familieEfIverksettUri: String,
    @Qualifier("iverksettRestClient")
    private val restClient: RestClient,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun simuler(simuleringRequest: SimuleringDto): BeriketSimuleringsresultat {
        val url = URI.create("$familieEfIverksettUri/api/simulering/v2")

        return restClient
            .post()
            .uri(url)
            .headers { it.addAll(HttpHeaders().medContentTypeJsonUTF8()) }
            .body(simuleringRequest)
            .retrieve()
            .body<Ressurs<BeriketSimuleringsresultat>>()!!
            .data!!
    }

    fun sendBehandlingsstatistikk(request: BehandlingsstatistikkDto) {
        restClient
            .post()
            .uri(URI.create("$familieEfIverksettUri/api/statistikk/behandlingsstatistikk"))
            .body(request)
            .retrieve()
            .body<Any>()
    }

    fun publiserVedtakshendelse(behandlingId: UUID) {
        restClient
            .post()
            .uri(URI.create("$familieEfIverksettUri/api/iverksett/vedtakshendelse/$behandlingId"))
            .body("")
            .retrieve()
            .body<Any>()
    }

    fun iverksett(
        iverksettDto: IverksettDto,
        fil: Fil,
    ) {
        val url = URI.create("$familieEfIverksettUri/api/iverksett")
        val request = IverksettMedBrevRequest(iverksettDto, fil.bytes)
        secureLogger.info("Sender iverksettDto: $iverksettDto")
        restClient
            .post()
            .uri(url)
            .body(request)
            .retrieve()
            .body<Any>()
    }

    fun iverksettUtenBrev(iverksettDto: IverksettDto) {
        val url = URI.create("$familieEfIverksettUri/api/iverksett/uten-brev")
        restClient
            .post()
            .uri(url)
            .body(iverksettDto)
            .retrieve()
            .body<Any>()
    }

    fun hentStatus(behandlingId: UUID): IverksettStatus {
        val url = URI.create("$familieEfIverksettUri/api/iverksett/status/$behandlingId")
        return restClient
            .get()
            .uri(url)
            .headers { it.addAll(HttpHeaders().medContentTypeJsonUTF8()) }
            .retrieve()
            .body<IverksettStatus>()!!
    }

    fun sendStartmeldingKonsistensavstemming(
        request: KonsistensavstemmingDto,
        transaksjonId: UUID,
    ) = konsistensavstemming(request, sendStartmelding = true, sendAvsluttmelding = false, transaksjonId)

    fun sendSluttmeldingKonsistensavstemming(
        request: KonsistensavstemmingDto,
        transaksjonId: UUID,
    ) = konsistensavstemming(request, sendStartmelding = false, sendAvsluttmelding = true, transaksjonId)

    fun sendKonsistensavstemming(
        request: KonsistensavstemmingDto,
        transaksjonId: UUID,
    ) = konsistensavstemming(request, sendStartmelding = false, sendAvsluttmelding = false, transaksjonId)

    private fun konsistensavstemming(
        request: KonsistensavstemmingDto,
        sendStartmelding: Boolean = true,
        sendAvsluttmelding: Boolean = true,
        transaksjonId: UUID = UUID.randomUUID(),
    ) {
        val url =
            UriComponentsBuilder
                .fromUriString("$familieEfIverksettUri/api/konsistensavstemming")
                .queryParam("sendStartmelding", sendStartmelding)
                .queryParam("sendAvsluttmelding", sendAvsluttmelding)
                .queryParam("transaksjonId", transaksjonId.toString())
                .build()
                .toUri()
        restClient
            .post()
            .uri(url)
            .body(request)
            .retrieve()
            .body<Any>()
    }

    fun sendFrittståendeBrev(frittståendeBrevDto: FrittståendeBrevDto) {
        restClient
            .post()
            .uri(URI.create("$familieEfIverksettUri/api/brev/frittstaende"))
            .body(frittståendeBrevDto)
            .retrieve()
            .body<Any>()
    }

    fun håndterUtsendingAvAktivitetspliktBrev(periodiskAktivitetspliktBrevDto: PeriodiskAktivitetspliktBrevDto) {
        restClient
            .post()
            .uri(URI.create("$familieEfIverksettUri/api/brev/frittstaende/innhenting-aktivitetsplikt"))
            .body(periodiskAktivitetspliktBrevDto)
            .retrieve()
            .body<Any>()
    }

    fun timeoutTest(sekunder: Long): String {
        val testUri = URI.create("$familieEfIverksettUri/api/konsistensavstemming/timeout-test?sekunder=$sekunder")
        return restClient
            .get()
            .uri(testUri)
            .headers { it.accept = listOf(MediaType.TEXT_PLAIN) }
            .retrieve()
            .body<String>()!!
    }
}
