package no.nav.familie.ef.sak.sigrun.ekstern

import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class SigrunClient(
    @Value("\${SIGRUN_URL}") private val uri: URI,
    @Qualifier("sigrun-clientcredentials") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "sigrun") {
    fun hentPensjonsgivendeInntekt(
        fødselsnummer: String,
        inntektsår: Int,
    ): PensjonsgivendeInntektResponse {
        val uri =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("v1", "pensjonsgivendeinntektforfolketrygden")
                .build()
                .toUri()

        val request =
            PensjonsgivendeInntektRequest(
                personident = fødselsnummer,
                inntektsaar = inntektsår.toString(),
            )

        val response = postForEntity<PensjonsgivendeInntektResponse>(uri, request)
        secureLogger.info("Pensjonsgivende inntekt for inntektsår $inntektsår: $response")
        return response
    }
}
