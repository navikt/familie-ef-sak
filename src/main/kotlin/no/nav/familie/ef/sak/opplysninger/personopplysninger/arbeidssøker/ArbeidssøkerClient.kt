package no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class ArbeidssøkerClient(
    @Value("\${ARBEIDSSOKER_URL}")
    private val uriGcp: URI,
    @Qualifier("arbeidssokerRestClient") private val restClient: RestClient,
) {
    fun hentPerioder(
        personIdent: String,
    ): List<ArbeidssøkerPeriode> {
        val uriBuilder =
            UriComponentsBuilder
                .fromUri(uriGcp)
                .pathSegment("api/v1/veileder/arbeidssoekerperioder")

        return restClient
            .post()
            .uri(uriBuilder.build().toUri())
            .body(FnrArbeidssøker(personIdent))
            .retrieve()
            .body<List<ArbeidssøkerPeriode>>()!!
    }
}

data class FnrArbeidssøker(
    val identitetsnummer: String,
)
