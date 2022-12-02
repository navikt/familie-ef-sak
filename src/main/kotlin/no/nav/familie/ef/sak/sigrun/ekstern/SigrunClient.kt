package no.nav.familie.ef.sak.sigrun.ekstern

import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

class SigrunClient(
    @Value("\${FAMILIE_EF_PROXY_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations
) : AbstractRestClient(restOperations, "sigrun") {

    // Bruke API-key i stedet for å unngå proxy-repo?

    fun hentSummertSkattegrunnlag(aktørId: Long, inntektsår: Int): SummertSkattegrunnlag {
        val uri = UriComponentsBuilder.fromUri(uri).pathSegment("api/v1/summertskattegrunnlag")
            .queryParam("inntektsfilter", "SummertSkattegrunnlagEnsligForsorger")
            .queryParam("inntektsaar", inntektsår.toString())
            .build().toUri()

        val headers = HttpHeaders()
        headers.set("Nav-Personident", aktørId.toString())
        return getForEntity(uri, headers)
    }

    fun hentBeregnetSkatt(aktørId: Long, inntektsår: Int): List<BeregnetSkatt> {
        val uri = UriComponentsBuilder.fromUri(uri).pathSegment("api/v1/beregnetskatt").build().toUri()

        val headers = HttpHeaders()
        headers.set("x-filter", "BeregnetSkattPensjonsgivendeInntekt")
        headers.set("x-aktoerid", aktørId.toString())
        headers.set("x-inntektsaar", inntektsår.toString())
        return getForEntity(uri, headers)
    }
}
