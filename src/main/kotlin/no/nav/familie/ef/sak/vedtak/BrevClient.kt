package no.nav.familie.ef.sak.vedtak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.felles.util.medContentTypeJsonUTF8
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksbrev
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI


@Component
class BrevClient(@Value("\${FAMILIE_BREV_API_URL}")
                 private val familieBrevUri: String,
                 @Qualifier("utenAuth")
                 private val restOperations: RestOperations) : AbstractPingableRestClient(restOperations, "familie.brev") {

    override val pingUri: URI = URI.create("$familieBrevUri/api/status")

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun genererBrev(vedtaksbrev: Vedtaksbrev): ByteArray {
        val url = URI.create("$familieBrevUri/api/ef-brev/avansert-dokument/bokmaal/${vedtaksbrev.brevmal}/pdf")
        return postForEntity(url,
                             BrevRequestMedSignaturer(objectMapper.readTree(vedtaksbrev.saksbehandlerBrevrequest),
                                                      vedtaksbrev.saksbehandlersignatur,
                                                      vedtaksbrev.besluttersignatur),
                             HttpHeaders().medContentTypeJsonUTF8())
    }

    fun lagManueltBrev(brevinnhold: JsonNode): ByteArray {
        val url = URI.create("$familieBrevUri/api/manuelt-brev")

        return postForEntity(url,
                             brevinnhold,
                             HttpHeaders().medContentTypeJsonUTF8())
    }
    
    companion object {

        val ef = "ef-brev"
        val test = "testdata"
    }
}

data class BrevRequestMedSignaturer(val brevFraSaksbehandler: JsonNode,
                                    val saksbehandlersignatur: String,
                                    val besluttersignatur: String?)

data class ManueltBrev(
        val brevinnhold: JsonNode)

