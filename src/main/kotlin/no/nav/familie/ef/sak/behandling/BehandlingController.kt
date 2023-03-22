package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandling.dto.TaAvVentStatusDto
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.vilkår.gjenbruk.GjenbrukVilkårService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(
    private val behandlingService: BehandlingService,
    private val behandlingPåVentService: BehandlingPåVentService,
    private val fagsakService: FagsakService,
    private val henleggService: HenleggService,
    private val tilgangService: TilgangService,
    private val gjenbrukVilkårService: GjenbrukVilkårService,
) {

    @GetMapping("{behandlingId}")
    fun hentBehandling(@PathVariable behandlingId: UUID): Ressurs<BehandlingDto> {
        val saksbehandling: Saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilPersonMedBarn(saksbehandling.ident, AuditLoggerEvent.ACCESS)
        return Ressurs.success(saksbehandling.tilDto())
    }

    @GetMapping("gamle-behandlinger")
    fun hentGamleUferdigeBehandlinger(): Ressurs<List<BehandlingDto>> {
        val stønadstyper = listOf(StønadType.OVERGANGSSTØNAD, StønadType.SKOLEPENGER, StønadType.BARNETILSYN)
        val gamleBehandlinger = stønadstyper.flatMap { stønadstype ->
            behandlingService.hentGamleUferdigeBehandlinger(stønadstype).map { it.tilDto(stønadstype) }
        }
        return Ressurs.success(gamleBehandlinger)
    }

    @PostMapping("{behandlingId}/vent")
    fun settPåVent(@PathVariable behandlingId: UUID): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        behandlingPåVentService.settPåVent(behandlingId)
        return Ressurs.success(behandlingId)
    }

    @GetMapping("{behandlingId}/kan-ta-av-vent")
    fun kanTaAvVent(@PathVariable behandlingId: UUID): Ressurs<TaAvVentStatusDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(behandlingPåVentService.kanTaAvVent(behandlingId))
    }

    @PostMapping("{behandlingId}/aktiver")
    fun taAvVent(@PathVariable behandlingId: UUID): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        behandlingPåVentService.taAvVent(behandlingId)
        return Ressurs.success(behandlingId)
    }

    @PostMapping("{behandlingId}/henlegg")
    fun henleggBehandling(@PathVariable behandlingId: UUID, @RequestBody henlagt: HenlagtDto): Ressurs<BehandlingDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        val henlagtBehandling = henleggService.henleggBehandling(behandlingId, henlagt)
        val fagsak: Fagsak = fagsakService.hentFagsak(henlagtBehandling.fagsakId)
        return Ressurs.success(henlagtBehandling.tilDto(fagsak.stønadstype))
    }

    @GetMapping("/ekstern/{eksternBehandlingId}")
    fun hentBehandling(@PathVariable eksternBehandlingId: Long): Ressurs<BehandlingDto> {
        val saksbehandling = behandlingService.hentSaksbehandling(eksternBehandlingId)
        tilgangService.validerTilgangTilPersonMedBarn(saksbehandling.ident, AuditLoggerEvent.ACCESS)
        return Ressurs.success(saksbehandling.tilDto())
    }

    @GetMapping("/gjenbruk/{behandlingId}")
    fun hentBehandlingForGjenbrukAvVilkår(@PathVariable behandlingId: UUID): Ressurs<List<BehandlingDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(gjenbrukVilkårService.finnBehandlingerForGjenbruk(behandlingId))
    }
}
