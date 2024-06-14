package no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.tilbakekreving.domain.tilDto
import no.nav.familie.ef.sak.tilbakekreving.dto.TilbakekrevingDto
import no.nav.familie.ef.sak.tilbakekreving.dto.VarseltekstDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling as TilbakekrevingBehandling

@RestController
@RequestMapping(path = ["/api/tilbakekreving"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class TilbakekrevingController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    @PostMapping("/{behandlingId}")
    fun lagreTilbakekreving(
        @PathVariable behandlingId: UUID,
        @RequestBody tilbakekrevingDto: TilbakekrevingDto,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto, behandlingId)
        return Ressurs.success(behandlingId)
    }

    @PostMapping("/fagsak/{fagsakId}/opprett-tilbakekreving")
    fun opprettManuellTilbakekreving(
        @PathVariable fagsakId: UUID,
    ): Ressurs<String> {
        tilgangService.validerTilgangTilFagsak(fagsakId = fagsakId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()
        tilbakekrevingService.opprettManuellTilbakekreving(fagsakId)
        return Ressurs.success("Opprettelse av manuell behandling iverksatt.")
    }

    @GetMapping("/{behandlingId}/er-allerede-opprettet")
    fun finnesTilbakekreving(
        @PathVariable behandlingId: UUID,
    ): Ressurs<Boolean> {
        return Ressurs.success(tilbakekrevingService.finnesÅpenTilbakekrevingsBehandling(behandlingId))
    }

    @GetMapping("/{behandlingId}/finnes-flere-tilbakekrevinger-valgt-siste-aar")
    fun finnesFlereTilbakekrevingerValgtSisteÅr(
        @PathVariable behandlingId: UUID,
    ): Ressurs<Boolean> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(tilbakekrevingService.finnesFlereTilbakekrevingerValgtSisteÅr(behandlingId))
    }

    @GetMapping("/{behandlingId}")
    fun hentTilbakekreving(
        @PathVariable behandlingId: UUID,
    ): Ressurs<TilbakekrevingDto?> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(tilbakekrevingService.hentTilbakekreving(behandlingId)?.tilDto())
    }

    @GetMapping("/behandlinger/{fagsakId}")
    fun hentTilbakekekrevingBehandlinger(
        @PathVariable fagsakId: UUID,
    ): Ressurs<List<TilbakekrevingBehandling>> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(tilbakekrevingService.hentTilbakekrevingBehandlinger(fagsakId))
    }

    @GetMapping("/{behandlingId}/brev")
    fun genererBrevMedEksisterendeVarseltekst(
        @PathVariable behandlingId: UUID,
    ): Ressurs<ByteArray> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.UPDATE)
        return Ressurs.success(tilbakekrevingService.genererBrevMedVarseltekstFraEksisterendeTilbakekreving(saksbehandling))
    }

    @PostMapping("/{behandlingId}/brev/generer")
    fun genererTilbakekekrevingBrevMedVarseltekst(
        @PathVariable behandlingId: UUID,
        @RequestBody varseltekstDto: VarseltekstDto,
    ): Ressurs<ByteArray> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.UPDATE)
        return Ressurs.success(tilbakekrevingService.genererBrev(saksbehandling, varseltekstDto.varseltekst))
    }
}
