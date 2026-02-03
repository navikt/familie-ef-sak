package no.nav.familie.ef.sak.sigrun

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.sigrun.ekstern.BeregnetSkatt
import no.nav.familie.ef.sak.sigrun.ekstern.PensjonsgivendeInntektResponse
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import no.nav.familie.ef.sak.sigrun.ekstern.SummertSkattegrunnlag
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/naeringsinntekt")
@ProtectedWithClaims(issuer = "azuread")
class SigrunController(
    private val tilgangService: TilgangService,
    private val sigrunService: SigrunService,
    private val sigrunClient: SigrunClient,
) {
    @GetMapping("fagsak-person/{fagsakPersonId}")
    fun hentPensjonsgivendeInntektForFolketrygden(
        @PathVariable fagsakPersonId: UUID,
    ): Ressurs<List<PensjonsgivendeInntektVisning>> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        val inntektSisteTreÅr = sigrunService.hentInntektForAlleÅrMedInntekt(fagsakPersonId)
        return Ressurs.success(inntektSisteTreÅr)
    }

    // TODO: Skal fjernes, kun for testing
    @GetMapping("test/pensjonsgivende-inntekt")
    fun testPensjonsgivendeInntekt(
        @RequestParam personident: String,
        @RequestParam inntektsaar: Int,
    ): Ressurs<PensjonsgivendeInntektResponse?> {
        tilgangService.validerHarForvalterrolle()
        return Ressurs.success(sigrunClient.hentPensjonsgivendeInntekt(personident, inntektsaar))
    }

    // TODO: Skal fjernes, kun for testing
    @GetMapping("test/summert-skattegrunnlag")
    fun testSummertSkattegrunnlag(
        @RequestParam personident: String,
        @RequestParam inntektsaar: Int,
    ): Ressurs<SummertSkattegrunnlag?> {
        tilgangService.validerHarForvalterrolle()
        return Ressurs.success(sigrunClient.hentSummertSkattegrunnlag(personident, inntektsaar))
    }

    // TODO: Skal fjernes, kun for testing
    @GetMapping("test/beregnet-skatt")
    fun testBeregnetSkatt(
        @RequestParam personident: String,
        @RequestParam inntektsaar: Int,
    ): Ressurs<List<BeregnetSkatt>> {
        tilgangService.validerHarForvalterrolle()
        return Ressurs.success(sigrunClient.hentBeregnetSkatt(personident, inntektsaar))
    }
}
