package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.REVURDERING
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynService
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
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonert
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.erSammenhengende
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class BeregnYtelseSteg(private val tilkjentYtelseService: TilkjentYtelseService,
                       private val beregningService: BeregningService,
                       private val beregningBarnetilsynService: BeregningBarnetilsynService,
                       private val simuleringService: SimuleringService,
                       private val vedtakService: VedtakService,
                       private val tilbakekrevingService: TilbakekrevingService,
                       private val barnService: BarnService,
                       private val fagsakService: FagsakService) : BehandlingSteg<VedtakDto> {


    override fun stegType(): StegType {
        return StegType.BEREGNE_YTELSE
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: VedtakDto) {
        validerStønadstype(saksbehandling, data)
        validerGyldigeVedtaksperioder(saksbehandling, data)

        val aktivIdent = fagsakService.fagsakMedOppdatertPersonIdent(saksbehandling.fagsakId).hentAktivIdent()
        val saksbehandlingMedOppdatertIdent = saksbehandling.copy(ident = aktivIdent)
        nullstillEksisterendeVedtakPåBehandling(saksbehandlingMedOppdatertIdent.id)
        vedtakService.lagreVedtak(vedtakDto = data,
                                  behandlingId = saksbehandlingMedOppdatertIdent.id,
                                  stønadstype = saksbehandlingMedOppdatertIdent.stønadstype)

        when (data) {
            is InnvilgelseOvergangsstønad -> {
                validerStartTidEtterSanksjon(data, saksbehandlingMedOppdatertIdent)
                opprettTilkjentYtelseForInnvilgetOvergangsstønad(data, saksbehandlingMedOppdatertIdent)
                simuleringService.hentOgLagreSimuleringsresultat(saksbehandlingMedOppdatertIdent)
            }
            is InnvilgelseBarnetilsyn -> {
                validerStartTidEtterSanksjon(data, saksbehandlingMedOppdatertIdent)
                opprettTilkjentYtelseForInnvilgetBarnetilsyn(data, saksbehandlingMedOppdatertIdent)
                simuleringService.hentOgLagreSimuleringsresultat(saksbehandlingMedOppdatertIdent)
            }
            is Opphør -> {
                validerStartTidEtterSanksjon(data.opphørFom, saksbehandlingMedOppdatertIdent)
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

    private fun validerStartTidEtterSanksjon(innvilget: InnvilgelseBarnetilsyn, behandling: Saksbehandling) {
        innvilget.perioder.firstOrNull()?.let {
            validerStartTidEtterSanksjon(it.årMånedFra, behandling)
        }
    }

    private fun validerStartTidEtterSanksjon(innvilget: InnvilgelseOvergangsstønad, behandling: Saksbehandling) {
        innvilget.perioder.firstOrNull()?.let {
            validerStartTidEtterSanksjon(it.årMånedFra, behandling)
        }
    }

    private fun validerStartTidEtterSanksjon(vedtakFom: YearMonth, behandling: Saksbehandling) {
        val nyesteSanksjonsperiode = tilkjentYtelseService.hentHistorikk(behandling.fagsakId, null)
                .lastOrNull { it.periodeType == VedtaksperiodeType.SANKSJON }
        nyesteSanksjonsperiode?.andel?.stønadFra?.let { sanksjonsdato ->
            feilHvis(sanksjonsdato >= vedtakFom.atDay(1)) {
                "Systemet støtter ikke revurdering før sanksjonsperioden. Kontakt brukerstøtte for videre bistand"
            }
        }
    }


    private fun validerGyldigeVedtaksperioder(saksbehandling: Saksbehandling, data: VedtakDto) {
        if (data is InnvilgelseOvergangsstønad) {
            val harOpphørsperioder = data.perioder.any { it.periodeType == VedtaksperiodeType.MIDLERTIDIG_OPPHØR }
            val harInnvilgedePerioder = data.perioder.any { it.periodeType != VedtaksperiodeType.MIDLERTIDIG_OPPHØR }
            brukerfeilHvis(harOpphørsperioder && !harInnvilgedePerioder) {
                "Må ha innvilgelsesperioder i tillegg til opphørsperioder"
            }
            brukerfeilHvis(!saksbehandling.erMigrering() && harPeriodeEllerAktivitetMigrering(data)) {
                "Kan ikke inneholde aktivitet eller periode av type migrering"
            }
        }
        if (data is InnvilgelseBarnetilsyn) {
            barnService.validerBarnFinnesPåBehandling(saksbehandling.id, data.perioder.flatMap { it.barn }.toSet())
        }
    }

    private fun validerStønadstype(saksbehandling: Saksbehandling, data: VedtakDto) {
        when (data) {
            is InnvilgelseOvergangsstønad -> validerStønadstype(saksbehandling, data, StønadType.OVERGANGSSTØNAD)
            is InnvilgelseBarnetilsyn -> validerStønadstype(saksbehandling, data, StønadType.BARNETILSYN)
            else -> return
        }
    }

    private fun validerStønadstype(saksbehandling: Saksbehandling, vedtak: VedtakDto, stønadstype: StønadType) {
        feilHvis(saksbehandling.stønadstype != stønadstype) {
            "Feil stønadstype=${saksbehandling.stønadstype} for gitt vedtakstype ${vedtak::class.java.simpleName}"
        }
    }

    private fun harPeriodeEllerAktivitetMigrering(data: InnvilgelseOvergangsstønad) =
            data.perioder.any { it.periodeType == VedtaksperiodeType.MIGRERING || it.aktivitet == AktivitetType.MIGRERING }

    private fun nullstillEksisterendeVedtakPåBehandling(behandlingId: UUID) {
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(behandlingId)
        vedtakService.slettVedtakHvisFinnes(behandlingId)
    }

    private fun opprettTilkjentYtelseForOpphørtBehandling(saksbehandling: Saksbehandling,
                                                          vedtak: Opphør) {
        brukerfeilHvis(saksbehandling.type != REVURDERING) { "Kan kun opphøre ved revurdering" }
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
                                                    opphørsdato: LocalDate,
                                                    forrigeTilkjenteYtelse: TilkjentYtelse): LocalDate {
        val opphørsdatoHvisFørTidligereAndeler = if (nyeAndeler.isEmpty()) opphørsdato else null
        @Suppress("FoldInitializerAndIfToElvis")
        if (opphørsdatoHvisFørTidligereAndeler == null) return forrigeTilkjenteYtelse.startdato

        return minOf(opphørsdatoHvisFørTidligereAndeler, forrigeTilkjenteYtelse.startdato)
    }

    private fun opprettTilkjentYtelseForInnvilgetOvergangsstønad(vedtak: InnvilgelseOvergangsstønad,
                                                                 saksbehandling: Saksbehandling) {

        brukerfeilHvis(!vedtak.perioder.erSammenhengende()) { "Periodene må være sammenhengende" }
        val andelerTilkjentYtelse: List<AndelTilkjentYtelse> =
                lagBeløpsperioderForInnvilgelseOvergangsstønad(vedtak, saksbehandling)
        brukerfeilHvis(andelerTilkjentYtelse.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }

        val (nyeAndeler, startdato) = when (saksbehandling.type) {
            FØRSTEGANGSBEHANDLING -> andelerTilkjentYtelse to startdatoForFørstegangsbehandling(andelerTilkjentYtelse)
            REVURDERING -> nyeAndelerForRevurderingAvOvergangsstønadMedStartdato(saksbehandling, vedtak, andelerTilkjentYtelse)
            else -> error("Steg ikke støttet for type=${saksbehandling.type}")
        }


        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelse(personident = saksbehandling.ident,
                                                                   behandlingId = saksbehandling.id,
                                                                   andelerTilkjentYtelse = nyeAndeler,
                                                                   samordningsfradragType = vedtak.samordningsfradragType,
                                                                   startdato = startdato))
    }

    private fun opprettTilkjentYtelseForInnvilgetBarnetilsyn(vedtak: InnvilgelseBarnetilsyn,
                                                             saksbehandling: Saksbehandling) {

        // TODO: Må periodene være sammenhengende?
        //  brukerfeilHvis(!vedtak.perioder.erSammenhengende()) { "Periodene må være sammenhengende" }
        val andelerTilkjentYtelse: List<AndelTilkjentYtelse> = lagBeløpsperioderForInnvilgelseBarnetilsyn(vedtak, saksbehandling)
        brukerfeilHvis(andelerTilkjentYtelse.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }

        val (nyeAndeler, startdato) = when (saksbehandling.type) {
            FØRSTEGANGSBEHANDLING -> andelerTilkjentYtelse to startdatoForFørstegangsbehandling(andelerTilkjentYtelse)
            REVURDERING -> nyeAndelerForRevurderingAvBarnetilsynMedStartdato(saksbehandling, vedtak, andelerTilkjentYtelse)
            else -> error("Steg ikke støttet for type=${saksbehandling.type}")
        }


        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelse(personident = saksbehandling.ident,
                                                                   behandlingId = saksbehandling.id,
                                                                   andelerTilkjentYtelse = nyeAndeler,
                                                                   startdato = startdato))
    }

    private fun startdatoForFørstegangsbehandling(andelerTilkjentYtelse: List<AndelTilkjentYtelse>): LocalDate {
        return andelerTilkjentYtelse.minOfOrNull { it.stønadFom } ?: error("Må ha med en periode i førstegangsbehandling")
    }

    private fun nyeAndelerForRevurderingAvOvergangsstønadMedStartdato(saksbehandling: Saksbehandling,
                                                                      vedtak: InnvilgelseOvergangsstønad,
                                                                      andelerTilkjentYtelse: List<AndelTilkjentYtelse>)
            : Pair<List<AndelTilkjentYtelse>, LocalDate> {
        val opphørsperioder = finnOpphørsperioder(vedtak)

        val forrigeTilkjenteYtelse = saksbehandling.forrigeBehandlingId?.let { hentForrigeTilkjenteYtelse(saksbehandling) }
        validerOpphørsperioder(opphørsperioder, finnInnvilgedePerioder(vedtak), forrigeTilkjenteYtelse)

        val nyeAndeler = beregnNyeAndelerForRevurdering(forrigeTilkjenteYtelse, andelerTilkjentYtelse, opphørsperioder)

        val forrigeOpphørsdato = forrigeTilkjenteYtelse?.startdato
        val startdato = nyttStartdato(saksbehandling.id, vedtak.perioder.tilPerioder(), forrigeOpphørsdato)
        return nyeAndeler to startdato
    }

    private fun nyeAndelerForRevurderingAvBarnetilsynMedStartdato(saksbehandling: Saksbehandling,
                                                                  vedtak: InnvilgelseBarnetilsyn,
                                                                  andelerTilkjentYtelse: List<AndelTilkjentYtelse>)
            : Pair<List<AndelTilkjentYtelse>, LocalDate> {
        val opphørsperioder = finnOpphørsperioder(vedtak)

        val forrigeTilkjenteYtelse = saksbehandling.forrigeBehandlingId?.let { hentForrigeTilkjenteYtelse(saksbehandling) }
        validerOpphørsperioder(opphørsperioder, finnInnvilgedePerioder(vedtak), forrigeTilkjenteYtelse)

        val nyeAndeler = beregnNyeAndelerForRevurdering(forrigeTilkjenteYtelse, andelerTilkjentYtelse, opphørsperioder)

        val forrigeOpphørsdato = forrigeTilkjenteYtelse?.startdato
        val startdato = nyttStartdato(saksbehandling.id, vedtak.perioder.tilPerioder(), forrigeOpphørsdato)
        return nyeAndeler to startdato
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


    private fun nyttStartdato(behandlingId: UUID,
                              perioder: List<Periode>,
                              forrigeOpphørsdato: LocalDate?): LocalDate {
        val startdato = min(perioder.minOfOrNull { it.fradato }, forrigeOpphørsdato)
        feilHvis(startdato == null) {
            "Klarer ikke å beregne startdato for behandling=$behandlingId"
        }
        return startdato
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

    private fun finnOpphørsperioder(vedtak: InnvilgelseOvergangsstønad) =
            vedtak.perioder.filter { it.periodeType == VedtaksperiodeType.MIDLERTIDIG_OPPHØR }.tilPerioder()

    private fun finnOpphørsperioder(vedtak: InnvilgelseBarnetilsyn) =
            vedtak.perioder.filter { it.utgifter == 0 }.tilPerioder()

    private fun finnInnvilgedePerioder(vedtak: InnvilgelseOvergangsstønad) =
            vedtak.perioder.filter { it.periodeType != VedtaksperiodeType.MIDLERTIDIG_OPPHØR }.tilPerioder()

    private fun finnInnvilgedePerioder(vedtak: InnvilgelseBarnetilsyn) =
            vedtak.perioder.filter { it.utgifter != 0 }.tilPerioder()

    private fun lagBeløpsperioderForInnvilgelseOvergangsstønad(vedtak: InnvilgelseOvergangsstønad,
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

    private fun lagBeløpsperioderForInnvilgelseBarnetilsyn(vedtak: InnvilgelseBarnetilsyn,
                                                           saksbehandling: Saksbehandling) =
            beregningBarnetilsynService.beregnYtelseBarnetilsyn(vedtak)
                    .map {
                        AndelTilkjentYtelse(beløp = it.beløp,
                                            stønadFom = it.periode.fradato,
                                            stønadTom = it.periode.tildato,
                                            kildeBehandlingId = saksbehandling.id,
                                            inntekt = 0,
                                            samordningsfradrag = 0,
                                            inntektsreduksjon = 0,
                                            personIdent = saksbehandling.ident)
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
                       forrigeTilkjentYtelse.startdato <= opphørFom) {
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
