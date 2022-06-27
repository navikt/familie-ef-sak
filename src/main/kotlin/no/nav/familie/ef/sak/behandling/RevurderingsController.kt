package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/revurdering"])
@ProtectedWithClaims(issuer = "azuread")
class RevurderingsController(
    private val revurderingService: RevurderingService,
    private val tilgangService: TilgangService
) {

    @PostMapping("{fagsakId}")
    fun startRevurdering(@RequestBody revurderingInnhold: RevurderingDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilFagsak(revurderingInnhold.fagsakId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()
        brukerfeilHvis(revurderingInnhold.behandlingsårsak == BehandlingÅrsak.SØKNAD) {
            "Systemet har ikke støtte for å revurdere med årsak “Søknad” for øyeblikket. " +
                "Vurder om behandlingen skal opprettes via en oppgave i oppgavebenken, " +
                "eller med revurderingsårsak \"Nye opplysninger\". " +
                "Hvis du trenger å \"flytte\" en søknad som er journalført mot infotrygd, kontakt superbrukere for flytting av journalpost"
        }
        brukerfeilHvis(
            revurderingInnhold.behandlingsårsak == BehandlingÅrsak.G_OMREGNING &&
                revurderingInnhold.barn.isNotEmpty()
        ) {
            "Kan ikke sende inn nye barn på revurdering med årsak G-omregning"
        }
        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingInnhold)
        return Ressurs.success(revurdering.id)
    }
}
