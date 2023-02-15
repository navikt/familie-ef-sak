package no.nav.familie.ef.sak.sigrun.ekstern

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

class SigrunClient(
    @Value("\${FAMILIE_EF_PROXY_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "sigrun") {

    fun hentSummertSkattegrunnlag(fødselsnummer: String, inntektsår: Int): SummertSkattegrunnlag {
        val uri = UriComponentsBuilder.fromUri(uri).pathSegment("api/v1/summertskattegrunnlag")
            .queryParam("inntektsaar", inntektsår.toString())
            .build().toUri()

        return postForEntity(uri, PersonIdent(fødselsnummer))
    }

    fun hentBeregnetSkatt(fødselsnummer: String, inntektsår: Int): List<BeregnetSkatt> {
        val uri = UriComponentsBuilder.fromUri(uri).pathSegment("api/v1/beregnetskatt")
            .queryParam("inntektsaar", inntektsår)
            .build().toUri()

        return postForEntity(uri, PersonIdent(fødselsnummer))
    }
}
