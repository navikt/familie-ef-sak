package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.tilkjentytelse.tilBeløpsperiode
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
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
class BeregningController(private val stegService: StegService,
                          private val behandlingService: BehandlingService,
                          private val beregningService: BeregningService,
                          private val tilkjentYtelseService: TilkjentYtelseService,
                          private val tilgangService: TilgangService) {

    @PostMapping
    fun beregnYtelserForRequest(@RequestBody beregningRequest: BeregningRequest): Ressurs<List<Beløpsperiode>> {
        val vedtaksperioder = beregningRequest.vedtaksperioder.map(ÅrMånedPeriode::tilPerioder)
        val inntektsperioder = beregningRequest.inntekt.tilInntektsperioder()
        return Ressurs.success(beregningService.beregnYtelse(vedtaksperioder, inntektsperioder))
    }

    @PostMapping("/{behandlingId}/fullfor")
    fun lagreVedtak(@PathVariable behandlingId: UUID, @RequestBody vedtak: VedtakDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        return Ressurs.success(stegService.håndterBeregnYtelseForStønad(behandling, vedtak).id)
    }

    @PostMapping(value = ["/{behandlingId}/lagre-vedtak", "/{behandlingId}/lagre-blankettvedtak"])
    fun lagreBlankettVedtak(@PathVariable behandlingId: UUID, @RequestBody vedtak: VedtakDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)

        return Ressurs.success(stegService.håndterVedtaBlankett(behandling, vedtak).id)
    }

    @GetMapping("/{behandlingId}")
    fun hentBeregnetBeløp(@PathVariable behandlingId: UUID): Ressurs<List<Beløpsperiode>> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(tilkjentYtelseService.hentForBehandling(behandlingId).tilBeløpsperiode())
    }

}
