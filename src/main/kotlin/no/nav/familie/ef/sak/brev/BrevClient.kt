package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.api.dto.BrevRequest
import no.nav.familie.ef.sak.util.medContentTypeJsonUTF8
import no.nav.familie.http.client.AbstractPingableRestClient
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

    override val pingUri: URI
        get() = URI.create(familieBrevUri)

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun genererBrev(målform: String, malnavn: String, request: BrevRequest): ByteArray {
        val url = URI.create("$familieBrevUri/api/ef-brev/avansert-dokument/$målform/$malnavn/pdf")

        return postForEntity(url, request.lagBody(), HttpHeaders().medContentTypeJsonUTF8())
    }
}

