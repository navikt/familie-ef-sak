package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.beregning.Avslå
import no.nav.familie.ef.sak.api.beregning.BeregningService
import no.nav.familie.ef.sak.api.beregning.Innvilget
import no.nav.familie.ef.sak.api.beregning.Opphør
import no.nav.familie.ef.sak.api.beregning.VedtakDto
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.api.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.api.beregning.tilPerioder
import no.nav.familie.ef.sak.api.feilHvis
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.simulering.SimuleringService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

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
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(behandling.id)

        when (vedtak) {
            is Innvilget -> {
                val beløpsperioder = lagBeløpsperioderForInnvilgetVedtak(vedtak, behandling, aktivIdent)
                feilHvis(beløpsperioder.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }

                val nyeAndeler = when (behandling.type) {
                    BehandlingType.FØRSTEGANGSBEHANDLING -> beløpsperioder
                    BehandlingType.REVURDERING -> andelerForRevurdering(behandling, beløpsperioder)
                    else -> error("Steg ikke støttet for type=${behandling.type}")
                }

                tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelse(personident = aktivIdent,
                                                                           behandlingId = behandling.id,
                                                                           andelerTilkjentYtelse = nyeAndeler))
            }
            is Opphør -> {
                feilHvis(behandling.type != BehandlingType.REVURDERING) { "Kan kun opphøre ved revurdering" }
                val nyeAndeler = andelerForOpphør(behandling, vedtak.opphørFom.atDay(1))
                tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelse(personident = aktivIdent,
                                                                           behandlingId = behandling.id,
                                                                           andelerTilkjentYtelse = nyeAndeler))
            }
            is Avslå -> {
                feilHvis(behandling.type != BehandlingType.FØRSTEGANGSBEHANDLING) { "Kan kun avslå ved førstegangsbehandling" }
            }
            else -> {
            }
        }

        vedtakService.slettVedtakHvisFinnes(behandling.id)
        vedtakService.lagreVedtak(vedtakDto = vedtak, behandlingId = behandling.id)
        simuleringService.hentOgLagreSimuleringsresultat(behandling)
    }

    private fun lagBeløpsperioderForInnvilgetVedtak(vedtak: Innvilget,
                                                    behandling: Behandling,
                                                    aktivIdent: String) =
            beregningService.beregnYtelse(vedtak.perioder.tilPerioder(), vedtak.inntekter.tilInntektsperioder())
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

    private fun andelerForRevurdering(behandling: Behandling,
                                      beløpsperioder: List<AndelTilkjentYtelse>): List<AndelTilkjentYtelse> {
        val forrigeAndeler = hentForrigeTilkjenteYtelse(behandling)

        return slåSammenAndelerSomSkalVidereføres(beløpsperioder, forrigeAndeler)
    }

    private fun andelerForOpphør(behandling: Behandling, opphørFom: LocalDate): List<AndelTilkjentYtelse> {
        val forrigeAndeler = hentForrigeTilkjenteYtelse(behandling)

        feilHvis(forrigeAndeler.andelerTilkjentYtelse.none { andel ->
            andel.stønadFom <= opphørFom && andel.stønadTom >= opphørFom
        }) { "Opphørsdato sammenfaller ikke med løpende vedtaksperioder" }

        return taMedAndelerFremTilDato(forrigeAndeler, opphørFom)
    }

    private fun hentForrigeTilkjenteYtelse(behandling: Behandling): TilkjentYtelse {
        val forrigeBehandlingId = behandlingService.finnSisteIverksatteBehandling(behandling.fagsakId)
                                  ?: error("Finner ikke forrige behandling til behandling=${behandling.id}")
        val forrigeAndeler = tilkjentYtelseService.hentForBehandling(forrigeBehandlingId)
        return forrigeAndeler
    }

    fun slåSammenAndelerSomSkalVidereføres(beløpsperioder: List<AndelTilkjentYtelse>,
                                           forrigeTilkjentYtelse: TilkjentYtelse): List<AndelTilkjentYtelse> {
        val fom = beløpsperioder.first().stønadFom
        return taMedAndelerFremTilDato(forrigeTilkjentYtelse, fom) + beløpsperioder
    }

    private fun taMedAndelerFremTilDato(forrigeTilkjentYtelse: TilkjentYtelse,
                                        fom: LocalDate) = forrigeTilkjentYtelse.andelerTilkjentYtelse
            .filter { andel -> andel.stønadTom < fom || (erStønadOverlappende(andel, fom)) }
            .map { andel ->
                if (erStønadOverlappende(andel, fom)) {
                    andel.copy(stønadTom = fom.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()))
                } else {
                    andel
                }
            }

    private fun erStønadOverlappende(andel: AndelTilkjentYtelse, fom: LocalDate) =
            andel.stønadFom < fom && andel.stønadTom >= fom

}