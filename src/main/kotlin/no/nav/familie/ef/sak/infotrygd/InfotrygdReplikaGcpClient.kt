package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Klient mot familie-ef-infotrygd-replika i GCP.
 *
 * Brukes utelukkende til å skyggekjøre kall som allerede har blitt gjort mot [InfotrygdReplikaClient] (familie-ef-infotrygd on-prem),
 * for å verifisere at migreringen til GCP-replikaen gir samme svar. Skal ikke brukes i den ordinære saksbehandlingsflyten
 * før migreringen er ferdig verifisert, se [no.nav.familie.ef.sak.infotrygd.skygge.SkyggeInfotrygdTask].
 */
@Service
class InfotrygdReplikaGcpClient(
    @Value("\${INFOTRYGD_REPLIKA_GCP_API_URL}")
    private val infotrygdReplikaGcpUri: URI,
    @Qualifier("azure")
    restOperations: RestOperations,
) : AbstractRestClient(restOperations, "infotrygd.replika.gcp") {
    private val perioderUri: URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaGcpUri)
            .pathSegment("api/perioder")
            .build()
            .toUri()

    private val sammenslåttePerioderUri: URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaGcpUri)
            .pathSegment("api/perioder/sammenslatte")
            .build()
            .toUri()

    private val finnSakerUri: URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaGcpUri)
            .pathSegment("api/saker/finn")
            .build()
            .toUri()

    private val eksistererUri: URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaGcpUri)
            .pathSegment("api/stonad/eksisterer")
            .build()
            .toUri()

    fun hentPerioder(request: InfotrygdPeriodeRequest): InfotrygdPeriodeResponse = postForEntity(perioderUri, request)

    fun hentSammenslåttePerioder(request: InfotrygdPeriodeRequest): InfotrygdPeriodeResponse = postForEntity(sammenslåttePerioderUri, request)

    fun hentSaker(request: InfotrygdSøkRequest): InfotrygdSakResponse = postForEntity(finnSakerUri, request)

    fun hentInslagHosInfotrygd(request: InfotrygdSøkRequest): InfotrygdFinnesResponse {
        require(request.personIdenter.isNotEmpty()) { "Identer har ingen verdier" }
        return postForEntity(eksistererUri, request)
    }
}
