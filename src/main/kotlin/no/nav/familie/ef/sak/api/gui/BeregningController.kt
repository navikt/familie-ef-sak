package no.nav.familie.ef.sak.api.gui

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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.*


@RestController
@RequestMapping(path = ["/api/beregning"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BeregningController(private val stegService: StegService,
                          private val behandlingService: BehandlingService,
                          private val fagsakService: FagsakService,
                          private val tilgangService: TilgangService) {

    @PostMapping("/{behandlingId}/fullfor")
    fun beregnYtelseForStønad(@PathVariable behandlingId: UUID): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val tilkjentYtelse = TilkjentYtelseDTO(
                fagsak.hentAktivIdent(),
                vedtaksdato = LocalDate.now(),
                behandlingId = behandling.id,
                andelerTilkjentYtelse = listOf(AndelTilkjentYtelseDTO(beløp = 100,
                                                                      stønadFom = LocalDate.now(),
                                                                      stønadTom = LocalDate.now().plusDays(100),
                                                                      kildeBehandlingId = behandling.id,
                                                                      personIdent = fagsak.hentAktivIdent()))
        )
        return Ressurs.success(stegService.håndterBeregnYtelseForStønad(behandling, tilkjentYtelse).id)
    }

}
