package no.nav.familie.ef.sak.andreytelser

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class ArbeidsavklaringspengerClient(
    @Value("\${ARBEIDSAVKLARINGSPENGER_URL}") private val uri: URI,
    @Qualifier("aapRestClient") private val restClient: RestClient,
) {
    val uriPerioder =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("maksimumUtenUtbetaling")
            .build()
            .toUri()

    fun hentPerioder(request: ArbeidsavklaringspengerRequest): ArbeidsavklaringspengerResponse =
        restClient
            .post()
            .uri(uriPerioder)
            .body(request)
            .retrieve()
            .body<ArbeidsavklaringspengerResponse>()!!
}
