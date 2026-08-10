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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class TilbakekrevingClient(
    @Qualifier("tilbakekrevingRestClient") private val restClient: RestClient,
    @Value("\${FAMILIE_TILBAKE_URL}") private val familieTilbakeUri: URI,
) {
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
        restClient
            .post()
            .uri(hentForhåndsvisningVarselbrevUri)
            .headers { it.accept = listOf(MediaType.APPLICATION_PDF) }
            .body(forhåndsvisVarselbrevRequest)
            .retrieve()
            .body<ByteArray>()!!

    fun finnesÅpenBehandling(fagsakEksternId: Long): Boolean {
        val response: Ressurs<FinnesBehandlingResponse> =
            restClient
                .get()
                .uri(finnesÅpenBehandlingUri(fagsakEksternId))
                .retrieve()
                .body<Ressurs<FinnesBehandlingResponse>>()!!
        return response.getDataOrThrow().finnesÅpenBehandling
    }

    fun finnBehandlinger(eksternFagsakId: Long): List<Behandling> {
        val response: Ressurs<List<Behandling>> =
            restClient
                .get()
                .uri(finnBehandlingerUri(eksternFagsakId))
                .retrieve()
                .body<Ressurs<List<Behandling>>>()!!
        return response.getDataOrThrow()
    }

    fun finnVedtak(eksternFagsakId: Long): List<FagsystemVedtak> {
        val response: Ressurs<List<FagsystemVedtak>> =
            restClient
                .get()
                .uri(finnVedtakUri(eksternFagsakId))
                .retrieve()
                .body<Ressurs<List<FagsystemVedtak>>>()!!
        return response.getDataOrThrow()
    }

    fun kanBehandlingOpprettesManuelt(
        stønadstype: StønadType,
        eksternFagsakId: Long,
    ): KanBehandlingOpprettesManueltRespons {
        val response: Ressurs<KanBehandlingOpprettesManueltRespons> =
            restClient
                .get()
                .uri(kanBehandlingOpprettesManueltUri(stønadstype, eksternFagsakId))
                .retrieve()
                .body<Ressurs<KanBehandlingOpprettesManueltRespons>>()!!

        return response.getDataOrThrow()
    }

    fun opprettManuellTilbakekreving(
        eksternFagsakId: Long,
        kravgrunnlagsreferanse: String,
        stønadstype: StønadType,
    ): Ressurs<String> =
        restClient
            .post()
            .uri(opprettManueltTilbakekrevingUri)
            .body(
                OpprettManueltTilbakekrevingRequest(
                    eksternFagsakId.toString(),
                    Ytelsestype.valueOf(stønadstype.name),
                    kravgrunnlagsreferanse,
                ),
            ).retrieve()
            .body<Ressurs<String>>()!!
}
