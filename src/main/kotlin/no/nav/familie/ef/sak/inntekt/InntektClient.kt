package no.nav.familie.ef.sak.inntekt

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class InntektClient(
        @Value("\${FAMILIE_EF_PROXY_URL}") uri: URI,
        @Qualifier("azure") restOperations: RestOperations
) : AbstractRestClient(restOperations, "inntekt") {

    private val inntektUri = UriComponentsBuilder.fromUri(uri).pathSegment("api/inntekt").build().toUri()

    fun hentInntekt(personIdent: String): Map<String, Any> {
        return postForEntity(inntektUri, PersonIdent(personIdent))
    }
}
