package no.nav.familie.ef.sak.sigrun

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/naeringsinntekt")
@ProtectedWithClaims(issuer = "azuread")
class SigrunController(
    private val tilgangService: TilgangService,
    private val sigrunService: SigrunService,
) {
    @GetMapping("fagsak-person/{fagsakPersonId}")
    fun hentPensjonsgivendeInntektForFolketrygden(
        @PathVariable fagsakPersonId: UUID,
    ): Ressurs<List<PensjonsgivendeInntektVisning>> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        val inntektSisteTreÅr = sigrunService.hentInntektForAlleÅrMedInntekt(fagsakPersonId)
        return Ressurs.success(inntektSisteTreÅr)
    }

    // TODO: Fjern
    @GetMapping("fagsak-person/{fagsakPersonId}/aar/{inntektsaar}")
    fun hentPensjonsgivendeInntektForÅr(
        @PathVariable fagsakPersonId: UUID,
        @PathVariable inntektsaar: Int,
    ): Ressurs<PensjonsgivendeInntektVisning> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        val inntekt = sigrunService.hentInntektForÅr(fagsakPersonId, inntektsaar)
        return Ressurs.success(inntekt)
    }
}
