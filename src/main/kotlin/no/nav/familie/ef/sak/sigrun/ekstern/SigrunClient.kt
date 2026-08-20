package no.nav.familie.ef.sak.sigrun.ekstern

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Kaller Sigrun (Skatteetaten) direkte istedenfor via familie-ef-proxy.
 * Krever maskin-til-maskin Azure AD-token (SIGRUN_SCOPE).
 */
@Component
class SigrunClient(
    @Value("\${SIGRUN_URL}") private val uri: URI,
    @Qualifier("sigrunRestClient") private val restClient: RestClient,
) {
    fun hentPensjonsgivendeInntekt(
        fødselsnummer: String,
        inntektsår: Int,
    ): PensjonsgivendeInntektResponse {
        val uri =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api", "v1", "pensjonsgivendeinntektforfolketrygden")
                .build()
                .toUri()

        val request =
            PensjonsgivendeInntektRequest(
                personident = fødselsnummer,
                inntektsaar = inntektsår.toString(),
            )

        return try {
            restClient
                .post()
                .uri(uri)
                .body(request)
                .retrieve()
                .body<PensjonsgivendeInntektResponse>()!!
        } catch (e: HttpClientErrorException.NotFound) {
            PensjonsgivendeInntektResponse(fødselsnummer, inntektsår, emptyList())
        }
    }
}

data class PensjonsgivendeInntektRequest(
    val personident: String,
    val inntektsaar: String,
    val rettighetspakke: String = "navEnsligForsoerger",
)
