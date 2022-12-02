package no.nav.familie.ef.sak.sigrun.ekstern

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.http.AbstractRestWebClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestOperations
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

class SigrunClient(
    @Value("\${FAMILIE_EF_PROXY_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
    @Qualifier("azureWebClient") webClient: WebClient,
    featureToggleService: FeatureToggleService
) : AbstractRestWebClient(restOperations, webClient, "sigrun", featureToggleService) {

    // Bruke API-key i stedet for å unngå proxy-repo?

    fun hentSummertSkattegrunnlag(aktørId: Long, inntektsår: Int): SummertSkattegrunnlag {
        val uri = UriComponentsBuilder.fromUri(uri).pathSegment("api/v1/summertskattegrunnlag")
            .queryParam("inntektsaar", inntektsår.toString())
            .build().toUri()

        val headers = HttpHeaders()
        headers.set("Nav-Personident", aktørId.toString())
        return getForEntity(uri, headers)
    }

    fun hentBeregnetSkatt(aktørId: Long, inntektsår: Int): List<BeregnetSkatt> {
        val uri = UriComponentsBuilder.fromUri(uri).pathSegment("api/v1/beregnetskatt").build().toUri()

        val headers = HttpHeaders()
        headers.set("x-aktoerid", aktørId.toString())
        headers.set("x-inntektsaar", inntektsår.toString())
        return getForEntity(uri, headers)
    }
}
