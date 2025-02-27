package no.nav.familie.ef.sak.amelding

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
    private val inntektService: InntektService,
) {
    @GetMapping("fagsak/{fagsakId}")
    fun hentInntekt(
        @PathVariable("fagsakId") fagsakId: UUID,
        @RequestParam fom: YearMonth?,
        @RequestParam tom: YearMonth?,
    ): Ressurs<AMeldingInntektDto> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        val inntekt =
            inntektService.hentInntekt(
                fagsakId = fagsakId,
                fom = fom ?: YearMonth.now().minusMonths(2),
                tom = tom ?: YearMonth.now(),
            )
        return success(inntekt)
    }

    data class Body (
        val maanedFom: YearMonth?,
        val maanedTom: YearMonth?,
    )

    // TODO: Husk å endre retur type fra Any til det som gjelder.
    @PostMapping("inntektv2/{fagsakid}")
    fun hentInntektV2(
        @PathVariable("fagsakid") fagsakId: UUID,
        @RequestBody body: Body,
    ): Ressurs<Any> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        val inntekt =
            inntektService.hentInntektV2(
                fagsakId = fagsakId,
                fom = body.maanedFom ?: YearMonth.now().minusMonths(2),
                tom = body.maanedTom ?: YearMonth.now(),
            )

        // TODO: Husk å fjern meg.
        val logger = LoggerFactory.getLogger(::javaClass.name)
        logger.info("FAMILIE-EF-SAK --- inntekt for personident med data: $inntekt")

        return success(inntekt)
    }

    @GetMapping("inntektv2-get/{fagsakid}")
    fun getHentInntektV2(
        @PathVariable("fagsakId") fagsakId: UUID,
        @RequestParam maanedFom: YearMonth?,
        @RequestParam maanedTom: YearMonth?,
    ): Ressurs<Any> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        val inntekt =
            inntektService.hentInntektV2(
                fagsakId = fagsakId,
                fom = maanedFom ?: YearMonth.now().minusMonths(2),
                tom = maanedTom ?: YearMonth.now(),
            )

        // TODO: Husk å fjern meg.
        val logger = LoggerFactory.getLogger(::javaClass.name)
        logger.info("FAMILIE-EF-SAK - GET --- inntekt for personident med data: $inntekt")

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
