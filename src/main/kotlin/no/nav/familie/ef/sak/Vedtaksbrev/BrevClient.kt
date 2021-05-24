package no.nav.familie.ef.sak.vedtaksbrev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.api.dto.BrevRequest
import no.nav.familie.ef.sak.util.medContentTypeJsonUTF8
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

    fun genererBrev(målform: String, malnavn: String, request: BrevRequest): ByteArray {
        val url = URI.create("$familieBrevUri/api/testdata/avansert-dokument/$målform/$malnavn/pdf")

        val streng = objectMapper.readTree(request.lagBody())

        return postForEntity(url, streng, HttpHeaders().medContentTypeJsonUTF8())
    }

    fun genererBrev(målform: String? = "bokmaal" , malnavn: String? = "innvilgetVedtakMVP", request: JsonNode): ByteArray {
        val url = URI.create("$familieBrevUri/api/testdata/avansert-dokument/$målform/$malnavn/pdf")

        return postForEntity(url, request, HttpHeaders().medContentTypeJsonUTF8 ())
    }
}

