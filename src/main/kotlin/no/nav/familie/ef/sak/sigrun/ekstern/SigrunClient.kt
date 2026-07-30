package no.nav.familie.ef.sak.sigrun.ekstern

import no.nav.familie.kontrakter.felles.PersonIdent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class SigrunClient(
    @Value("\${FAMILIE_EF_PROXY_URL}") private val uri: URI,
    @Qualifier("efProxyRestClient") private val restClient: RestClient,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

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

        val response =
            restClient
                .post()
                .uri(uri)
                .body(PersonIdent(fødselsnummer))
                .retrieve()
                .body<PensjonsgivendeInntektResponse>()!!
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

        val response =
            restClient
                .post()
                .uri(uri)
                .body(PersonIdent(fødselsnummer))
                .retrieve()
                .body<SummertSkattegrunnlag>()!!
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

        val response =
            restClient
                .post()
                .uri(uri)
                .body(PersonIdent(fødselsnummer))
                .retrieve()
                .body<List<BeregnetSkatt>>()!!
        secureLogger.info("Beregnet skattegrunnlag for inntektsår $inntektsår: $response") // Fjernes når det er litt mer kjennskap til dataene
        return response
    }
}
