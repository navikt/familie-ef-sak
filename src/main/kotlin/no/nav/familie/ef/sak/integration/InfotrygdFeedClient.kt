package no.nav.familie.ef.sak.integration

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.ef.infotrygd.OpprettPeriodeHendelseDto
import no.nav.familie.kontrakter.ef.infotrygd.OpprettStartBehandlingHendelseDto
import no.nav.familie.kontrakter.ef.infotrygd.OpprettVedtakHendelseDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI


@Service
class InfotrygdFeedClient(@Value("\${INFOTRYGD_FEED_API_URL}")
                          private val infotrygdFeedUri: URI,
                          @Qualifier("azure")
                          restOperations: RestOperations)
    : AbstractPingableRestClient(restOperations, "infotrygd.feed") {


    val opprettVedtakUri: URI =
            UriComponentsBuilder.fromUri(infotrygdFeedUri).pathSegment("api/entry/vedtak").build().toUri()
    val opprettStartBehandlingUri: URI =
            UriComponentsBuilder.fromUri(infotrygdFeedUri).pathSegment("api/entry/start-behandling").build().toUri()
    val opprettPeriodeUri: URI =
            UriComponentsBuilder.fromUri(infotrygdFeedUri).pathSegment("api/entry/periode").build().toUri()

    fun opprettVedtakHendelse(hendelseDto: OpprettVedtakHendelseDto) {
        postForEntity<Any>(opprettVedtakUri, hendelseDto)
    }

    fun opprettStartBehandlingHendelse(hendelseDto: OpprettStartBehandlingHendelseDto) {
        postForEntity<Any>(opprettStartBehandlingUri, hendelseDto)
    }

    fun opprettPeriodeHendelse(hendelseDto: OpprettPeriodeHendelseDto) {
        postForEntity<Any>(opprettPeriodeUri, hendelseDto)
    }

    override val pingUri: URI
        get() = UriComponentsBuilder.fromUri(infotrygdFeedUri).pathSegment("api/ping").build().toUri()

}
