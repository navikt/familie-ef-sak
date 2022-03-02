package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.felles.util.min
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
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
                       private val fagsakService: FagsakService,
                       private val featureToggleService: FeatureToggleService) : BehandlingSteg<VedtakDto> {


    override fun validerSteg(behandling: Behandling) {

    }

    override fun stegType(): StegType {
        return StegType.BEREGNE_YTELSE
    }

    override fun utførSteg(behandling: Behandling, data: VedtakDto) {
        validerGyldigeVedtaksperioder(behandling, data)
        val aktivIdent = fagsakService.fagsakMedOppdatertPersonIdent(behandling.fagsakId).hentAktivIdent()
        nullstillEksisterendeVedtakPåBehandling(behandling.id)
        vedtakService.lagreVedtak(vedtakDto = data, behandlingId = behandling.id)

        when (data) {
            is Innvilget -> {
                opprettTilkjentYtelseForInnvilgetBehandling(data, behandling, aktivIdent)
                simuleringService.hentOgLagreSimuleringsresultat(behandling)
            }
            is Opphør -> {
                opprettTilkjentYtelseForOpphørtBehandling(behandling, data, aktivIdent)
                simuleringService.hentOgLagreSimuleringsresultat(behandling)
            }
            is Avslå -> {
                simuleringService.slettSimuleringForBehandling(behandling.id)
                tilbakekrevingService.slettTilbakekreving(behandling.id)
            }
            is Sanksjonert -> {
                opprettTilkjentYtelseForSanksjonertBehandling(data, behandling, aktivIdent)
            }
        }
    }

    private fun validerGyldigeVedtaksperioder(behandling: Behandling, data: VedtakDto) {
        if (data is Innvilget) {
            val harOpphørsperioder = data.perioder.any { it.periodeType == VedtaksperiodeType.MIDLERTIDIG_OPPHØR }
            val harInnvilgedePerioder = data.perioder.any { it.periodeType != VedtaksperiodeType.MIDLERTIDIG_OPPHØR }
            brukerfeilHvis(harOpphørsperioder && !harInnvilgedePerioder) {
                "Må ha innvilgelsesperioder i tillegg til opphørsperioder"
            }
            brukerfeilHvis(!behandling.erMigrering() && harPeriodeEllerAktivitetMigrering(data)) {
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

    private fun opprettTilkjentYtelseForOpphørtBehandling(behandling: Behandling,
                                                          vedtak: Opphør,
                                                          aktivIdent: String) {
        brukerfeilHvis(behandling.type != BehandlingType.REVURDERING) { "Kan kun opphøre ved revurdering" }
        val opphørsdato = vedtak.opphørFom.atDay(1)
        val forrigeTilkjenteYtelse = hentForrigeTilkjenteYtelse(behandling)
        val nyeAndeler = andelerForOpphør(forrigeTilkjenteYtelse, opphørsdato)
        val nyttOpphørsdato = beregnNyttOpphørsdatoForRevurdering(nyeAndeler, opphørsdato, forrigeTilkjenteYtelse)
        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelse(personident = aktivIdent,
                                                                   behandlingId = behandling.id,
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
                                                            behandling: Behandling,
                                                            aktivIdent: String) {

        brukerfeilHvis(!vedtak.perioder.erSammenhengende()) { "Periodene må være sammenhengende" }
        val andelerTilkjentYtelse: List<AndelTilkjentYtelse> = lagBeløpsperioderForInnvilgetVedtak(vedtak, behandling, aktivIdent)
        brukerfeilHvis(andelerTilkjentYtelse.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }

        val (nyeAndeler, opphørsdato) = when (behandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> andelerTilkjentYtelse to datoForFørstegangsbehandling(andelerTilkjentYtelse)
            BehandlingType.REVURDERING -> nyeAndelerForRevurderingMedOpphørsdato(behandling, vedtak, andelerTilkjentYtelse)
            else -> error("Steg ikke støttet for type=${behandling.type}")
        }


        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelse(personident = aktivIdent,
                                                                   behandlingId = behandling.id,
                                                                   andelerTilkjentYtelse = nyeAndeler,
                                                                   samordningsfradragType = vedtak.samordningsfradragType,
                                                                   startdato = opphørsdato))
    }

    private fun datoForFørstegangsbehandling(andelerTilkjentYtelse: List<AndelTilkjentYtelse>): LocalDate? {
        return if (featureToggleService.isEnabled("familie.ef.sak.startdato")) {
            andelerTilkjentYtelse.minOfOrNull { it.stønadFom } // eller minOf? Må vel alltid ha en andel hvis man innvilget en førstegangsbehandling?
        } else null
    }

    private fun nyeAndelerForRevurderingMedOpphørsdato(behandling: Behandling,
                                                       vedtak: Innvilget,
                                                       andelerTilkjentYtelse: List<AndelTilkjentYtelse>): Pair<List<AndelTilkjentYtelse>, LocalDate?> {
        val opphørsperioder = finnOpphørsperioder(vedtak)

        val forrigeTilkjenteYtelse = behandling.forrigeBehandlingId?.let { hentForrigeTilkjenteYtelse(behandling) }
        validerStartdato(forrigeTilkjenteYtelse)
        validerOpphørsperioder(opphørsperioder, finnInnvilgedePerioder(vedtak), forrigeTilkjenteYtelse)

        val nyeAndeler = beregnNyeAndelerForRevurdering(forrigeTilkjenteYtelse, andelerTilkjentYtelse, opphørsperioder)

        val forrigeOpphørsdato = forrigeTilkjenteYtelse?.startdato
        val opphørsdato = opphørsdatoHvisFørFørsteAndelSinFomDato(opphørsperioder, nyeAndeler, forrigeOpphørsdato)
        return nyeAndeler to opphørsdato
    }

    private fun validerStartdato(forrigeTilkjenteYtelse: TilkjentYtelse?) {
        feilHvis(featureToggleService.isEnabled("familie.ef.sak.startdato") &&
                 forrigeTilkjenteYtelse != null && forrigeTilkjenteYtelse.startdato == null) {
            "Mangler startdato på tilkjent ytelse behandlingId=${forrigeTilkjenteYtelse?.behandlingId}"
        }
    }

    private fun validerOpphørsperioder(opphørsperioder: List<Periode>,
                                       vedtaksperioder: List<Periode>,
                                       forrigeTilkjenteYtelse: TilkjentYtelse?) {
        if (featureToggleService.isEnabled("familie.ef.sak.startdato")) {
            return
        }
        val førsteOpphørsdato = opphørsperioder.minOfOrNull { it.fradato }
        val førsteVedtaksFradato = vedtaksperioder.minOfOrNull { it.fradato }
        val harKunOpphørEllerOpphørFørInnvilgetPeriode =
                førsteOpphørsdato != null && (førsteVedtaksFradato == null || førsteOpphørsdato < førsteVedtaksFradato)
        feilHvis(forrigeTilkjenteYtelse == null && harKunOpphørEllerOpphørFørInnvilgetPeriode) {
            "Har ikke støtte for å innvilge med opphør først, når man mangler tidligere behandling å opphøre"
        }
        val harKun0Beløp = forrigeTilkjenteYtelse?.andelerTilkjentYtelse?.all { it.beløp == 0 } ?: false
        feilHvis(harKun0Beløp && harKunOpphørEllerOpphørFørInnvilgetPeriode) {
            "Har ikke støtte for å innvilge med opphør først, når man kun har perioder med 0 som beløp fra før"
        }
    }

    private fun beregnNyeAndelerForRevurdering(forrigeTilkjenteYtelse: TilkjentYtelse?,
                                               andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                                               opphørsperioder: List<Periode>) =
            forrigeTilkjenteYtelse?.let {
                slåSammenAndelerSomSkalVidereføres(andelerTilkjentYtelse, forrigeTilkjenteYtelse, opphørsperioder)
            } ?: andelerTilkjentYtelse


    private fun opphørsdatoHvisFørFørsteAndelSinFomDato(opphørsperioder: List<Periode>,
                                                        andeler: List<AndelTilkjentYtelse>,
                                                        forrigeOpphørsdato: LocalDate?): LocalDate? {
        val nyttOpphørsdato = nyttOpphørsdato(opphørsperioder, andeler)
        return min(nyttOpphørsdato, forrigeOpphørsdato)
    }

    // TODO når featureToggleService fjernes kan opphørsdatoHvisFørFørsteAndelSinFomDato gjøre en min på vedtakens sine perioder
    // og ikke forholde seg til både opphørsperioder og andeler
    private fun nyttOpphørsdato(opphørsperioder: List<Periode>,
                                andeler: List<AndelTilkjentYtelse>): LocalDate? {
        return if (featureToggleService.isEnabled("familie.ef.sak.startdato")) {
            min(opphørsperioder.minOfOrNull { it.fradato }, andeler.minOfOrNull { it.stønadFom })
        } else {
            opphørsperioder.minOfOrNull { it.fradato }
                    ?.takeIf { stønadsdato -> andeler.minOfOrNull { it.stønadFom }?.isAfter(stønadsdato) ?: false }
        }
    }

    private fun opprettTilkjentYtelseForSanksjonertBehandling(vedtak: Sanksjonert,
                                                              behandling: Behandling,
                                                              aktivIdent: String) {

        val andelerTilkjentYtelse = andelerForSanksjonertRevurdering(behandling, vedtak.periode.tilPeriode())
        brukerfeilHvis(andelerTilkjentYtelse.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }

        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelse(personident = aktivIdent,
                                                                   behandlingId = behandling.id,
                                                                   andelerTilkjentYtelse = andelerTilkjentYtelse))
    }

    private fun finnOpphørsperioder(vedtak: Innvilget) =
            vedtak.perioder.filter { it.periodeType == VedtaksperiodeType.MIDLERTIDIG_OPPHØR }.tilPerioder()

    private fun finnInnvilgedePerioder(vedtak: Innvilget) =
            vedtak.perioder.filter { it.periodeType != VedtaksperiodeType.MIDLERTIDIG_OPPHØR }.tilPerioder()

    private fun lagBeløpsperioderForInnvilgetVedtak(vedtak: Innvilget,
                                                    behandling: Behandling,
                                                    aktivIdent: String) =
            beregningService.beregnYtelse(finnInnvilgedePerioder(vedtak), vedtak.inntekter.tilInntektsperioder())
                    .map {
                        AndelTilkjentYtelse(beløp = it.beløp.toInt(),
                                            stønadFom = it.periode.fradato,
                                            stønadTom = it.periode.tildato,
                                            kildeBehandlingId = behandling.id,
                                            personIdent = aktivIdent,
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

    private fun andelerForSanksjonertRevurdering(behandling: Behandling,
                                                 opphørsperiode: Periode): List<AndelTilkjentYtelse> {
        return behandling.forrigeBehandlingId?.let {
            val forrigeTilkjenteYtelse = hentForrigeTilkjenteYtelse(behandling)
            return vurderPeriodeForOpphør(forrigeTilkjenteYtelse.andelerTilkjentYtelse, listOf(opphørsperiode))
        } ?: throw Feil("Kan ikke opprette sanksjon når det ikke finnes en tidligere behandling")
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
                       forrigeTilkjentYtelse.startdato != null && forrigeTilkjentYtelse.startdato < opphørFom) {
            "Forrige vedtak er allerede opphørt fra ${forrigeTilkjentYtelse.startdato}"
        }

        brukerfeilHvis(forrigeTilkjentYtelse.andelerTilkjentYtelse.all { it.beløp == 0 }) {
            "Har ikke støtte for å opphøre når alle tidligere perioder har 0 i stønad"
        }

        return forrigeTilkjentYtelse.taMedAndelerFremTilDato(opphørFom)
    }

    private fun hentForrigeTilkjenteYtelse(behandling: Behandling): TilkjentYtelse {
        val forrigeBehandlingId = behandling.forrigeBehandlingId
                                  ?: error("Finner ikke forrige behandling til behandling=${behandling.id}")
        return tilkjentYtelseService.hentForBehandling(forrigeBehandlingId)
    }

}
