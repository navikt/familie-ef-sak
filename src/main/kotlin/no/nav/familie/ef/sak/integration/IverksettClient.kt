package no.nav.familie.ef.sak.integration

import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class IverksettClient(
    @Value("\${FAMILIE_OPPDRAG_IVERKSETT_URL}")
    private val iverksettEndpoint: String,
    @Qualifier("azure")
    restOperations: RestOperations
) : AbstractPingableRestClient(restOperations, "familie.iverksett") {

    fun hentTestrespons(): String {
        val uri = URI.create("$iverksettEndpoint/api/iverksett/test")
        return getForEntity(uri)
    }

    override val pingUri: URI
        get() = UriComponentsBuilder.fromUri(URI.create(iverksettEndpoint)).pathSegment("api/ping").build().toUri()

}