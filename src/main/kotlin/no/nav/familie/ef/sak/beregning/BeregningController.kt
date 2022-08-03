package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.tilBeløpsperiode
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import no.nav.familie.kontrakter.felles.Periode
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/beregning"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BeregningController(
    private val beregningService: BeregningService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService
) {

    @PostMapping
    fun beregnYtelserForRequest(@RequestBody beregningRequest: BeregningRequest): Ressurs<List<Beløpsperiode>> {
        val vedtaksperioder: List<Periode> = beregningRequest.vedtaksperioder
            .filter { it.periodeType != VedtaksperiodeType.MIDLERTIDIG_OPPHØR }
            .tilPerioder()

        val inntektsperioder = beregningRequest.inntekt.tilInntektsperioder()
        return Ressurs.success(beregningService.beregnYtelse(vedtaksperioder, inntektsperioder))
    }

    @GetMapping("/{behandlingId}")
    fun hentBeregnetBeløp(@PathVariable behandlingId: UUID): Ressurs<List<Beløpsperiode>> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        val vedtakForBehandling = vedtakService.hentVedtak(behandlingId)
        if (vedtakForBehandling.resultatType === ResultatType.OPPHØRT) {
            throw Feil("Kan ikke vise fremtidige beløpsperioder for opphørt vedtak med id=$behandlingId")
        }
        val startDatoForVedtak = vedtakForBehandling.perioder?.perioder?.minByOrNull { it.datoFra }?.datoFra
            ?: error("Fant ingen startdato for vedtak på behandling med id=$behandlingId")
        return Ressurs.success(tilkjentYtelseService.hentForBehandling(behandlingId).tilBeløpsperiode(startDatoForVedtak))
    }
}
