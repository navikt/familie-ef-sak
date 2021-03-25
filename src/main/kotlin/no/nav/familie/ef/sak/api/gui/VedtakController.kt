package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.ApiFeil
import no.nav.familie.ef.sak.api.beregning.VedtakDto
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.api.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.api.dto.TotrinnskontrollStatusDto
import no.nav.familie.ef.sak.service.*
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping(path = ["/api/vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtakController(private val stegService: StegService,
                       private val behandlingService: BehandlingService,
                       private val totrinnskontrollService: TotrinnskontrollService,
                       private val tilgangService: TilgangService,
                       private val vedtakService: VedtakService) {

    @PostMapping("/{behandlingId}/send-til-beslutter")
    fun sendTilBeslutter(@PathVariable behandlingId: UUID): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        return Ressurs.success(stegService.håndterSendTilBeslutter(behandling).id)
    }

    @PostMapping("/{behandlingId}/beslutte-vedtak")
    fun beslutteVedtak(@PathVariable behandlingId: UUID,
                       @RequestBody request: BeslutteVedtakDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        if (!request.godkjent && request.begrunnelse.isNullOrBlank()) {
            throw ApiFeil("Mangler begrunnelse", HttpStatus.BAD_REQUEST)
        }
        val behandling = behandlingService.hentBehandling(behandlingId)
        return Ressurs.success(stegService.håndterBeslutteVedtak(behandling, request).id)
    }

    @GetMapping("{behandlingId}/totrinnskontroll")
    fun hentTotrinnskontroll(@PathVariable behandlingId: UUID): ResponseEntity<Ressurs<TotrinnskontrollStatusDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId)
        return ResponseEntity.ok(Ressurs.success(totrinnskontroll))
    }

    @GetMapping("{behandlingId}")
    fun hentVedtak(@PathVariable behandlingId: UUID): Ressurs<VedtakDto?> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(vedtakService.hentVedtakHvisEksisterer(behandlingId))
    }
}
