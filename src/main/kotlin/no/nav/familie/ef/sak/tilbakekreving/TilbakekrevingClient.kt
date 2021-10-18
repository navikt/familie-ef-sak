package no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.FinnesBehandlingResponse
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class TilbakekrevingClient(@Qualifier("azure") restOperations: RestOperations,
                           @Value("\${FAMILIE_TILBAKE_URL}") private val familieTilbakeUri: URI)
    : AbstractRestClient(restOperations, "familie.tilbakekreving") {

    private val hentForhåndsvisningVarselbrevUri: URI = UriComponentsBuilder.fromUri(familieTilbakeUri)
            .pathSegment("api/dokument/forhandsvis-varselbrev")
            .build()
            .toUri()

    private fun finnesÅpenBehandlingUri(fagsakExternId: Long) = UriComponentsBuilder.fromUri(familieTilbakeUri)
            .pathSegment("api/fagsystem/${Fagsystem.EF}/fagsak/$fagsakExternId/finnesApenBehandling/v1")
            .build()
            .toUri()

    private fun finnBehandlingerUri(fagsakExternId: Long) = UriComponentsBuilder.fromUri(familieTilbakeUri)
            .pathSegment("api/fagsystem/${Fagsystem.EF}/fagsak/$fagsakExternId/behandlinger/v1")
            .build()
            .toUri()


    fun hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        return postForEntity(hentForhåndsvisningVarselbrevUri,
                             forhåndsvisVarselbrevRequest,
                             HttpHeaders().apply { accept = listOf(MediaType.APPLICATION_PDF) })
    }

    fun finnesÅpenBehandling(fagsakEksternId: Long): Boolean {
        val response: Ressurs<FinnesBehandlingResponse> = getForEntity(finnesÅpenBehandlingUri(fagsakEksternId))
        return response.getDataOrThrow().finnesÅpenBehandling
    }

    fun finnBehandlinger(fagsakExternId: Long): List<Behandling> {
        val response: Ressurs<List<Behandling>> = getForEntity(finnBehandlingerUri(fagsakExternId))
        return response.getDataOrThrow()
    }

}
