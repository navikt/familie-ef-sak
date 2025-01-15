package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
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
@RequestMapping(path = ["/api/revurdering"])
@ProtectedWithClaims(issuer = "azuread")
class RevurderingController(
    private val revurderingService: RevurderingService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("{fagsakId}")
    fun startRevurdering(
        @RequestBody revurderingInnhold: RevurderingDto,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilFagsak(revurderingInnhold.fagsakId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()
        brukerfeilHvis(revurderingInnhold.behandlingsårsak == BehandlingÅrsak.SØKNAD) {
            "Systemet har ikke støtte for å revurdere med årsak “Søknad”. " +
                "Vurder om behandlingen skal opprettes via en oppgave i oppgavebenken, " +
                "eller med revurderingsårsak \"Nye opplysninger\". " +
                "Hvis du trenger å \"flytte\" en søknad som er journalført mot infotrygd, kontakt superbrukere for flytting av journalpost"
        }
        feilHvis(
            revurderingInnhold.behandlingsårsak == BehandlingÅrsak.G_OMREGNING &&
                revurderingInnhold.vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE,
        ) {
            "Kan ikke behandle nye barn på revurdering med årsak G-omregning"
        }

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingInnhold)
        return Ressurs.success(revurdering.id)
    }

    @PostMapping("informasjon/{behandlingId}")
    fun lagreRevurderingsinformasjon(
        @PathVariable behandlingId: UUID,
        @RequestBody revurderingsinformasjonDto: RevurderingsinformasjonDto,
    ): Ressurs<RevurderingsinformasjonDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()

        val oppdatertRevurderingsinformasjon =
            revurderingService.lagreRevurderingsinformasjon(behandlingId, revurderingsinformasjonDto)
        return Ressurs.success(oppdatertRevurderingsinformasjon)
    }

    @DeleteMapping("informasjon/{behandlingId}")
    fun slettRevurderingsinformasjon(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()

        revurderingService.slettRevurderingsinformasjon(behandlingId)

        return Ressurs.success("Slettet")
    }

    @GetMapping("informasjon/{behandlingId}")
    fun hentRevurderingsinformasjon(
        @PathVariable behandlingId: UUID,
    ): Ressurs<RevurderingsinformasjonDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(revurderingService.hentRevurderingsinformasjon(behandlingId))
    }
}
