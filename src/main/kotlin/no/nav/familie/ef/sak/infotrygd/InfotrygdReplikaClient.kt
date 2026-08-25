package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class InfotrygdReplikaClient(
    @Value("\${INFOTRYGD_REPLIKA_API_URL}")
    private val infotrygdReplikaUri: URI,
    @Qualifier("infotrygdReplikaRestClient")
    private val restClient: RestClient,
) {
    private val perioderUri: URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaUri)
            .pathSegment("api/perioder")
            .build()
            .toUri()

    private val sammenslåttePerioderUri: URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaUri)
            .pathSegment("api/perioder/sammenslatte")
            .build()
            .toUri()

    private val finnSakerUri: URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaUri)
            .pathSegment("api/saker/finn")
            .build()
            .toUri()

    private val eksistererUri: URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaUri)
            .pathSegment("api/stonad/eksisterer")
            .build()
            .toUri()

    private fun migreringspersonerUri(antall: Int): URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaUri)
            .pathSegment("api/perioder/migreringspersoner")
            .queryParam("antall", antall)
            .build()
            .toUri()

    fun hentPerioder(request: InfotrygdPeriodeRequest): InfotrygdPeriodeResponse =
        restClient
            .post()
            .uri(perioderUri)
            .body(request)
            .retrieve()
            .body<InfotrygdPeriodeResponse>()!!

    fun hentSammenslåttePerioder(request: InfotrygdPeriodeRequest): InfotrygdPeriodeResponse =
        restClient
            .post()
            .uri(sammenslåttePerioderUri)
            .body(request)
            .retrieve()
            .body<InfotrygdPeriodeResponse>()!!

    fun hentSaker(request: InfotrygdSøkRequest): InfotrygdSakResponse =
        restClient
            .post()
            .uri(finnSakerUri)
            .body(request)
            .retrieve()
            .body<InfotrygdSakResponse>()!!

    fun hentPersonerForMigrering(antall: Int): Set<String> {
        val response =
            restClient
                .get()
                .uri(migreringspersonerUri(antall))
                .retrieve()
                .body<Map<String, Any>>()!!
        @Suppress("UNCHECKED_CAST")
        return (response.getValue("personIdenter") as List<String>).toSet()
    }

    /**
     * Infotrygd skal alltid returnere en stønadTreff for hver søknadType som er input
     */
    fun hentInslagHosInfotrygd(request: InfotrygdSøkRequest): InfotrygdFinnesResponse {
        require(request.personIdenter.isNotEmpty()) { "Identer har ingen verdier" }
        return restClient
            .post()
            .uri(eksistererUri)
            .body(request)
            .retrieve()
            .body<InfotrygdFinnesResponse>()!!
    }
}
