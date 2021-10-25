package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.brev.MellomlagringBrevService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.taMedAndelerFremTilDato
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class BeregnYtelseSteg(private val tilkjentYtelseService: TilkjentYtelseService,
                       private val behandlingService: BehandlingService,
                       private val beregningService: BeregningService,
                       private val simuleringService: SimuleringService,
                       private val vedtakService: VedtakService,
                       private val mellomlagringBrevService: MellomlagringBrevService,
                       private val tilbakekrevingService: TilbakekrevingService) : BehandlingSteg<VedtakDto> {


    override fun validerSteg(behandling: Behandling) {

    }

    override fun stegType(): StegType {
        return StegType.BEREGNE_YTELSE
    }

    override fun utførSteg(behandling: Behandling, vedtak: VedtakDto) {
        val aktivIdent = behandlingService.hentAktivIdent(behandling.id)
        nullstillEksisterendeVedtakPåBehandling(behandling.id)
        vedtakService.lagreVedtak(vedtakDto = vedtak, behandlingId = behandling.id)

        when (vedtak) {
            is Innvilget -> {
                opprettTilkjentYtelseForInnvilgetBehandling(vedtak, behandling, aktivIdent)
                simuleringService.hentOgLagreSimuleringsresultat(behandling)
            }
            is Opphør -> {
                opprettTilkjentYtelseForOpphørtBehandling(behandling, vedtak, aktivIdent)
                simuleringService.hentOgLagreSimuleringsresultat(behandling)
            }
            is Avslå -> {
                simuleringService.slettSimuleringForBehandling(behandling.id)
                tilbakekrevingService.slettTilbakekreving(behandling.id)
            }
            else -> error("Kan ikke utføre steg ${stegType()} for behandling ${behandling.id}")
        }
    }

    private fun nullstillEksisterendeVedtakPåBehandling(behandlingId: UUID) {
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(behandlingId)
        mellomlagringBrevService.slettMellomlagringHvisFinnes(behandlingId)
        vedtakService.slettVedtakHvisFinnes(behandlingId)
    }

    private fun opprettTilkjentYtelseForOpphørtBehandling(behandling: Behandling,
                                                          vedtak: Opphør,
                                                          aktivIdent: String) {
        feilHvis(behandling.type != BehandlingType.REVURDERING) { "Kan kun opphøre ved revurdering" }
        val nyeAndeler = andelerForOpphør(behandling, vedtak.opphørFom.atDay(1))
        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelse(personident = aktivIdent,
                                                                   behandlingId = behandling.id,
                                                                   andelerTilkjentYtelse = nyeAndeler))
    }

    private fun opprettTilkjentYtelseForInnvilgetBehandling(vedtak: Innvilget,
                                                            behandling: Behandling,
                                                            aktivIdent: String) {
        val beløpsperioder = lagBeløpsperioderForInnvilgetVedtak(vedtak, behandling, aktivIdent)
        feilHvis(beløpsperioder.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }

        val nyeAndeler = when (behandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> beløpsperioder
            BehandlingType.REVURDERING -> andelerForInnvilgetRevurdering(behandling, beløpsperioder)
            else -> error("Steg ikke støttet for type=${behandling.type}")
        }

        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelse(personident = aktivIdent,
                                                                   behandlingId = behandling.id,
                                                                   andelerTilkjentYtelse = nyeAndeler))
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

    private fun andelerForInnvilgetRevurdering(behandling: Behandling,
                                               beløpsperioder: List<AndelTilkjentYtelse>): List<AndelTilkjentYtelse> {
        return behandling.forrigeBehandlingId?.let {
            val forrigeTilkjenteYtelse = hentForrigeTilkjenteYtelse(behandling)
            return slåSammenAndelerSomSkalVidereføres(beløpsperioder, forrigeTilkjenteYtelse)
        } ?: beløpsperioder
    }

    fun slåSammenAndelerSomSkalVidereføres(beløpsperioder: List<AndelTilkjentYtelse>,
                                           forrigeTilkjentYtelse: TilkjentYtelse): List<AndelTilkjentYtelse> {
        val fom = beløpsperioder.first().stønadFom
        return forrigeTilkjentYtelse.taMedAndelerFremTilDato(fom) + beløpsperioder
    }

    private fun andelerForOpphør(behandling: Behandling, opphørFom: LocalDate): List<AndelTilkjentYtelse> {
        val forrigeTilkjenteYtelse = hentForrigeTilkjenteYtelse(behandling)

        feilHvis(forrigeTilkjenteYtelse.andelerTilkjentYtelse.none { andel ->
            andel.stønadFom <= opphørFom && andel.stønadTom >= opphørFom
        }) { "Opphørsdato sammenfaller ikke med løpende vedtaksperioder" }

        return forrigeTilkjenteYtelse.taMedAndelerFremTilDato(opphørFom)
    }

    private fun hentForrigeTilkjenteYtelse(behandling: Behandling): TilkjentYtelse {
        val forrigeBehandlingId = behandling.forrigeBehandlingId
                                  ?: error("Finner ikke forrige behandling til behandling=${behandling.id}")
        return tilkjentYtelseService.hentForBehandling(forrigeBehandlingId)
    }

}