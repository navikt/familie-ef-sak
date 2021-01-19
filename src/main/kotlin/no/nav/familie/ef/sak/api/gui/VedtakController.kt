package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.ApiFeil
import no.nav.familie.ef.sak.api.dto.TotrinnskontrollDto
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping(path = ["/api/vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtakController(private val stegService: StegService,
                       private val behandlingService: BehandlingService,
                       private val tilgangService: TilgangService) {

    @PostMapping("/{behandlingId}/send-til-beslutter")
    fun sendTilBeslutter(@PathVariable behandlingId: UUID): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        return Ressurs.success(stegService.håndterSendTilBeslutter(behandling).id)
    }

    @PostMapping("/{behandlingId}/beslutte-vedtak")
    fun beslutteVedtak(@PathVariable behandlingId: UUID,
                       @RequestBody request: TotrinnskontrollDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        if (!request.godkjent && request.begrunnelse.isNullOrBlank()) {
            throw ApiFeil("Mangler begrunnelse", HttpStatus.BAD_REQUEST)
        }
        val behandling = behandlingService.hentBehandling(behandlingId)
        return Ressurs.success(stegService.håndterBeslutteVedtak(behandling, request).id)
    }


    //TODO status ish på totrinn
}
