package no.nav.familie.ef.sak.amelding

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.util.UUID

@RestController
@RequestMapping("/api/inntekt")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class InntektController(
        private val tilgangService: TilgangService,
        private val inntektService: InntektService
) {

    @GetMapping("fagsak/{fagsakId}")
    fun hentInntekt(@PathVariable("fagsakId") fagsakId: UUID,
                    @RequestParam fom: YearMonth?,
                    @RequestParam tom: YearMonth?): Ressurs<AMeldingInntektDto> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        val inntekt = inntektService.hentInntekt(fagsakId = fagsakId,
                                                 fom = fom ?: YearMonth.now().minusMonths(2),
                                                 tom = tom ?: YearMonth.now())
        return success(inntekt)
    }

    @GetMapping("fagsak/{fagsakId}/generer-url")
    fun genererAInntektUrl(@PathVariable("fagsakId") fagsakId: UUID): Ressurs<String> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        return success(inntektService.genererAInntektUrl(fagsakId))
    }
}
