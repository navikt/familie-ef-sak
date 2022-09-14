package no.nav.familie.ef.sak.klage

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class KlageClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${FAMILIE_KLAGE_URL}") private val familieKlageUri: URI
) :
    AbstractRestClient(restOperations, "familie.klage") {

    private val opprettKlage =
        UriComponentsBuilder.fromUri(familieKlageUri).pathSegment("api/behandling/opprett").build().toUri()

    private val hentKlagebehandlinger =
        UriComponentsBuilder.fromUri(familieKlageUri).pathSegment(
            "api/ekstern/behandling/${Fagsystem.EF}"
        ).build().toUri()

    fun opprettKlage(opprettKlagebehandlingRequest: OpprettKlagebehandlingRequest) {
        return postForEntity(opprettKlage, opprettKlagebehandlingRequest)
    }

    fun hentKlagebehandlinger(eksternIder: Set<Long>): KlagebehandlingDto {
        val uri = UriComponentsBuilder.fromUri(hentKlagebehandlinger).queryParam("eksternIder", eksternIder)
        return getForEntity<Ressurs<KlagebehandlingDto>>(hentKlagebehandlinger).data!!
    }
}
