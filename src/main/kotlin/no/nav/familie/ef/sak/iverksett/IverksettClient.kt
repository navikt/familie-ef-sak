package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.repository.domain.Fil
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.util.medContentTypeJsonUTF8
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.ef.infotrygd.OpprettStartBehandlingHendelseDto
import no.nav.familie.http.client.MultipartBuilder
import no.nav.familie.kontrakter.ef.iverksett.IverksettDto
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
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

    fun simuler(simuleringRequest: SimuleringDto): DetaljertSimuleringResultat {
        val url = URI.create("$familieEfIverksettUri/api/simulering")

        return postForEntity<Ressurs<DetaljertSimuleringResultat>>(url,
                                                                   simuleringRequest,
                                                                   HttpHeaders().medContentTypeJsonUTF8()).data!!
    }

    fun startBehandling(request: OpprettStartBehandlingHendelseDto) {
        postForEntity<Any>(URI.create("$familieEfIverksettUri/api/start-behandling"), request)
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

    fun iverksettTekniskOpphør(tilkjentYtelse: TilkjentYtelseMedMetaData) {
        val url = URI.create("$familieEfIverksettUri/api/tekniskopphor")
        val headers = HttpHeaders().apply { this.add("Content-Type", "multipart/form-data") }
        postForEntity<Any>(url, tilkjentYtelse, headers)
    }

    fun hentStatus(behandlingId: UUID): IverksettStatus {
        val url = URI.create("$familieEfIverksettUri/api/iverksett/status/$behandlingId")
        return getForEntity(url, HttpHeaders().medContentTypeJsonUTF8())
    }
}

