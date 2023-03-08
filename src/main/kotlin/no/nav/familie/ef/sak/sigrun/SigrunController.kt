package no.nav.familie.ef.sak.sigrun

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/naeringsinntekt")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SigrunController(
    private val tilgangService: TilgangService,
    private val sigrunService: SigrunService
) {

    @GetMapping("fagsak-person/{fagsakPersonId}")
    fun hentBeregnetSkatt(@PathVariable fagsakPersonId: UUID): Ressurs<List<PensjonsgivendeInntektVisning>> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        val inntektSisteTreÅr = sigrunService.hentInntektSisteTreÅr(fagsakPersonId)
        return Ressurs.success(inntektSisteTreÅr)
    }
}
