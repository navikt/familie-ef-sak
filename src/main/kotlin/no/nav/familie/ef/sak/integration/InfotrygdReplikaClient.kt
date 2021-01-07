package no.nav.familie.ef.sak.integration

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI


@Service
class InfotrygdReplikaClient(@Value("\${INFOTRYGD_REPLIKA_API_URL}")
                             private val infotrygdFeedUri: URI,
                             @Qualifier("azure")
                             restOperations: RestOperations)
    : AbstractPingableRestClient(restOperations, "infotrygd.replika") {

    private val perioderOvergangsstønadUri: URI =
            UriComponentsBuilder.fromUri(infotrygdFeedUri).pathSegment("api/perioder/overgangsstonad").build().toUri()

    fun hentPerioderOvergangsstønad(request: InfotrygdPerioderOvergangsstønadRequest)
            : InfotrygdPerioderOvergangsstønadResponse {
        return postForEntity(perioderOvergangsstønadUri, request)
    }

    override val pingUri: URI
        get() = UriComponentsBuilder.fromUri(infotrygdFeedUri).pathSegment("api/ping").build().toUri()

}
