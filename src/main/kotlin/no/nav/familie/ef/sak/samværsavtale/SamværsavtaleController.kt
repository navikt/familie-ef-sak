package no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.samværsavtale.dto.JournalførBeregnetSamværRequest
import no.nav.familie.ef.sak.samværsavtale.dto.SamværsavtaleDto
import no.nav.familie.ef.sak.samværsavtale.dto.tilDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/samvaersavtale"])
@ProtectedWithClaims(issuer = "azuread")
class SamværsavtaleController(
    private val samværsavtaleService: SamværsavtaleService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("{behandlingId}")
    fun hentSamværsavtaler(
        @PathVariable behandlingId: UUID,
    ): Ressurs<List<SamværsavtaleDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(samværsavtaleService.hentSamværsavtalerForBehandling(behandlingId).tilDto())
    }

    @PostMapping
    fun oppdaterSamværsavtale(
        @RequestBody request: SamværsavtaleDto,
    ): Ressurs<List<SamværsavtaleDto>> {
        tilgangService.validerTilgangTilBehandling(request.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        samværsavtaleService.opprettEllerErstattSamværsavtale(request)
        return Ressurs.success(samværsavtaleService.hentSamværsavtalerForBehandling(request.behandlingId).tilDto())
    }

    @PostMapping("/journalfor")
    fun journalførBeregnetSamvær(
        @RequestBody request: JournalførBeregnetSamværRequest,
    ): Ressurs<String> {
        tilgangService.validerTilgangTilPersonMedBarn(request.personIdent, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(samværsavtaleService.journalførBeregnetSamvær(request))
    }

    @DeleteMapping("{behandlingId}/{behandlingBarnId}")
    fun slettSamværsavtale(
        @PathVariable behandlingId: UUID,
        @PathVariable behandlingBarnId: UUID,
    ): Ressurs<List<SamværsavtaleDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.DELETE)
        tilgangService.validerHarSaksbehandlerrolle()
        samværsavtaleService.slettSamværsavtale(behandlingId, behandlingBarnId)
        return Ressurs.success(samværsavtaleService.hentSamværsavtalerForBehandling(behandlingId).tilDto())
    }
}
