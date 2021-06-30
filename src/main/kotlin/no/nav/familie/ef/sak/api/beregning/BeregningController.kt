package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID


@RestController
@RequestMapping(path = ["/api/beregning"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BeregningController(private val stegService: StegService,
                          private val behandlingService: BehandlingService,
                          private val beregningService: BeregningService,
                          private val tilgangService: TilgangService) {

    @PostMapping
    fun beregnYtelserForRequest(@RequestBody beregningRequest: BeregningRequest): Ressurs<List<Beløpsperiode>> {
        val vedtaksperioder = beregningRequest.vedtaksperioder.map(ÅrMånedPeriode::tilPerioder)
        val inntektsperioder = beregningRequest.inntekt.tilInntektsperioder()
        return Ressurs.success(beregningService.beregnYtelse(vedtaksperioder, inntektsperioder))
    }

    @PostMapping("/{behandlingId}/fullfor")
    fun beregnYtelseForStønad(@PathVariable behandlingId: UUID, @RequestBody vedtak: VedtakDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        return Ressurs.success(stegService.håndterBeregnYtelseForStønad(behandling, vedtak).id)
    }

    @PostMapping("/{behandlingId}/lagre-vedtak")
    fun lagreVedtak(@PathVariable behandlingId: UUID, @RequestBody vedtak: VedtakDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)

        return Ressurs.success(stegService.håndterVedtaBlankett(behandling, vedtak).id)
    }

}
