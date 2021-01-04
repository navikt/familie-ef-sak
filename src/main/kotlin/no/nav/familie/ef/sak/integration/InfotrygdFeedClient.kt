package no.nav.familie.ef.sak.integration

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.ef.infotrygd.OpprettPeriodeHendelseDto
import no.nav.familie.kontrakter.ef.infotrygd.OpprettStartBehandlingHendelseDto
import no.nav.familie.kontrakter.ef.infotrygd.OpprettVedtakHendelseDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
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
                          restOperations: RestOperations
) : AbstractPingableRestClient(restOperations, "infotrygd.feed") {


    private val opprettVedtakUri: URI =
            UriComponentsBuilder.fromUri(infotrygdFeedUri).pathSegment("api/entry/vedtak").build().toUri()
    private val opprettStartBehandlingUri: URI =
            UriComponentsBuilder.fromUri(infotrygdFeedUri).pathSegment("api/entry/start-behandling").build().toUri()
    private val opprettPeriodeUri: URI =
            UriComponentsBuilder.fromUri(infotrygdFeedUri).pathSegment("api/entry/periode").build().toUri()

    fun opprettVedtakHendelse(hendelseDto: OpprettVedtakHendelseDto): String {
        return postForEntity<Ressurs<String>>(opprettVedtakUri, hendelseDto).getDataOrThrow()
    }

    fun opprettStartBehandlingHendelse(hendelseDto: OpprettStartBehandlingHendelseDto): String {
        return postForEntity<Ressurs<String>>(opprettStartBehandlingUri, hendelseDto).getDataOrThrow()
    }

    fun opprettPeriodeHendelse(hendelseDto: OpprettPeriodeHendelseDto): String {
        return postForEntity<Ressurs<String>>(opprettPeriodeUri, hendelseDto).getDataOrThrow()
    }

    override val pingUri: URI
        get() = UriComponentsBuilder.fromUri(infotrygdFeedUri).pathSegment("api/ping").build().toUri()

}
