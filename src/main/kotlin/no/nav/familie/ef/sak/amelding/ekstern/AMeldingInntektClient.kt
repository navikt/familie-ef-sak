package no.nav.familie.ef.sak.amelding.ekstern

import no.nav.familie.ef.sak.amelding.HentInntektPayload
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.logger
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.YearMonth

@Component
class AMeldingInntektClient(
    @Value("\${INNTEKT_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "inntekt") {
    private fun lagInntektUri(
        fom: YearMonth,
        tom: YearMonth,
    ) = UriComponentsBuilder
        .fromUri(uri)
        .pathSegment("api/inntekt")
        .queryParam("fom", fom)
        .queryParam("tom", tom)
        .build()
        .toUri()

    private val genererInntektV2NyIngress =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("rest/v2/inntekt")
            .build()
            .toUri()

    private val genererInntektV2Uri =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api/inntekt/v2")
            .build()
            .toUri()

    private val genererUrlUri =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api/ainntekt/generer-url")
            .build()
            .toUri()

    private val genererUrlUriArbeidsforhold =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api/ainntekt/generer-url-arbeidsforhold")
            .build()
            .toUri()

    fun hentInntektNyIngress(
        personIdent: String,
        månedFom: YearMonth,
        månedTom: YearMonth,
    ): Map<String, Any> {
        logger.info("--- Henter inntekt")

        val request =
            genererInntektRequest(
                personIdent = personIdent,
                månedFom = månedFom,
                månedTom = månedTom,
            )

        logger.info("--- request $request")

        val payload = request
        val token = genererToken()
        val entity =
            postForEntity<Map<String, Any>>(
                uri = genererInntektV2NyIngress,
                payload = payload,
                httpHeaders =
                    headers(
                        token = token.toString(),
                    ),
            )

        logger.info("--- entity $entity")

        return entity
    }

    private val tokenEndpoint = System.getenv("NAIS_TOKEN_ENDPOINT")

    private fun genererToken() {
        logger.info("--- generer token")
        val uri = URI.create(tokenEndpoint)

        val payload =
            mapOf(
                "identity_provider" to "azuread",
                "target" to "api://dev-gcp.teamfamilie.familie-ef-sak/.default",
            )

//        val body = ObjectMapperProvider.objectMapper.writeValueAsString(obj)

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                accept = listOf(MediaType.APPLICATION_JSON)
            }

        logger.info("--- uri $uri")
        logger.info("--- payload $payload")
        logger.info("--- headers $headers")

        val res =
            postForEntity<Any>( // TODO: Sett korrekt type
                uri = uri,
                payload = payload,
                httpHeaders = headers,
            )

        logger.info("--- res $res")
//
        return res as Unit
    }

//    private fun token(): String = Texas().genererToken()

    private fun headers(token: String): HttpHeaders =
        HttpHeaders().apply {
            setBearerAuth(token)
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
        }

    private fun genererInntektRequest(
        personIdent: String,
        månedFom: YearMonth,
        månedTom: YearMonth,
    ) = mapOf(
        "personident" to personIdent,
        "filter" to "StoenadEnsligMorEllerFarA-inntekt",
        "formaal" to "StoenadEnsligMorEllerFar",
        "maanedFom" to månedFom,
        "maanedTom" to månedTom,
    )

    fun hentInntekt(
        personIdent: String,
        månedFom: YearMonth,
        månedTom: YearMonth,
    ): InntektResponse =
        postForEntity(
            uri = genererInntektV2Uri,
            payload =
                HentInntektPayload(
                    personIdent = personIdent,
                    månedFom = månedFom,
                    månedTom = månedTom,
                ),
        )

    fun genererAInntektUrl(personIdent: String): String =
        postForEntity(
            genererUrlUri,
            PersonIdent(personIdent),
            HttpHeaders().apply {
                accept = listOf(MediaType.TEXT_PLAIN)
            },
        )

    fun genererAInntektArbeidsforholdUrl(personIdent: String): String =
        postForEntity(
            genererUrlUriArbeidsforhold,
            PersonIdent(personIdent),
            HttpHeaders().apply {
                accept = listOf(MediaType.TEXT_PLAIN)
            },
        )
}
