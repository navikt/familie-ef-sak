package no.nav.familie.ef.sak.vedtaksbrev

import no.nav.familie.ef.sak.iverksett.SimuleringDto
import no.nav.familie.ef.sak.util.medContentTypeJsonUTF8
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI


@Component
class IverksettClient(@Value("\${FAMILIE_EF_IVERKSETT_URL}")
                      private val familieEfIverksettUri: String,
                      @Qualifier("azure")
                      private val restOperations: RestOperations) : AbstractPingableRestClient(restOperations, "familie.brev") {

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
}

