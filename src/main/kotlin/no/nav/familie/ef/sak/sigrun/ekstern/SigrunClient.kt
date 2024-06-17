package no.nav.familie.ef.sak.sigrun.ekstern

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class SigrunClient(
    @Value("\${FAMILIE_EF_PROXY_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "sigrun") {
    fun hentPensjonsgivendeInntekt(
        fødselsnummer: String,
        inntektsår: Int,
    ): PensjonsgivendeInntektResponse {
        val uri =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/sigrun/pensjonsgivendeinntekt")
                .queryParam("inntektsaar", inntektsår.toString())
                .build()
                .toUri()

        val response = postForEntity<PensjonsgivendeInntektResponse>(uri, PersonIdent(fødselsnummer))
        secureLogger.info("Pensjonsgivende inntekt for inntektsår $inntektsår: $response") // Fjernes når det er litt mer kjennskap til dataene
        return response
    }

    fun hentSummertSkattegrunnlag(
        fødselsnummer: String,
        inntektsår: Int,
    ): SummertSkattegrunnlag {
        val uri =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/sigrun/summertskattegrunnlag")
                .queryParam("inntektsaar", inntektsår.toString())
                .build()
                .toUri()

        val response = postForEntity<SummertSkattegrunnlag>(uri, PersonIdent(fødselsnummer))
        secureLogger.info("Summert skattegrunnlag for inntektsår $inntektsår: $response") // Fjernes når det er litt mer kjennskap til dataene
        return response
    }

    fun hentBeregnetSkatt(
        fødselsnummer: String,
        inntektsår: Int,
    ): List<BeregnetSkatt> {
        val uri =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/sigrun/beregnetskatt")
                .queryParam("inntektsaar", inntektsår)
                .build()
                .toUri()

        val response = postForEntity<List<BeregnetSkatt>>(uri, PersonIdent(fødselsnummer))
        secureLogger.info("Beregnet skattegrunnlag for inntektsår $inntektsår: $response") // Fjernes når det er litt mer kjennskap til dataene
        return response
    }
}
