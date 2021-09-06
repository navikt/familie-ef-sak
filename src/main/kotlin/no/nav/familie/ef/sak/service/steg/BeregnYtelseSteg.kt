package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.beregning.BeregningService
import no.nav.familie.ef.sak.api.beregning.Innvilget
import no.nav.familie.ef.sak.api.beregning.VedtakDto
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.api.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.api.beregning.tilPerioder
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.simulering.SimuleringService
import org.springframework.stereotype.Service

@Service
class BeregnYtelseSteg(private val tilkjentYtelseService: TilkjentYtelseService,
                       private val behandlingService: BehandlingService,
                       private val beregningService: BeregningService,
                       private val simuleringService: SimuleringService,
                       private val vedtakService: VedtakService) : BehandlingSteg<VedtakDto> {

    override fun validerSteg(behandling: Behandling) {

    }

    override fun stegType(): StegType {
        return StegType.BEREGNE_YTELSE
    }

    override fun utførSteg(behandling: Behandling, vedtak: VedtakDto) {
        val aktivIdent = behandlingService.hentAktivIdent(behandling.id)
        val beløpsperioder = when (vedtak) {
            is Innvilget -> {
                beregningService.beregnYtelse(
                        vedtak.perioder.tilPerioder(),
                        vedtak.inntekter.tilInntektsperioder()
                )
                        .map { beløpsperiode ->
                            AndelTilkjentYtelse(
                                    beløp = beløpsperiode.beløp.toInt(),
                                    stønadFom = beløpsperiode.periode.fradato,
                                    stønadTom = beløpsperiode.periode.tildato,
                                    kildeBehandlingId = behandling.id,
                                    personIdent = aktivIdent,
                                    samordningsfradrag = beløpsperiode.beregningsgrunnlag?.samordningsfradrag?.toInt() ?: 0,
                                    inntekt = beløpsperiode.beregningsgrunnlag?.inntekt?.toInt() ?: 0,
                                    inntektsreduksjon = beløpsperiode.beregningsgrunnlag?.avkortningPerMåned?.toInt() ?: 0,
                            )
                        }
            }
            else -> emptyList()
        }

        // TODO: Hent tilkjentYtelse fra forrige behandling og gjør diff med ny og ta vare på kildeBehandlingId
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(behandling.id)
        if (beløpsperioder.isNotEmpty()) {
            tilkjentYtelseService.opprettTilkjentYtelse(
                    TilkjentYtelse(
                            personident = aktivIdent,
                            behandlingId = behandling.id,
                            andelerTilkjentYtelse = beløpsperioder
                    )
            )
        }
        vedtakService.slettVedtakHvisFinnes(behandling.id)
        vedtakService.lagreVedtak(vedtakDto = vedtak, behandlingId = behandling.id)
        simuleringService.hentOgLagreSimuleringsresultat(behandling)
    }

}