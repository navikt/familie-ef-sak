package no.nav.familie.ef.sak.sigrun.ekstern

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.http.AbstractRestWebClient
import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class SigrunClient(
    @Value("\${FAMILIE_EF_PROXY_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
    @Qualifier("azureWebClient") webClient: WebClient,
    featureToggleService: FeatureToggleService
) : AbstractRestWebClient(restOperations, webClient, "sigrun", featureToggleService) {

    fun hentSummertSkattegrunnlag(fødselsnummer: String, inntektsår: Int): SummertSkattegrunnlag {
        val uri = UriComponentsBuilder.fromUri(uri).pathSegment("api/v1/summertskattegrunnlag")
            .queryParam("inntektsaar", inntektsår.toString())
            .build().toUri()

        val response = postForEntity<SummertSkattegrunnlag>(uri, PersonIdent(fødselsnummer))
        secureLogger.info("Summert skattegrunnlag for inntektsår $inntektsår: $response") //Fjernes når det er litt mer kjennskap til dataene
        return response
    }

    fun hentBeregnetSkatt(fødselsnummer: String, inntektsår: Int): List<BeregnetSkatt> {
        val uri = UriComponentsBuilder.fromUri(uri).pathSegment("api/v1/beregnetskatt")
            .queryParam("inntektsaar", inntektsår)
            .build().toUri()

        val response = postForEntity<List<BeregnetSkatt>>(uri, PersonIdent(fødselsnummer))
        secureLogger.info("Beregnet skattegrunnlag for inntektsår $inntektsår: $response") //Fjernes når det er litt mer kjennskap til dataene
        return response
    }
}
