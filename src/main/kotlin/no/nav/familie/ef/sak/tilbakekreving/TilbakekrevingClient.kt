package no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.FinnesBehandlingResponse
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.KanBehandlingOpprettesManueltRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class TilbakekrevingClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${FAMILIE_TILBAKE_URL}") private val familieTilbakeUri: URI,
) : AbstractRestClient(restOperations, "familie.tilbakekreving") {
    private val hentForhåndsvisningVarselbrevUri: URI =
        UriComponentsBuilder
            .fromUri(familieTilbakeUri)
            .pathSegment("api/dokument/forhandsvis-varselbrev")
            .build()
            .toUri()

    private val opprettManueltTilbakekrevingUri =
        UriComponentsBuilder
            .fromUri(familieTilbakeUri)
            .pathSegment("api/behandling/manuelt/task/v1")
            .build()
            .toUri()

    private fun kanBehandlingOpprettesManueltUri(
        stønadstype: StønadType,
        eksternFagsakId: Long,
    ) = UriComponentsBuilder
        .fromUri(familieTilbakeUri)
        .pathSegment(
            "api",
            "ytelsestype",
            stønadstype.name,
            "fagsak",
            eksternFagsakId.toString(),
            "kanBehandlingOpprettesManuelt",
            "v1",
        ).encode()
        .build()
        .toUri()

    private fun finnesÅpenBehandlingUri(eksternFagsakId: Long) =
        UriComponentsBuilder
            .fromUri(familieTilbakeUri)
            .pathSegment("api/fagsystem/${Fagsystem.EF}/fagsak/$eksternFagsakId/finnesApenBehandling/v1")
            .build()
            .toUri()

    private fun finnBehandlingerUri(eksternFagsakId: Long) =
        UriComponentsBuilder
            .fromUri(familieTilbakeUri)
            .pathSegment("api/fagsystem/${Fagsystem.EF}/fagsak/$eksternFagsakId/behandlinger/v1")
            .build()
            .toUri()

    private fun finnVedtakUri(eksternFagsakId: Long) =
        UriComponentsBuilder
            .fromUri(familieTilbakeUri)
            .pathSegment("api/fagsystem/${Fagsystem.EF}/fagsak/$eksternFagsakId/vedtak/v1")
            .build()
            .toUri()

    fun hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray =
        postForEntity(
            hentForhåndsvisningVarselbrevUri,
            forhåndsvisVarselbrevRequest,
            HttpHeaders().apply { accept = listOf(MediaType.APPLICATION_PDF) },
        )

    fun finnesÅpenBehandling(fagsakEksternId: Long): Boolean {
        val response: Ressurs<FinnesBehandlingResponse> = getForEntity(finnesÅpenBehandlingUri(fagsakEksternId))
        return response.getDataOrThrow().finnesÅpenBehandling
    }

    fun finnBehandlinger(eksternFagsakId: Long): List<Behandling> {
        val response: Ressurs<List<Behandling>> = getForEntity(finnBehandlingerUri(eksternFagsakId))
        return response.getDataOrThrow()
    }

    fun finnVedtak(eksternFagsakId: Long): List<FagsystemVedtak> {
        val response: Ressurs<List<FagsystemVedtak>> = getForEntity(finnVedtakUri(eksternFagsakId))
        return response.getDataOrThrow()
    }

    fun kanBehandlingOpprettesManuelt(
        stønadstype: StønadType,
        eksternFagsakId: Long,
    ): KanBehandlingOpprettesManueltRespons {
        val response: Ressurs<KanBehandlingOpprettesManueltRespons> =
            getForEntity(kanBehandlingOpprettesManueltUri(stønadstype, eksternFagsakId))

        return response.getDataOrThrow()
    }

    fun opprettManuellTilbakekreving(
        eksternFagsakId: Long,
        kravgrunnlagsreferanse: String,
        stønadstype: StønadType,
    ): Ressurs<String> =
        postForEntity(
            opprettManueltTilbakekrevingUri,
            OpprettManueltTilbakekrevingRequest(
                eksternFagsakId.toString(),
                Ytelsestype.valueOf(stønadstype.name),
                kravgrunnlagsreferanse,
            ),
        )
}
