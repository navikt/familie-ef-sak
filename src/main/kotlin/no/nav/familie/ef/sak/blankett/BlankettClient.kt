package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.util.medContentTypeJsonUTF8
import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class BlankettClient(@Value("\${FAMILIE_BLANKETT_API_URL}")
                     private val familieBlankettUri: String,
                     @Qualifier("utenAuth")
                     private val restOperations: RestOperations) : AbstractPingableRestClient(restOperations,
                                                                                              "familie.blankett") {

    override val pingUri: URI
        get() = URI.create(familieBlankettUri)

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun genererBlankett(blankettPdfRequest: BlankettPdfRequest): ByteArray {
        val url = URI.create("$familieBlankettUri/api/pdf")
        return postForEntity(url, blankettPdfRequest, HttpHeaders().medContentTypeJsonUTF8())
    }
}