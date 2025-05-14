package no.nav.familie.ef.sak.amelding.ekstern

import no.nav.familie.ef.sak.amelding.HentInntektPayload
import no.nav.familie.ef.sak.amelding.InntektResponse
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
    @Value("\${FAMILIE_EF_PROXY_URL}") private val uri: URI,
    @Value("\${INNTEKT_URL}") private val inntektUri: URI,
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

    private val genererInntektV2Uri =
        UriComponentsBuilder
            .fromUri(inntektUri)
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
