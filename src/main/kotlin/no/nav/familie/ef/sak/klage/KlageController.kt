package no.nav.familie.ef.sak.klage

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.klage.dto.OpprettKlageDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/klage"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class KlageController(
    private val tilgangService: TilgangService,
    private val klageService: KlageService
) {

    @PostMapping("/{behandlingId}")
    fun opprettKlage(@PathVariable behandlingId: UUID, @RequestBody opprettKlageDto: OpprettKlageDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()
        klageService.opprettKlage(behandlingId, opprettKlageDto)
        return Ressurs.success(behandlingId)
    }

}
