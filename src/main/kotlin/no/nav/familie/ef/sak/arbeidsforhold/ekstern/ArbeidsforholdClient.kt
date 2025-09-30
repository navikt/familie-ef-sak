package no.nav.familie.ef.sak.arbeidsforhold.ekstern

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidsforhold
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Component
class ArbeidsforholdClient(
    @Value("\${AAREG_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "arbeidsforhold") {
    private fun lagArbeidsforholdUri() =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api/v2/arbeidstaker/arbeidsforhold")
            .build()
            .toUri()

    fun hentArbeidsforhold(
        personIdent: String,
    ): List<Arbeidsforhold> {
        val responseHeaders = HttpHeaders()
        responseHeaders["Nav-Personident"] = personIdent

        return getForEntity(
            uri = lagArbeidsforholdUri(),
            httpHeaders = responseHeaders,
        )
    }
}
