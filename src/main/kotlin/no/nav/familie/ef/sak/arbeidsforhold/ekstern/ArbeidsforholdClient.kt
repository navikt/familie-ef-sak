package no.nav.familie.ef.sak.arbeidsforhold.ekstern

import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidsforhold
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class ArbeidsforholdClient(
    @Value("\${AAREG_URL}") private val uri: URI,
    @Qualifier("aaregRestClient") private val restClient: RestClient,
) {
    private fun lagArbeidsforholdUri() =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api/v2/arbeidstaker/arbeidsforhold")
            .build()
            .toUri()

    fun hentArbeidsforhold(personIdent: String): List<Arbeidsforhold> =
        restClient
            .get()
            .uri(lagArbeidsforholdUri())
            .header("Nav-Personident", personIdent)
            .retrieve()
            .body<List<Arbeidsforhold>>()!!
}
