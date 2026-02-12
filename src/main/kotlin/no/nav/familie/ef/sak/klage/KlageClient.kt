package no.nav.familie.ef.sak.klage

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.UUID

@Component
class KlageClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${FAMILIE_KLAGE_URL}") private val familieKlageUri: URI,
) : AbstractRestClient(restOperations, "familie.klage") {
    private val opprettKlage =
        UriComponentsBuilder
            .fromUri(familieKlageUri)
            .pathSegment("api/ekstern/behandling/opprett")
            .build()
            .toUri()

    private val hentKlagebehandlinger =
        UriComponentsBuilder
            .fromUri(familieKlageUri)
            .pathSegment(
                "api/ekstern/behandling/${Fagsystem.EF}",
            ).build()
            .toUri()

    fun opprettKlage(opprettKlagebehandlingRequest: OpprettKlagebehandlingRequest): Any = postForEntity(opprettKlage, opprettKlagebehandlingRequest)

    fun hentKlagebehandlinger(eksternIder: Set<Long>): Map<Long, List<KlagebehandlingDto>> {
        val uri =
            UriComponentsBuilder
                .fromUri(hentKlagebehandlinger)
                .queryParam("eksternFagsakId", eksternIder.joinToString(","))
                .build()
                .toUri()
        return getForEntity<Ressurs<Map<Long, List<KlagebehandlingDto>>>>(uri).getDataOrThrow()
    }

    fun oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId: UUID) {
        val uri = URI.create("$familieKlageUri/api/ekstern/behandling/$behandlingId/gjelder-tilbakekreving")
        return patchForEntity(uri, "")
    }
}
