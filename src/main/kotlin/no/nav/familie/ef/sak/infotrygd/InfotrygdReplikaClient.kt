package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderArenaRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class InfotrygdReplikaClient(@Value("\${INFOTRYGD_REPLIKA_API_URL}")
                             private val infotrygdReplikaUri: URI,
                             @Qualifier("azure")
                             restOperations: RestOperations)
    : AbstractPingableRestClient(restOperations, "infotrygd.replika") {

    private val perioderArenaUri: URI =
            UriComponentsBuilder.fromUri(infotrygdReplikaUri).pathSegment("api/perioder/arena").build().toUri()

    private val eksistererUri: URI =
            UriComponentsBuilder.fromUri(infotrygdReplikaUri).pathSegment("api/stonad/eksisterer").build().toUri()

    fun hentPerioderArena(request: InfotrygdPerioderArenaRequest): InfotrygdPerioderResponse {
        return postForEntity(perioderArenaUri, request)
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