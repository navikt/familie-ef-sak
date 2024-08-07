package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.VedtakSkolepengerDto
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
@RequestMapping(path = ["/api/beregning/skolepenger"])
@ProtectedWithClaims(issuer = "azuread")
class BeregningSkolepengerController(
    private val beregningSkolepengerService: BeregningSkolepengerService,
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService,
) {
    @PostMapping
    fun beregnYtelse(
        @RequestBody request: BeregningSkolepengerRequest,
    ): Ressurs<BeregningSkolepengerResponse> =
        Ressurs.success(
            beregningSkolepengerService.beregnYtelse(
                request.skoleårsperioder,
                request.behandlingId,
                request.erOpphør,
            ),
        )

    @GetMapping("/{behandlingId}")
    fun hentBeregning(
        @PathVariable behandlingId: UUID,
    ): Ressurs<BeregningSkolepengerResponse> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val vedtak = vedtakService.hentVedtakDto(behandlingId)

        if (vedtak is VedtakSkolepengerDto) {
            // TODO vi kaller ikke beregning for de andre, men der har vi en enklere oppdeling av hvilke perioder som gir X beløp
            return Ressurs.Companion.success(
                beregningSkolepengerService.beregnYtelse(
                    vedtak.skoleårsperioder,
                    behandlingId,
                    vedtak.erOpphør(),
                ),
            )
        }
        error("Kan ikke hente beregning for vedtakstype ${vedtak._type}")
    }
}
