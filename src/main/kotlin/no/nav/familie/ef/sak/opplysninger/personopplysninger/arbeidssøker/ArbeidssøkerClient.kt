package no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker

import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Service
class ArbeidssøkerClient(
    @Value("\${ARBEIDSSOKER_URL}")
    private val uriGcp: URI,
    @Qualifier("azure") restOperations: RestOperations,
) :
    AbstractRestClient(restOperations, "paw.arbeidssoker") {
    fun hentPerioder(
        personIdent: String,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate? = null,
    ): ArbeidssøkerResponse {
        val uriBuilder =
            UriComponentsBuilder.fromUri(uriGcp)
                .pathSegment("veilarbregistrering/api/arbeidssoker/perioder")
                .queryParam("fraOgMed", fraOgMed)
        tilOgMed?.let { uriBuilder.queryParam("tilOgMed", tilOgMed) }

        return postForEntity(uriBuilder.build().toUri(), FnrArbeidssøker(personIdent))
    }
}

data class FnrArbeidssøker(
    val fnr: String,
)
