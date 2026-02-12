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
import no.nav.familie.restklient.client.AbstractPingableRestClient
import no.nav.familie.restklient.client.MultipartBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.UUID

@Component
class IverksettClient(
    @Value("\${FAMILIE_EF_IVERKSETT_URL}")
    private val familieEfIverksettUri: String,
    @Qualifier("azure")
    private val restOperations: RestOperations,
) : AbstractPingableRestClient(restOperations, "familie.ef.iverksett") {
    override val pingUri: URI = URI.create("$familieEfIverksettUri/api/status")

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun simuler(simuleringRequest: SimuleringDto): BeriketSimuleringsresultat {
        val url = URI.create("$familieEfIverksettUri/api/simulering/v2")

        return postForEntity<Ressurs<BeriketSimuleringsresultat>>(
            url,
            simuleringRequest,
            HttpHeaders().medContentTypeJsonUTF8(),
        ).data!!
    }

    fun sendBehandlingsstatistikk(request: BehandlingsstatistikkDto) {
        postForEntity<Any>(URI.create("$familieEfIverksettUri/api/statistikk/behandlingsstatistikk"), request)
    }

    fun publiserVedtakshendelse(behandlingId: UUID) {
        postForEntity<Any>(URI.create("$familieEfIverksettUri/api/iverksett/vedtakshendelse/$behandlingId"), "")
    }

    fun iverksett(
        iverksettDto: IverksettDto,
        fil: Fil,
    ) {
        val url = URI.create("$familieEfIverksettUri/api/iverksett")
        val request =
            MultipartBuilder()
                .withJson("data", iverksettDto)
                .withByteArray("fil", "vedtak", fil.bytes)
                .build()
        val headers = HttpHeaders().apply { this.add("Content-Type", "multipart/form-data") }
        postForEntity<Any>(url, request, headers)
    }

    fun iverksettUtenBrev(iverksettDto: IverksettDto) {
        val url = URI.create("$familieEfIverksettUri/api/iverksett/uten-brev")
        postForEntity<Any>(url, iverksettDto)
    }

    fun hentStatus(behandlingId: UUID): IverksettStatus {
        val url = URI.create("$familieEfIverksettUri/api/iverksett/status/$behandlingId")
        return getForEntity(url, HttpHeaders().medContentTypeJsonUTF8())
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
        postForEntity<Any>(url, request)
    }

    fun sendFrittståendeBrev(frittståendeBrevDto: FrittståendeBrevDto) {
        postForEntity<Any>(URI.create("$familieEfIverksettUri/api/brev/frittstaende"), frittståendeBrevDto)
    }

    fun håndterUtsendingAvAktivitetspliktBrev(periodiskAktivitetspliktBrevDto: PeriodiskAktivitetspliktBrevDto) {
        postForEntity<Any>(URI.create("$familieEfIverksettUri/api/brev/frittstaende/innhenting-aktivitetsplikt"), periodiskAktivitetspliktBrevDto)
    }

    fun timeoutTest(sekunder: Long): String {
        val testUri = URI.create("$familieEfIverksettUri/api/konsistensavstemming/timeout-test?sekunder=$sekunder")
        val headers =
            HttpHeaders().apply {
                accept = listOf(MediaType.TEXT_PLAIN)
            }
        return getForEntity<String>(testUri, headers)
    }
}
