package no.nav.familie.ef.sak.sigrun.ekstern

import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class SigrunClient(
    @Value("\${SIGRUN_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "sigrun") {
    fun hentPensjonsgivendeInntekt(
        fødselsnummer: String,
        inntektsår: Int,
    ): PensjonsgivendeInntektResponse? {
        val uri =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/v1/pensjonsgivendeinntektforfolketrygden")
                .build()
                .toUri()

        val request =
            PensjonsgivendeInntektRequest(
                norskPersonidentifikator = fødselsnummer,
                inntektsaar = inntektsår,
            )

        return try {
            val response = postForEntity<PensjonsgivendeInntektResponse>(uri, request)
            secureLogger.info("Pensjonsgivende inntekt for inntektsår $inntektsår: $response")
            response
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                secureLogger.info("Fant ikke pensjonsgivende inntekt for inntektsår $inntektsår (404)")
                null
            } else {
                throw e
            }
        }
    }

    fun hentSummertSkattegrunnlag(
        fødselsnummer: String,
        inntektsår: Int,
    ): SummertSkattegrunnlag? {
        val uri =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/v2/summertskattegrunnlag")
                .build()
                .toUri()

        val request =
            SummertSkattegrunnlagRequest(
                personidentifikator = fødselsnummer,
                inntektsaar = inntektsår,
            )

        return try {
            val response = postForEntity<SummertSkattegrunnlag>(uri, request)
            secureLogger.info("Summert skattegrunnlag for inntektsår $inntektsår: $response")
            response
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                secureLogger.info("Fant ikke summert skattegrunnlag for inntektsår $inntektsår (404)")
                null
            } else {
                throw e
            }
        }
    }

    fun hentBeregnetSkatt(
        fødselsnummer: String,
        inntektsår: Int,
    ): List<BeregnetSkatt> {
        val uri =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/beregnetskatt")
                .build()
                .toUri()

        val request =
            BeregnetSkattRequest(
                personidentifikator = fødselsnummer,
                inntektsaar = inntektsår,
            )

        return try {
            val response = postForEntity<List<BeregnetSkatt>>(uri, request)
            secureLogger.info("Beregnet skattegrunnlag for inntektsår $inntektsår: $response")
            response
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                secureLogger.info("Fant ikke beregnet skatt for inntektsår $inntektsår (404)")
                emptyList()
            } else {
                throw e
            }
        }
    }
}
