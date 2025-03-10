package no.nav.familie.ef.sak.amelding

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.util.UUID

@RestController
@RequestMapping("/api/inntekt")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class InntektController(
    private val tilgangService: TilgangService,
    private val inntektService: InntektService,
) {
    @PostMapping("inntektv2/{fagsakid}")
    fun hentInntekt(
        @PathVariable("fagsakid") fagsakId: UUID,
        @RequestBody inntektRequestBody: InntektRequestBody,
    ): Ressurs<List<InntektMåned>> {
        tilgangService.validerTilgangTilFagsak(
            fagsakId = fagsakId,
            event = AuditLoggerEvent.ACCESS,
        )

        val inntekt =
            inntektService.hentInntektV2(
                fagsakId = fagsakId,
                fom = inntektRequestBody.månedFom ?: YearMonth.now().minusMonths(2),
                tom = inntektRequestBody.månedTom ?: YearMonth.now(),
            )

        return success(inntekt)
    }

    @GetMapping("fagsak/{fagsakId}/generer-url")
    fun genererAInntektUrlFagsak(
        @PathVariable("fagsakId") fagsakId: UUID,
    ): Ressurs<String> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        return success(inntektService.genererAInntektUrlFagsak(fagsakId))
    }

    @GetMapping("fagsak-person/{fagsakPersonId}/generer-url")
    fun genererAInntektUrl(
        @PathVariable("fagsakPersonId") fagsakPersonId: UUID,
    ): Ressurs<String> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return success(inntektService.genererAInntektUrl(fagsakPersonId))
    }

    @GetMapping("fagsak/{fagsakId}/generer-url-arbeidsforhold")
    fun genererAInntektArbeidsforholdUrl(
        @PathVariable("fagsakId") fagsakId: UUID,
    ): Ressurs<String> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        return success(inntektService.genererAInntektArbeidsforholdUrl(fagsakId))
    }
}
