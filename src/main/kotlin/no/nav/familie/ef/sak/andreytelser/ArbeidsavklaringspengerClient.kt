package no.nav.familie.ef.sak.andreytelser

import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class ArbeidsavklaringspengerClient(
    @Value("\${ARBEIDSAVKLARINGSPENGER_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "arbeidsavklaringspenger") {
    val uriPerioder =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("maksimumUtenUtbetaling")
            .build()
            .toUri()

    fun hentPerioder(
        request: ArbeidsavklaringspengerRequest
    ) = postForEntity<ArbeidsavklaringspengerResponse>(uriPerioder, request)

}