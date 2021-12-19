package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.behandling.domain.Brevmottakere
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/brevmottakere/"])
@ProtectedWithClaims(issuer = "azuread")
class BrevmottakereController(private val tilgangService: TilgangService,
                              private val brevmottakereService: BrevmottakereService) {

    @GetMapping("/{behandlingId}")
    fun hentBrevmottakere(@PathVariable behandlingId: UUID): Ressurs<BrevmottakereDto?> {
        tilgangService.validerTilgangTilBehandling(behandlingId)

        return Ressurs.Companion.success(brevmottakereService.hentBrevmottakere(behandlingId))
    }

    @PostMapping("/{behandlingId}")
    fun velgBrevmottakere(@PathVariable behandlingId: UUID,
                          @RequestBody brevmottakere: BrevmottakereDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        tilgangService.validerHarSaksbehandlerrolle()

        return Ressurs.Companion.success(brevmottakereService.lagreBrevmottakere(behandlingId, brevmottakere))
    }

}

