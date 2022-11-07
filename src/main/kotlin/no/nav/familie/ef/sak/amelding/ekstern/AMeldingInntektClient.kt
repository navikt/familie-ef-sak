package no.nav.familie.ef.sak.amelding.ekstern

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.webflux.client.AbstractWebClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.YearMonth

@Component
class AMeldingInntektClient(
    @Value("\${FAMILIE_EF_PROXY_URL}") private val uri: URI,
    @Qualifier("azureWebClient") webClient: WebClient
) : AbstractWebClient(webClient, "inntekt") {

    private fun lagInntektUri(fom: YearMonth, tom: YearMonth) =
        UriComponentsBuilder.fromUri(uri).pathSegment("api/inntekt")
            .queryParam("fom", fom)
            .queryParam("tom", tom)
            .build().toUri()

    private val genererUrlUri = UriComponentsBuilder.fromUri(uri).pathSegment("api/ainntekt/generer-url").build().toUri()

    fun hentInntekt(personIdent: String, fom: YearMonth, tom: YearMonth): HentInntektListeResponse {
        return postForEntity(lagInntektUri(fom, tom), PersonIdent(personIdent))
    }

    fun genererAInntektUrl(personIdent: String): String {
        return postForEntity(
            genererUrlUri,
            PersonIdent(personIdent),
            HttpHeaders().apply {
                accept = listOf(MediaType.TEXT_PLAIN)
            }
        )
    }
}
