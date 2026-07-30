package no.nav.familie.ef.sak.amelding.ekstern

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Kaller arbeid-og-inntekt direkte istedenfor via familie-ef-proxy.
 * Krever ingen Azure AD-token, kun nettverkstilgang (webproxy/access policy) og Nav-Personident-header.
 */
@Component
class ArbeidOgInntektClient(
    @Value("\${ARBEID_OG_INNTEKT_URL}") private val uri: URI,
    @Qualifier("utenAuthRestClient") private val restClient: RestClient,
) {
    private val genererUrlUri =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api", "v2", "redirect", "sok", "a-inntekt")
            .build()
            .toUri()

    private val genererUrlUriArbeidsforhold =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api", "v2", "redirect", "sok", "arbeidstaker")
            .build()
            .toUri()

    fun genererAInntektArbeidsforholdUrl(personIdent: String): String =
        restClient
            .get()
            .uri(genererUrlUriArbeidsforhold)
            .header("Nav-Personident", personIdent)
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .body<String>()!!

    fun genererAInntektUrl(personIdent: String): String =
        restClient
            .get()
            .uri(genererUrlUri)
            .header("Nav-Personident", personIdent)
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .body<String>()!!
}
