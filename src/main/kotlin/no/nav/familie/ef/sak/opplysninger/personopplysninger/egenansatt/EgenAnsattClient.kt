package no.nav.familie.ef.sak.opplysninger.personopplysninger.egenansatt

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class EgenAnsattClient(
    @Value("\${SKJERMEDE_PERSONER_URL}") private val uri: URI,
    @Qualifier("skjermedePersonerRestClient") private val restClient: RestClient,
) {
    private val egenAnsattUri: URI =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("skjermet")
            .build()
            .toUri()

    fun egenAnsatt(ident: String): Boolean =
        restClient
            .post()
            .uri(egenAnsattUri)
            .body(EgenAnsattRequest(ident))
            .retrieve()
            .body<Boolean>()!!
}

data class EgenAnsattRequest(
    val personident: String,
)
