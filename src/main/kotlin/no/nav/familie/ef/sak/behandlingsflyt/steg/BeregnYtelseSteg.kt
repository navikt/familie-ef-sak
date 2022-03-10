package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.felles.util.min
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.taMedAndelerFremTilDato
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonert
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.erSammenhengende
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class BeregnYtelseSteg(private val tilkjentYtelseService: TilkjentYtelseService,
                       private val beregningService: BeregningService,
                       private val simuleringService: SimuleringService,
                       private val vedtakService: VedtakService,
                       private val tilbakekrevingService: TilbakekrevingService,
                       private val fagsakService: FagsakService) : BehandlingSteg<VedtakDto> {


    override fun validerSteg(saksbehandling: Saksbehandling) {

    }

    override fun stegType(): StegType {
        return StegType.BEREGNE_YTELSE
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: VedtakDto) {
        validerGyldigeVedtaksperioder(saksbehandling, data)
        val aktivIdent = fagsakService.fagsakMedOppdatertPersonIdent(saksbehandling.fagsakId).hentAktivIdent()
        val saksbehandlingMedOppdatertIdent = saksbehandling.copy(ident = aktivIdent)
        nullstillEksisterendeVedtakPåBehandling(saksbehandlingMedOppdatertIdent.id)
        vedtakService.lagreVedtak(vedtakDto = data, behandlingId = saksbehandlingMedOppdatertIdent.id)

        when (data) {
            is Innvilget -> {
                opprettTilkjentYtelseForInnvilgetBehandling(data, saksbehandlingMedOppdatertIdent)
                simuleringService.hentOgLagreSimuleringsresultat(saksbehandlingMedOppdatertIdent)
            }
            is Opphør -> {
                opprettTilkjentYtelseForOpphørtBehandling(saksbehandlingMedOppdatertIdent, data)
                simuleringService.hentOgLagreSimuleringsresultat(saksbehandlingMedOppdatertIdent)
            }
            is Avslå -> {
                simuleringService.slettSimuleringForBehandling(saksbehandlingMedOppdatertIdent.id)
                tilbakekrevingService.slettTilbakekreving(saksbehandlingMedOppdatertIdent.id)
            }
            is Sanksjonert -> {
                opprettTilkjentYtelseForSanksjonertBehandling(data, saksbehandlingMedOppdatertIdent)
            }
        }
    }

    private fun validerGyldigeVedtaksperioder(saksbehandling: Saksbehandling, data: VedtakDto) {
        if (data is Innvilget) {
            val harOpphørsperioder = data.perioder.any { it.periodeType == VedtaksperiodeType.MIDLERTIDIG_OPPHØR }
            val harInnvilgedePerioder = data.perioder.any { it.periodeType != VedtaksperiodeType.MIDLERTIDIG_OPPHØR }
            brukerfeilHvis(harOpphørsperioder && !harInnvilgedePerioder) {
                "Må ha innvilgelsesperioder i tillegg til opphørsperioder"
            }
            brukerfeilHvis(!saksbehandling.erMigrering() && harPeriodeEllerAktivitetMigrering(data)) {
                "Kan ikke inneholde aktivitet eller periode av type migrering"
            }
        }
    }

    private fun harPeriodeEllerAktivitetMigrering(data: Innvilget) =
            data.perioder.any { it.periodeType == VedtaksperiodeType.MIGRERING || it.aktivitet == AktivitetType.MIGRERING }

    private fun nullstillEksisterendeVedtakPåBehandling(behandlingId: UUID) {
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(behandlingId)
        vedtakService.slettVedtakHvisFinnes(behandlingId)
    }

    private fun opprettTilkjentYtelseForOpphørtBehandling(saksbehandling: Saksbehandling,
                                                          vedtak: Opphør) {
        brukerfeilHvis(saksbehandling.type != BehandlingType.REVURDERING) { "Kan kun opphøre ved revurdering" }
        val opphørsdato = vedtak.opphørFom.atDay(1)
        val forrigeTilkjenteYtelse = hentForrigeTilkjenteYtelse(saksbehandling)
        val nyeAndeler = andelerForOpphør(forrigeTilkjenteYtelse, opphørsdato)
        val nyttOpphørsdato = beregnNyttOpphørsdatoForRevurdering(nyeAndeler, opphørsdato, forrigeTilkjenteYtelse)
        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelse(personident = saksbehandling.ident,
                                                                   behandlingId = saksbehandling.id,
                                                                   andelerTilkjentYtelse = nyeAndeler,
                                                                   samordningsfradragType = null,
                                                                   startdato = nyttOpphørsdato))
    }

    private fun beregnNyttOpphørsdatoForRevurdering(nyeAndeler: List<AndelTilkjentYtelse>,
                                                    opphørsdato: LocalDate?,
                                                    forrigeTilkjenteYtelse: TilkjentYtelse): LocalDate? {
        val opphørsdatoHvisFørTidligereAndeler = if (nyeAndeler.isEmpty()) opphørsdato else null
        return min(opphørsdatoHvisFørTidligereAndeler, forrigeTilkjenteYtelse.startdato)
    }

    private fun opprettTilkjentYtelseForInnvilgetBehandling(vedtak: Innvilget,
                                                            saksbehandling: Saksbehandling) {

        brukerfeilHvis(!vedtak.perioder.erSammenhengende()) { "Periodene må være sammenhengende" }
        val andelerTilkjentYtelse: List<AndelTilkjentYtelse> = lagBeløpsperioderForInnvilgetVedtak(vedtak, saksbehandling)
        brukerfeilHvis(andelerTilkjentYtelse.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }

        val (nyeAndeler, startdato) = when (saksbehandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING ->
                andelerTilkjentYtelse to startdatoForFørstegangsbehandling(andelerTilkjentYtelse)
            BehandlingType.REVURDERING -> nyeAndelerForRevurderingMedStartdato(saksbehandling, vedtak, andelerTilkjentYtelse)
            else -> error("Steg ikke støttet for type=${saksbehandling.type}")
        }


        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelse(personident = saksbehandling.ident,
                                                                   behandlingId = saksbehandling.id,
                                                                   andelerTilkjentYtelse = nyeAndeler,
                                                                   samordningsfradragType = vedtak.samordningsfradragType,
                                                                   startdato = startdato))
    }

    private fun startdatoForFørstegangsbehandling(andelerTilkjentYtelse: List<AndelTilkjentYtelse>): LocalDate {
        return andelerTilkjentYtelse.minOfOrNull { it.stønadFom } ?: error("Må ha med en periode i førstegangsbehandling")
    }

    private fun nyeAndelerForRevurderingMedStartdato(saksbehandling: Saksbehandling,
                                                     vedtak: Innvilget,
                                                     andelerTilkjentYtelse: List<AndelTilkjentYtelse>)
            : Pair<List<AndelTilkjentYtelse>, LocalDate?> {
        val opphørsperioder = finnOpphørsperioder(vedtak)

        val forrigeTilkjenteYtelse = saksbehandling.forrigeBehandlingId?.let { hentForrigeTilkjenteYtelse(saksbehandling) }
        validerStartdato(forrigeTilkjenteYtelse)
        validerOpphørsperioder(opphørsperioder, finnInnvilgedePerioder(vedtak), forrigeTilkjenteYtelse)

        val nyeAndeler = beregnNyeAndelerForRevurdering(forrigeTilkjenteYtelse, andelerTilkjentYtelse, opphørsperioder)

        val forrigeOpphørsdato = forrigeTilkjenteYtelse?.startdato
        val startdato = nyttStartdato(vedtak, forrigeOpphørsdato)
        return nyeAndeler to startdato
    }

    // TODO denne kan fjernes når startdato blir not null
    private fun validerStartdato(forrigeTilkjenteYtelse: TilkjentYtelse?) {
        feilHvis(forrigeTilkjenteYtelse != null && forrigeTilkjenteYtelse.startdato == null) {
            "Mangler startdato på tilkjent ytelse behandlingId=${forrigeTilkjenteYtelse?.behandlingId}"
        }
    }

    private fun validerOpphørsperioder(opphørsperioder: List<Periode>,
                                       vedtaksperioder: List<Periode>,
                                       forrigeTilkjenteYtelse: TilkjentYtelse?) {
        val førsteOpphørsdato = opphørsperioder.minOfOrNull { it.fradato }
        val førsteVedtaksFradato = vedtaksperioder.minOfOrNull { it.fradato }
        val harKunOpphørEllerOpphørFørInnvilgetPeriode =
                førsteOpphørsdato != null && (førsteVedtaksFradato == null || førsteOpphørsdato < førsteVedtaksFradato)
        feilHvis(forrigeTilkjenteYtelse == null && harKunOpphørEllerOpphørFørInnvilgetPeriode) {
            "Har ikke støtte for å innvilge med opphør først, når man mangler tidligere behandling å opphøre"
        }
    }

    private fun beregnNyeAndelerForRevurdering(forrigeTilkjenteYtelse: TilkjentYtelse?,
                                               andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                                               opphørsperioder: List<Periode>) =
            forrigeTilkjenteYtelse?.let {
                slåSammenAndelerSomSkalVidereføres(andelerTilkjentYtelse, forrigeTilkjenteYtelse, opphørsperioder)
            } ?: andelerTilkjentYtelse


    private fun nyttStartdato(innvilget: Innvilget, forrigeOpphørsdato: LocalDate?): LocalDate? {
        return min(innvilget.perioder.minOfOrNull { it.årMånedFra.atDay(1) }, forrigeOpphørsdato)
    }

    private fun opprettTilkjentYtelseForSanksjonertBehandling(vedtak: Sanksjonert,
                                                              saksbehandling: Saksbehandling) {
        brukerfeilHvis(saksbehandling.forrigeBehandlingId == null) {
            "Kan ikke opprette sanksjon når det ikke finnes en tidligere behandling"
        }
        val forrigeTilkjenteYtelse = hentForrigeTilkjenteYtelse(saksbehandling)
        val andelerTilkjentYtelse = andelerForSanksjonertRevurdering(forrigeTilkjenteYtelse, vedtak)

        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelse(personident = saksbehandling.ident,
                                                                   behandlingId = saksbehandling.id,
                                                                   andelerTilkjentYtelse = andelerTilkjentYtelse,
                                                                   startdato = forrigeTilkjenteYtelse.startdato))
    }

    private fun finnOpphørsperioder(vedtak: Innvilget) =
            vedtak.perioder.filter { it.periodeType == VedtaksperiodeType.MIDLERTIDIG_OPPHØR }.tilPerioder()

    private fun finnInnvilgedePerioder(vedtak: Innvilget) =
            vedtak.perioder.filter { it.periodeType != VedtaksperiodeType.MIDLERTIDIG_OPPHØR }.tilPerioder()

    private fun lagBeløpsperioderForInnvilgetVedtak(vedtak: Innvilget,
                                                    saksbehandling: Saksbehandling) =
            beregningService.beregnYtelse(finnInnvilgedePerioder(vedtak), vedtak.inntekter.tilInntektsperioder())
                    .map {
                        AndelTilkjentYtelse(beløp = it.beløp.toInt(),
                                            stønadFom = it.periode.fradato,
                                            stønadTom = it.periode.tildato,
                                            kildeBehandlingId = saksbehandling.id,
                                            personIdent = saksbehandling.ident,
                                            samordningsfradrag = it.beregningsgrunnlag?.samordningsfradrag?.toInt() ?: 0,
                                            inntekt = it.beregningsgrunnlag?.inntekt?.toInt() ?: 0,
                                            inntektsreduksjon = it.beregningsgrunnlag?.avkortningPerMåned?.toInt() ?: 0)
                    }

    fun slåSammenAndelerSomSkalVidereføres(beløpsperioder: List<AndelTilkjentYtelse>,
                                           forrigeTilkjentYtelse: TilkjentYtelse,
                                           opphørsperioder: List<Periode>): List<AndelTilkjentYtelse> {
        val fomPerioder = beløpsperioder.firstOrNull()?.stønadFom ?: LocalDate.MAX
        val fomOpphørPerioder = opphørsperioder.firstOrNull()?.fradato ?: LocalDate.MAX
        val nyePerioderUtenOpphør =
                forrigeTilkjentYtelse.taMedAndelerFremTilDato(minOf(fomPerioder, fomOpphørPerioder)) + beløpsperioder
        return vurderPeriodeForOpphør(nyePerioderUtenOpphør, opphørsperioder)

    }

    private fun andelerForSanksjonertRevurdering(forrigeTilkjenteYtelse: TilkjentYtelse,
                                                 vedtak: Sanksjonert): List<AndelTilkjentYtelse> {
        val andelerTilkjentYtelse = vurderPeriodeForOpphør(forrigeTilkjenteYtelse.andelerTilkjentYtelse,
                                                           listOf(vedtak.periode.tilPeriode()))
        brukerfeilHvis(andelerTilkjentYtelse.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }
        return andelerTilkjentYtelse
    }

    fun vurderPeriodeForOpphør(andelTilkjentYtelser: List<AndelTilkjentYtelse>,
                               opphørsperioder: List<Periode>): List<AndelTilkjentYtelse> {
        return andelTilkjentYtelser.map {

            val tilkjentPeriode = it.periode
            if (opphørsperioder.none { periode -> periode.overlapper(tilkjentPeriode) }) {
                listOf(it)
            } else if (opphørsperioder.any { periode -> periode.omslutter(tilkjentPeriode) }) {
                listOf()
            } else {
                val overlappendeOpphør = opphørsperioder.first { periode -> periode.overlapper(tilkjentPeriode) }

                if (overlappendeOpphør.overlapperIStartenAv(tilkjentPeriode)) {
                    vurderPeriodeForOpphør(listOf(it.copy(stønadFom = overlappendeOpphør.tildato.plusDays(1))), opphørsperioder)
                } else if (overlappendeOpphør.overlapperISluttenAv(tilkjentPeriode)) {
                    vurderPeriodeForOpphør(listOf(it.copy(stønadTom = overlappendeOpphør.fradato.minusDays(1))), opphørsperioder)
                } else { // periode blir delt i to av opphold.
                    vurderPeriodeForOpphør(listOf(it.copy(stønadTom = overlappendeOpphør.fradato.minusDays(1)),
                                                  it.copy(stønadFom = overlappendeOpphør.tildato.plusDays(1))), opphørsperioder)
                }
            }
        }.flatten()
    }

    private fun andelerForOpphør(forrigeTilkjentYtelse: TilkjentYtelse, opphørFom: LocalDate): List<AndelTilkjentYtelse> {
        brukerfeilHvis(forrigeTilkjentYtelse.andelerTilkjentYtelse.maxOfOrNull { it.stønadTom }?.isBefore(opphørFom) ?: false) {
            "Kan ikke opphøre frem i tiden"
        }

        brukerfeilHvis(forrigeTilkjentYtelse.andelerTilkjentYtelse.isEmpty() &&
                       forrigeTilkjentYtelse.startdato != null && forrigeTilkjentYtelse.startdato <= opphørFom) {
            "Forrige vedtak er allerede opphørt fra ${forrigeTilkjentYtelse.startdato}"
        }

        return forrigeTilkjentYtelse.taMedAndelerFremTilDato(opphørFom)
    }

    private fun hentForrigeTilkjenteYtelse(saksbehandling: Saksbehandling): TilkjentYtelse {
        val forrigeBehandlingId = saksbehandling.forrigeBehandlingId
                                  ?: error("Finner ikke forrige behandling til behandling=${saksbehandling.id}")
        return tilkjentYtelseService.hentForBehandling(forrigeBehandlingId)
    }

}
