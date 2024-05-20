package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/vedtak"],
)
class EksternVedtakController(
    private val tilgangService: TilgangService,
    private val eksternVedtakService: EksternVedtakService,
) {
    @GetMapping("/{eksternFagsakId}")
    @ProtectedWithClaims(issuer = "azuread")
    fun hentVedtak(
        @PathVariable eksternFagsakId: Long,
    ): Ressurs<List<FagsystemVedtak>> {
        if (!SikkerhetContext.erMaskinTilMaskinToken()) {
            tilgangService.validerTilgangTilEksternFagsak(eksternFagsakId, AuditLoggerEvent.ACCESS)
        }

        return Ressurs.success(eksternVedtakService.hentVedtak(eksternFagsakId))
    }
}
