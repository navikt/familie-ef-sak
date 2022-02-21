package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.felles.util.medContentTypeJsonUTF8
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.OppgaverForBarnDto
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.http.client.MultipartBuilder
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto
import no.nav.familie.kontrakter.ef.infotrygd.OpprettStartBehandlingHendelseDto
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsstatistikkDto
import no.nav.familie.kontrakter.ef.iverksett.IverksettDto
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingDto
import no.nav.familie.kontrakter.ef.iverksett.SimuleringDto
import no.nav.familie.kontrakter.ef.iverksett.TekniskOpphørDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI
import java.util.UUID


@Component
class IverksettClient(@Value("\${FAMILIE_EF_IVERKSETT_URL}")
                      private val familieEfIverksettUri: String,
                      @Qualifier("azure")
                      private val restOperations: RestOperations)
    : AbstractPingableRestClient(restOperations, "familie.ef.iverksett") {

    override val pingUri: URI = URI.create("$familieEfIverksettUri/api/status")

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun simuler(simuleringRequest: SimuleringDto): BeriketSimuleringsresultat {
        val url = URI.create("$familieEfIverksettUri/api/simulering/v2")

        return postForEntity<Ressurs<BeriketSimuleringsresultat>>(url,
                                                                  simuleringRequest,
                                                                  HttpHeaders().medContentTypeJsonUTF8()).data!!
    }

    fun startBehandling(request: OpprettStartBehandlingHendelseDto) {
        postForEntity<Any>(URI.create("$familieEfIverksettUri/api/start-behandling"), request)
    }

    fun sendOppgaverForBarn(oppgaverForBarn: OppgaverForBarnDto) {
        postForEntity<Any>(URI.create("$familieEfIverksettUri/api/oppgave-for-barns-alder/opprett"), oppgaverForBarn)
    }

    fun sendBehandlingsstatistikk(request: BehandlingsstatistikkDto) {
        postForEntity<Any>(URI.create("$familieEfIverksettUri/api/statistikk/behandlingsstatistikk"), request)
    }

    fun publiserVedtakshendelse(behandlingId: UUID) {
        postForEntity<Any>(URI.create("$familieEfIverksettUri/api/iverksett/vedtakshendelse/${behandlingId}"), "")
    }

    fun iverksett(iverksettDto: IverksettDto, fil: Fil) {
        val url = URI.create("$familieEfIverksettUri/api/iverksett")
        val request = MultipartBuilder()
                .withJson("data", iverksettDto)
                .withByteArray("fil", "vedtak", fil.bytes)
                .build()
        val headers = HttpHeaders().apply { this.add("Content-Type", "multipart/form-data") }
        postForEntity<Any>(url, request, headers)
    }

    fun iverksettMigrering(iverksettDto: IverksettDto) {
        val url = URI.create("$familieEfIverksettUri/api/iverksett/migrering")
        postForEntity<Any>(url, iverksettDto)
    }

    fun iverksettTekniskOpphør(tekniskOpphørDto: TekniskOpphørDto) {
        val url = URI.create("$familieEfIverksettUri/api/tekniskopphor")
        postForEntity<Any>(url, tekniskOpphørDto, HttpHeaders().medContentTypeJsonUTF8())
    }

    fun hentStatus(behandlingId: UUID): IverksettStatus {
        val url = URI.create("$familieEfIverksettUri/api/iverksett/status/$behandlingId")
        return getForEntity(url, HttpHeaders().medContentTypeJsonUTF8())
    }

    fun konsistensavstemming(request: KonsistensavstemmingDto) {
        postForEntity<Any>(URI.create("$familieEfIverksettUri/api/konsistensavstemming"), request)
    }

    fun sendFrittståendeBrev(frittståendeBrevDto: FrittståendeBrevDto) {
        postForEntity<Any>(URI.create("$familieEfIverksettUri/api/brev/frittstaende"), frittståendeBrevDto)
    }
}
