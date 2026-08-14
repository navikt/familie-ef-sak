package no.nav.familie.ef.sak.sigrun.ekstern

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Kaller Sigrun (Skatteetaten) direkte istedenfor via familie-ef-proxy.
 * Krever maskin-til-maskin Azure AD-token (SIGRUN_SCOPE).
 */
@Component
class SigrunClient(
    @Value("\${SIGRUN_URL}") private val uri: URI,
    @Qualifier("sigrunRestClient") sigrunClient: RestClient,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    // Legger på en loggende interceptor sist i kjeden, slik at Authorization-headeren
    // (satt av auth-interceptoren i entraIDRestClientFactory) er med når vi logger.
    private val restClient: RestClient =
        sigrunClient
            .mutate()
            .requestInterceptor(loggendeInterceptor())
            .build()

    private fun loggendeInterceptor(): ClientHttpRequestInterceptor =
        ClientHttpRequestInterceptor { request, body, execution ->
            logRequest(request, body)
            execution.execute(request, body)
        }

    private fun logRequest(
        request: HttpRequest,
        body: ByteArray,
    ) {
        val headere =
            request.headers.headerNames().joinToString("\n") { navn ->
                val verdier = request.headers[navn].orEmpty()
                val visning = if (navn.equals("Authorization", ignoreCase = true)) maskerAuthorization(verdier) else verdier
                "  $navn: $visning"
            }
        secureLogger.info(
            """
            Kall til Sigrun:
            ${request.method} ${request.uri}
            Headers:
            $headere
            Body:
            ${String(body, StandardCharsets.UTF_8)}
            """.trimIndent(),
        )
    }

    private fun maskerAuthorization(verdier: List<String>): List<String> =
        verdier.map { verdi ->
            val skjemaPrefiks = verdi.substringBefore(" ", "ukjent-skjema")
            "$skjemaPrefiks(maskert, lengde=${verdi.length})"
        }

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

        val response =
            restClient
                .post()
                .uri(uri)
                .body(request)
                .retrieve()
                .body<PensjonsgivendeInntektResponse>()!!
        secureLogger.info("Pensjonsgivende inntekt for inntektsår $inntektsår: $response") // Fjernes når det er litt mer kjennskap til dataene
        return response
    }
}

data class PensjonsgivendeInntektRequest(
    val personident: String,
    val inntektsaar: String,
    val rettighetspakke: String = "navEnsligForsoerger",
)
