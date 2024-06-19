package no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.tilBeløpsperiodeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
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
@RequestMapping(path = ["/api/beregning/barnetilsyn"])
@ProtectedWithClaims(issuer = "azuread")
class BeregningBarnetilsynController(
    private val beregningBarnetilsynService: BeregningBarnetilsynService,
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val featureToggleService: FeatureToggleService,
) {
    @PostMapping
    fun beregnYtelserForBarnetilsyn(
        @RequestBody
        barnetilsynBeregningRequest: BeregningBarnetilsynRequest,
    ): Ressurs<List<BeløpsperiodeBarnetilsynDto>> =
        Ressurs.success(
            beregningBarnetilsynService.beregnYtelseBarnetilsyn(
                barnetilsynBeregningRequest.utgiftsperioder,
                barnetilsynBeregningRequest.kontantstøtteperioder,
                barnetilsynBeregningRequest.tilleggsstønadsperioder,
            ),
        )

    @GetMapping("/{behandlingId}")
    fun hentBeregning(
        @PathVariable behandlingId: UUID,
    ): Ressurs<List<BeløpsperiodeBarnetilsynDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        val vedtak = vedtakService.hentVedtakDto(behandlingId)

        if (vedtak is InnvilgelseBarnetilsyn) {
            val brukIkkeVedtatteSatser = featureToggleService.isEnabled(Toggle.SATSENDRING_BRUK_IKKE_VEDTATT_MAXSATS)

            return Ressurs.success(
                tilkjentYtelseService.hentForBehandling(behandlingId).tilBeløpsperiodeBarnetilsyn(
                    vedtak,
                    brukIkkeVedtatteSatser,
                ),
            )
        }
        error("Kan ikke hente beregning for vedtakstype ${vedtak._type}")
    }
}
