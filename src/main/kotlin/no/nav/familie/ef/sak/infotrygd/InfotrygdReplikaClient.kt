package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class InfotrygdReplikaClient(
    @Value("\${INFOTRYGD_REPLIKA_API_URL}")
    private val infotrygdReplikaUri: URI,
    @Qualifier("azure")
    restOperations: RestOperations,
) :
    AbstractPingableRestClient(restOperations, "infotrygd.replika") {

    private val perioderUri: URI =
        UriComponentsBuilder.fromUri(infotrygdReplikaUri).pathSegment("api/perioder").build().toUri()

    private val sammenslåttePerioderUri: URI =
        UriComponentsBuilder.fromUri(infotrygdReplikaUri).pathSegment("api/perioder/sammenslatte").build().toUri()

    private val finnSakerUri: URI =
        UriComponentsBuilder.fromUri(infotrygdReplikaUri).pathSegment("api/saker/finn").build().toUri()

    private val eksistererUri: URI =
        UriComponentsBuilder.fromUri(infotrygdReplikaUri).pathSegment("api/stonad/eksisterer").build().toUri()

    private fun migreringspersonerUri(antall: Int): URI =
        UriComponentsBuilder.fromUri(infotrygdReplikaUri)
            .pathSegment("api/perioder/migreringspersoner")
            .queryParam("antall", antall)
            .build()
            .toUri()

    fun hentPerioder(request: InfotrygdPeriodeRequest): InfotrygdPeriodeResponse {
        return postForEntity(perioderUri, request)
    }

    fun hentSammenslåttePerioder(request: InfotrygdPeriodeRequest): InfotrygdPeriodeResponse {
        return postForEntity(sammenslåttePerioderUri, request)
    }

    fun hentSaker(request: InfotrygdSøkRequest): InfotrygdSakResponse {
        return postForEntity(finnSakerUri, request)
    }

    fun hentPersonerForMigrering(antall: Int): Set<String> {
        val response = getForEntity<Map<String, Any>>(migreringspersonerUri(antall))
        @Suppress("UNCHECKED_CAST")
        return (response.getValue("personIdenter") as List<String>).toSet()
    }

    /**
     * Infotrygd skal alltid returnere en stønadTreff for hver søknadType som er input
     */
    fun hentInslagHosInfotrygd(request: InfotrygdSøkRequest): InfotrygdFinnesResponse {
        require(request.personIdenter.isNotEmpty()) { "Identer har ingen verdier" }
        return postForEntity(eksistererUri, request)
    }

    override val pingUri: URI
        get() = UriComponentsBuilder.fromUri(infotrygdReplikaUri).pathSegment("api/ping").build().toUri()
}
