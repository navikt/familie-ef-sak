package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*


@RestController
@RequestMapping(path = ["/api/beregning"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BeregningController(private val stegService: StegService,
                          private val behandlingService: BehandlingService,
                          private val fagsakService: FagsakService,
                          private val beregningService: BeregningService,
                          private val tilgangService: TilgangService) {

    @PostMapping("/{behandlingId}/fullfor")
    fun beregnYtelseForStønad(@PathVariable behandlingId: UUID, @RequestBody beregningRequest: BeregningRequest): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val beløpsperioder = beregningService.beregnFullYtelse(beregningRequest) // TODO: Tar ikke høyde for inntekt
        val tilkjentYtelse = TilkjentYtelseDTO(
                fagsak.hentAktivIdent(),
                vedtaksdato = LocalDate.now(),
                behandlingId = behandling.id,
                andelerTilkjentYtelse = beløpsperioder.map {
                    AndelTilkjentYtelseDTO(beløp = it.beløp.toInt(),
                                           stønadFom = it.fraOgMedDato,
                                           stønadTom = it.tilDato,
                                           kildeBehandlingId = behandling.id,
                                           personIdent = fagsak.hentAktivIdent())
                }
        )
        return Ressurs.success(stegService.håndterBeregnYtelseForStønad(behandling, tilkjentYtelse).id)
    }

    @PostMapping("/{behandlingId}/lagre-vedtak")
    fun lagreVedtak(@PathVariable behandlingId: UUID, @RequestBody vedtak: VedtakDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)

        return Ressurs.success(stegService.håndterVedtaBlankett(behandling, vedtak).id)
    }

}
