package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.REVURDERING
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.ValiderOmregningService
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynService
import no.nav.familie.ef.sak.beregning.skolepenger.BeregningSkolepengerService
import no.nav.familie.ef.sak.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.min
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseSkolepenger
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.OpphørSkolepenger
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonert
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakSkolepengerDto
import no.nav.familie.ef.sak.vedtak.dto.erSammenhengende
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import no.nav.familie.ef.sak.vedtak.historikk.erIkkeFjernet
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.erSammenhengende
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.reflect.KClass

@Service
class BeregnYtelseSteg(
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val andelsHistorikkService: AndelsHistorikkService,
    private val beregningService: BeregningService,
    private val beregningBarnetilsynService: BeregningBarnetilsynService,
    private val beregningSkolepengerService: BeregningSkolepengerService,
    private val simuleringService: SimuleringService,
    private val vedtakService: VedtakService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val barnService: BarnService,
    private val fagsakService: FagsakService,
    private val validerOmregningService: ValiderOmregningService,
    private val featureToggleService: FeatureToggleService
) : BehandlingSteg<VedtakDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun stegType(): StegType {
        return StegType.BEREGNE_YTELSE
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: VedtakDto) {
        validerStønadstype(saksbehandling, data)
        validerGyldigeVedtaksperioder(saksbehandling, data)

        val aktivIdent = fagsakService.fagsakMedOppdatertPersonIdent(saksbehandling.fagsakId).hentAktivIdent()
        val saksbehandlingMedOppdatertIdent = saksbehandling.copy(ident = aktivIdent)
        nullstillEksisterendeVedtakPåBehandling(saksbehandlingMedOppdatertIdent.id)
        vedtakService.lagreVedtak(
            vedtakDto = data,
            behandlingId = saksbehandlingMedOppdatertIdent.id,
            stønadstype = saksbehandlingMedOppdatertIdent.stønadstype
        )

        when (data) {
            is InnvilgelseOvergangsstønad -> {
                validerOmregningService.validerHarSammePerioderSomTidligereVedtak(data, saksbehandlingMedOppdatertIdent)
                validerStartTidEtterSanksjon(data, saksbehandlingMedOppdatertIdent)
                opprettTilkjentYtelseForInnvilgetOvergangsstønad(data, saksbehandlingMedOppdatertIdent)
                simuleringService.hentOgLagreSimuleringsresultat(saksbehandlingMedOppdatertIdent)
            }
            is InnvilgelseBarnetilsyn -> {
                validerStartTidEtterSanksjon(data, saksbehandlingMedOppdatertIdent)
                opprettTilkjentYtelseForInnvilgetBarnetilsyn(data, saksbehandlingMedOppdatertIdent)
                simuleringService.hentOgLagreSimuleringsresultat(saksbehandlingMedOppdatertIdent)
            }
            is VedtakSkolepengerDto -> {
                opprettTilkjentYtelseForInnvilgetSkolepenger(data, saksbehandlingMedOppdatertIdent)
                simuleringService.hentOgLagreSimuleringsresultat(saksbehandlingMedOppdatertIdent)
            }
            is Opphør -> {
                validerStartTidEtterSanksjon(data.opphørFom, saksbehandlingMedOppdatertIdent)
                opprettTilkjentYtelseForOpphørtBehandling(saksbehandlingMedOppdatertIdent, data)
                simuleringService.hentOgLagreSimuleringsresultat(saksbehandlingMedOppdatertIdent)
            }
            is Avslå -> {
                simuleringService.slettSimuleringForBehandling(saksbehandlingMedOppdatertIdent)
                tilbakekrevingService.slettTilbakekreving(saksbehandlingMedOppdatertIdent.id)
            }
            is Sanksjonert -> {
                opprettTilkjentYtelseForSanksjonertBehandling(data, saksbehandlingMedOppdatertIdent)
            }
        }
    }

    private fun validerStartTidEtterSanksjon(innvilget: InnvilgelseBarnetilsyn, behandling: Saksbehandling) {
        innvilget.perioder.firstOrNull()?.let {
            validerStartTidEtterSanksjon(it.periode.fom, behandling)
        }
    }

    private fun validerStartTidEtterSanksjon(innvilget: InnvilgelseOvergangsstønad, behandling: Saksbehandling) {
        if (behandling.erOmregning) {
            return
        }

        innvilget.perioder.firstOrNull()?.let {
            validerStartTidEtterSanksjon(it.periode.fom, behandling)
        }
    }

    private fun validerStartTidEtterSanksjon(vedtakFom: YearMonth, behandling: Saksbehandling) {
        if (featureToggleService.isEnabled(Toggle.ERSTATTE_SANKSJON)) {
            logger.info("Ignorerer validerStartTidEtterSanksjon for behandling=${behandling.id}")
            return
        }
        val nyesteSanksjonsperiode = andelsHistorikkService.hentHistorikk(behandling.fagsakId, null)
            .filter { it.erIkkeFjernet() }
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
            brukerfeilHvis(
                !saksbehandling.erMigrering &&
                    !saksbehandling.erOmregning &&
                    harPeriodeEllerAktivitetMigrering(data)
            ) {
                "Kan ikke inneholde aktivitet eller periode av type migrering"
            }
        }
        if (data is InnvilgelseBarnetilsyn) {
            barnService.validerBarnFinnesPåBehandling(saksbehandling.id, data.perioder.flatMap { it.barn }.toSet())
            validerInnvilgelseBarnetilsyn(data.perioder, saksbehandling)
        }
    }

    private fun validerInnvilgelseBarnetilsyn(
        utgiftsperioder: List<UtgiftsperiodeDto>,
        saksbehandling: Saksbehandling
    ) {
        validerAntallBarnOgUtgifterVedMidlertidigOpphør(utgiftsperioder, saksbehandling.id)
        validerTidligereVedtakVedMidlertidigOpphør(utgiftsperioder, saksbehandling)
        validerSammenhengendePerioderVedMidlertidigOpphør(utgiftsperioder, saksbehandling)
    }

    private fun validerAntallBarnOgUtgifterVedMidlertidigOpphør(
        utgiftsperioder: List<UtgiftsperiodeDto>,
        behandlingId: UUID
    ) {
        brukerfeilHvis(utgiftsperioder.any { it.erMidlertidigOpphør && it.barn.isNotEmpty() }) {
            "Kan ikke ta med barn på en periode som er et midlertidig opphør, på behandling=$behandlingId"
        }
        brukerfeilHvis(utgiftsperioder.any { it.erMidlertidigOpphør && it.utgifter > 0 }) {
            "kan ikke ha utgifter større enn null på en periode som er et midlertidig opphør, på behandling=$behandlingId"
        }
        brukerfeilHvis(utgiftsperioder.any { !it.erMidlertidigOpphør && it.barn.isEmpty() }) {
            "Må ha med minst et barn på en periode som ikke er et midlertidig opphør, på behandling=$behandlingId"
        }
        brukerfeilHvis(utgiftsperioder.any { !it.erMidlertidigOpphør && it.utgifter <= 0 }) {
            "Kan ikke ha null utgifter på en periode som ikke er et midlertidig opphør, på behandling=$behandlingId"
        }
    }

    private fun validerTidligereVedtakVedMidlertidigOpphør(
        utgiftsperioder: List<UtgiftsperiodeDto>,
        saksbehandling: Saksbehandling
    ) {
        val førstePeriodeErMidlertidigOpphør = utgiftsperioder.first().erMidlertidigOpphør
        brukerfeilHvis(førstePeriodeErMidlertidigOpphør && saksbehandling.forrigeBehandlingId == null) {
            "Første periode kan ikke ha et nullbeløp, på førstegangsbehandling=${saksbehandling.id}"
        }
        val harIkkeInnvilgetBeløp =
            if (saksbehandling.forrigeBehandlingId != null) tilkjentYtelseService.hentForBehandling(saksbehandling.forrigeBehandlingId).andelerTilkjentYtelse.all { it.beløp == 0 } else true
        brukerfeilHvis(harIkkeInnvilgetBeløp && førstePeriodeErMidlertidigOpphør) {
            "Første periode kan ikke ha et nullbeløp dersom det ikke har blitt innvilget beløp på et tidligere vedtak, på behandling=${saksbehandling.id}"
        }
    }

    private fun validerSammenhengendePerioderVedMidlertidigOpphør(
        utgiftsperioder: List<UtgiftsperiodeDto>,
        saksbehandling: Saksbehandling
    ) {
        brukerfeilHvis(!utgiftsperioder.erSammenhengende()) {
            "Perioder som er midlertidig opphør må være sammenhengende, på behandling=${saksbehandling.id}"
        }
    }

    private fun validerStønadstype(saksbehandling: Saksbehandling, data: VedtakDto) {
        when (saksbehandling.stønadstype) {
            StønadType.OVERGANGSSTØNAD -> validerGyldigeVedtakstyper(
                saksbehandling.stønadstype,
                data,
                InnvilgelseOvergangsstønad::class,
                Avslå::class,
                Opphør::class,
                Sanksjonert::class
            )
            StønadType.BARNETILSYN -> validerGyldigeVedtakstyper(
                saksbehandling.stønadstype,
                data,
                InnvilgelseBarnetilsyn::class,
                Avslå::class,
                Opphør::class,
                Sanksjonert::class
            )
            StønadType.SKOLEPENGER -> validerGyldigeVedtakstyper(
                saksbehandling.stønadstype,
                data,
                InnvilgelseSkolepenger::class,
                Avslå::class,
                OpphørSkolepenger::class
            )
        }
    }

    private fun validerGyldigeVedtakstyper(
        stønadstype: StønadType,
        data: VedtakDto,
        vararg vedtakstype: KClass<out VedtakDto>
    ) {
        feilHvis(vedtakstype.none { it.isInstance(data) }) {
            "Stønadstype=$stønadstype har ikke støtte for ${data.javaClass.simpleName}"
        }
    }

    private fun harPeriodeEllerAktivitetMigrering(data: InnvilgelseOvergangsstønad) =
        data.perioder.any { it.periodeType == VedtaksperiodeType.MIGRERING || it.aktivitet == AktivitetType.MIGRERING }

    private fun nullstillEksisterendeVedtakPåBehandling(behandlingId: UUID) {
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(behandlingId)
        vedtakService.slettVedtakHvisFinnes(behandlingId)
    }

    private fun opprettTilkjentYtelseForOpphørtBehandling(
        saksbehandling: Saksbehandling,
        vedtak: Opphør
    ) {
        brukerfeilHvis(saksbehandling.type != REVURDERING) { "Kan kun opphøre ved revurdering" }
        val opphørsmåned = vedtak.opphørFom
        val forrigeTilkjenteYtelse = hentForrigeTilkjenteYtelse(saksbehandling)
        val nyeAndeler = andelerForOpphør(forrigeTilkjenteYtelse, opphørsmåned)
        val nyStartdato = beregnNyttStartdatoForRevurdering(nyeAndeler, opphørsmåned, forrigeTilkjenteYtelse)
        tilkjentYtelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                personident = saksbehandling.ident,
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = nyeAndeler,
                samordningsfradragType = null,
                startmåned = nyStartdato,
                grunnbeløpsmåned = forrigeTilkjenteYtelse.grunnbeløpsmåned
            )
        )
    }

    private fun beregnNyttStartdatoForRevurdering(
        nyeAndeler: List<AndelTilkjentYtelse>,
        opphørsmåned: YearMonth,
        forrigeTilkjenteYtelse: TilkjentYtelse
    ): YearMonth {
        val opphørsdatoHvisFørTidligereAndeler = if (nyeAndeler.isEmpty()) opphørsmåned else null
        @Suppress("FoldInitializerAndIfToElvis")
        if (opphørsdatoHvisFørTidligereAndeler == null) return forrigeTilkjenteYtelse.startmåned

        return minOf(opphørsdatoHvisFørTidligereAndeler, forrigeTilkjenteYtelse.startmåned)
    }

    private fun opprettTilkjentYtelseForInnvilgetOvergangsstønad(
        vedtak: InnvilgelseOvergangsstønad,
        saksbehandling: Saksbehandling
    ) {
        brukerfeilHvis(!saksbehandling.erOmregning && !vedtak.perioder.map { it.periode }.erSammenhengende()) {
            "Periodene må være sammenhengende"
        }

        val andelerTilkjentYtelse: List<AndelTilkjentYtelse> =
            lagBeløpsperioderForInnvilgelseOvergangsstønad(vedtak, saksbehandling)
        brukerfeilHvis(andelerTilkjentYtelse.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }

        val (nyeAndeler, startdato) = when (saksbehandling.type) {
            FØRSTEGANGSBEHANDLING -> andelerTilkjentYtelse to startdatoForFørstegangsbehandling(andelerTilkjentYtelse)
            REVURDERING -> nyeAndelerForRevurderingAvOvergangsstønadMedStartdato(
                saksbehandling,
                vedtak,
                andelerTilkjentYtelse
            )
        }

        tilkjentYtelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                personident = saksbehandling.ident,
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = nyeAndeler,
                samordningsfradragType = vedtak.samordningsfradragType,
                startmåned = startdato
            )
        )
    }

    private fun opprettTilkjentYtelseForInnvilgetBarnetilsyn(
        vedtak: InnvilgelseBarnetilsyn,
        saksbehandling: Saksbehandling
    ) {
        // TODO: Må periodene være sammenhengende?
        //  brukerfeilHvis(!vedtak.perioder.erSammenhengende()) { "Periodene må være sammenhengende" }
        val andelerTilkjentYtelse: List<AndelTilkjentYtelse> =
            lagBeløpsperioderForInnvilgelseBarnetilsyn(vedtak, saksbehandling)
        brukerfeilHvis(andelerTilkjentYtelse.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }

        val (nyeAndeler, startdato) = when (saksbehandling.type) {
            FØRSTEGANGSBEHANDLING -> andelerTilkjentYtelse to startdatoForFørstegangsbehandling(andelerTilkjentYtelse)
            REVURDERING -> nyeAndelerForRevurderingAvBarnetilsynMedStartdato(
                saksbehandling,
                vedtak,
                andelerTilkjentYtelse
            )
        }

        tilkjentYtelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                personident = saksbehandling.ident,
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = nyeAndeler,
                startmåned = startdato
            )
        )
    }

    private fun opprettTilkjentYtelseForInnvilgetSkolepenger(
        vedtak: VedtakSkolepengerDto,
        saksbehandling: Saksbehandling
    ) {
        val forrigeTilkjentYtelse = saksbehandling.forrigeBehandlingId?.let { forrigeBehandlingId ->
            tilkjentYtelseService.hentForBehandling(forrigeBehandlingId)
        }
        val andelerTilkjentYtelse = lagBeløpsperioderForInnvilgelseSkolepenger(vedtak, saksbehandling)
        validerSkolepenger(saksbehandling, vedtak, andelerTilkjentYtelse, forrigeTilkjentYtelse)
        val (nyeAndeler, startmåned) = when (saksbehandling.type) {
            FØRSTEGANGSBEHANDLING -> andelerTilkjentYtelse to startdatoForFørstegangsbehandling(andelerTilkjentYtelse)
            // Burde kanskje summere tidligere forbrukt fra andeler, per skoleår
            REVURDERING -> {
                val startdatoNyeAndeler = andelerTilkjentYtelse.minOfOrNull { it.periode.fomMåned }
                val nyttStartdato = min(forrigeTilkjentYtelse?.startmåned, startdatoNyeAndeler)
                    ?: error("Må ha startdato fra forrige behandling eller sende inn andeler")
                andelerTilkjentYtelse to nyttStartdato
            }
        }

        tilkjentYtelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                personident = saksbehandling.ident,
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = nyeAndeler,
                startmåned = startmåned
            )
        )
    }

    private fun validerSkolepenger(
        saksbehandling: Saksbehandling,
        vedtak: VedtakSkolepengerDto,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        forrigeTilkjentYtelse: TilkjentYtelse?
    ) {
        feilHvis(saksbehandling.type == FØRSTEGANGSBEHANDLING && vedtak !is InnvilgelseSkolepenger) {
            "Kan ikke opprette tilkjent ytelse for ${vedtak.javaClass.simpleName} på førstegangsbehandling"
        }
        brukerfeilHvis(!vedtak.erOpphør() && andelerTilkjentYtelse.isEmpty()) {
            "Innvilget vedtak må ha minimum en beløpsperiode"
        }
        brukerfeilHvis(
            forrigeTilkjentYtelse != null &&
                forrigeTilkjentYtelse.andelerTilkjentYtelse.isEmpty() &&
                vedtak.erOpphør()
        ) {
            "Kan ikke opphøre når det ikke finnes noen perioder å opphøre"
        }
    }

    private fun startdatoForFørstegangsbehandling(andelerTilkjentYtelse: List<AndelTilkjentYtelse>): YearMonth {
        return andelerTilkjentYtelse.minOfOrNull { it.periode.fomMåned }
            ?: error("Må ha med en periode i førstegangsbehandling")
    }

    private fun nyeAndelerForRevurderingAvOvergangsstønadMedStartdato(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseOvergangsstønad,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>
    ): Pair<List<AndelTilkjentYtelse>, YearMonth> {
        val opphørsperioder = finnOpphørsperioder(vedtak)

        val forrigeTilkjenteYtelse =
            saksbehandling.forrigeBehandlingId?.let { hentForrigeTilkjenteYtelse(saksbehandling) }
        validerOpphørsperioder(opphørsperioder, finnInnvilgedePerioder(vedtak), forrigeTilkjenteYtelse)

        val nyeAndeler = beregnNyeAndelerForRevurdering(forrigeTilkjenteYtelse, andelerTilkjentYtelse, opphørsperioder)

        val forrigeStartdato = forrigeTilkjenteYtelse?.startmåned
        val startdato = nyttStartdato(saksbehandling.id, vedtak.perioder.tilPerioder(), forrigeStartdato)
        return nyeAndeler to startdato
    }

    private fun nyeAndelerForRevurderingAvBarnetilsynMedStartdato(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseBarnetilsyn,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>
    ): Pair<List<AndelTilkjentYtelse>, YearMonth> {
        val opphørsperioder = finnOpphørsperioder(vedtak)

        val forrigeTilkjenteYtelse =
            saksbehandling.forrigeBehandlingId?.let { hentForrigeTilkjenteYtelse(saksbehandling) }
        validerOpphørsperioder(opphørsperioder, finnInnvilgedePerioder(vedtak), forrigeTilkjenteYtelse)

        val nyeAndeler = beregnNyeAndelerForRevurdering(forrigeTilkjenteYtelse, andelerTilkjentYtelse, opphørsperioder)

        val forrigeStartdato = forrigeTilkjenteYtelse?.startmåned
        val startmåned = nyttStartdato(saksbehandling.id, vedtak.perioder.tilPerioder(), forrigeStartdato)
        return nyeAndeler to startmåned
    }

    private fun validerOpphørsperioder(
        opphørsperioder: List<Månedsperiode>,
        vedtaksperioder: List<Månedsperiode>,
        forrigeTilkjenteYtelse: TilkjentYtelse?
    ) {
        val førsteOpphørsdato = opphørsperioder.minOfOrNull { it.fom }
        val førsteVedtaksFradato = vedtaksperioder.minOfOrNull { it.tom }
        val harKunOpphørEllerOpphørFørInnvilgetPeriode =
            førsteOpphørsdato != null && (førsteVedtaksFradato == null || førsteOpphørsdato < førsteVedtaksFradato)
        feilHvis(forrigeTilkjenteYtelse == null && harKunOpphørEllerOpphørFørInnvilgetPeriode) {
            "Har ikke støtte for å innvilge med opphør først, når man mangler tidligere behandling å opphøre"
        }
    }

    private fun beregnNyeAndelerForRevurdering(
        forrigeTilkjenteYtelse: TilkjentYtelse?,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        opphørsperioder: List<Månedsperiode>
    ) =
        forrigeTilkjenteYtelse?.let {
            slåSammenAndelerSomSkalVidereføres(andelerTilkjentYtelse, forrigeTilkjenteYtelse, opphørsperioder)
        } ?: andelerTilkjentYtelse

    private fun nyttStartdato(
        behandlingId: UUID,
        perioder: List<Månedsperiode>,
        forrigeStartdato: YearMonth?
    ): YearMonth {
        val startmåned = min(perioder.minOfOrNull { it.fom }, forrigeStartdato)
        feilHvis(startmåned == null) {
            "Klarer ikke å beregne startdato for behandling=$behandlingId"
        }
        return startmåned
    }

    private fun opprettTilkjentYtelseForSanksjonertBehandling(
        vedtak: Sanksjonert,
        saksbehandling: Saksbehandling
    ) {
        brukerfeilHvis(saksbehandling.forrigeBehandlingId == null) {
            "Kan ikke opprette sanksjon når det ikke finnes en tidligere behandling"
        }
        val forrigeTilkjenteYtelse = hentForrigeTilkjenteYtelse(saksbehandling)
        val andelerTilkjentYtelse = andelerForSanksjonertRevurdering(forrigeTilkjenteYtelse, vedtak)

        tilkjentYtelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                personident = saksbehandling.ident,
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                startmåned = forrigeTilkjenteYtelse.startmåned
            )
        )
    }

    private fun finnOpphørsperioder(vedtak: InnvilgelseOvergangsstønad) =
        vedtak.perioder.filter { it.periodeType == VedtaksperiodeType.MIDLERTIDIG_OPPHØR }.tilPerioder()

    private fun finnOpphørsperioder(vedtak: InnvilgelseBarnetilsyn) =
        vedtak.perioder.filter { it.utgifter == 0 }.tilPerioder()

    private fun finnInnvilgedePerioder(vedtak: InnvilgelseOvergangsstønad) =
        vedtak.perioder.filter { it.periodeType != VedtaksperiodeType.MIDLERTIDIG_OPPHØR }.tilPerioder()

    private fun finnInnvilgedePerioder(vedtak: InnvilgelseBarnetilsyn) =
        vedtak.perioder.filter { it.utgifter != 0 }.tilPerioder()

    private fun lagBeløpsperioderForInnvilgelseOvergangsstønad(
        vedtak: InnvilgelseOvergangsstønad,
        saksbehandling: Saksbehandling
    ) =
        beregningService.beregnYtelse(finnInnvilgedePerioder(vedtak), vedtak.inntekter.tilInntektsperioder())
            .map {
                AndelTilkjentYtelse(
                    beløp = it.beløp.toInt(),
                    periode = it.periode.toDatoperiode(),
                    kildeBehandlingId = saksbehandling.id,
                    personIdent = saksbehandling.ident,
                    samordningsfradrag = it.beregningsgrunnlag?.samordningsfradrag?.toInt() ?: 0,
                    inntekt = it.beregningsgrunnlag?.inntekt?.toInt() ?: 0,
                    inntektsreduksjon = it.beregningsgrunnlag?.avkortningPerMåned?.toInt() ?: 0
                )
            }

    private fun lagBeløpsperioderForInnvilgelseBarnetilsyn(
        vedtak: InnvilgelseBarnetilsyn,
        saksbehandling: Saksbehandling
    ): List<AndelTilkjentYtelse> {
        val beløpsperioder = beregningBarnetilsynService.beregnYtelseBarnetilsyn(
            vedtak.perioder.filterNot { it.erMidlertidigOpphør },
            vedtak.perioderKontantstøtte,
            vedtak.tilleggsstønad.perioder
        )
        validerRiktigResultattypeForInnvilgetBarnetilsyn(beløpsperioder, vedtak)
        return beløpsperioder
            .map {
                AndelTilkjentYtelse(
                    beløp = it.beløp,
                    periode = it.periode.toDatoperiode(),
                    kildeBehandlingId = saksbehandling.id,
                    inntekt = 0,
                    samordningsfradrag = 0,
                    inntektsreduksjon = 0,
                    personIdent = saksbehandling.ident
                )
            }
    }

    private fun lagBeløpsperioderForInnvilgelseSkolepenger(
        vedtak: VedtakSkolepengerDto,
        saksbehandling: Saksbehandling
    ): List<AndelTilkjentYtelse> {
        return beregningSkolepengerService.beregnYtelse(
            vedtak.skoleårsperioder,
            saksbehandling.id,
            vedtak.erOpphør()
        ).perioder
            .filter { it.beløp > 0 }
            .map {
                AndelTilkjentYtelse(
                    beløp = it.beløp,
                    periode = Datoperiode(it.årMånedFra, it.årMånedFra),
                    kildeBehandlingId = saksbehandling.id,
                    inntekt = 0,
                    samordningsfradrag = 0,
                    inntektsreduksjon = 0,
                    personIdent = saksbehandling.ident
                )
            }
    }

    private fun validerRiktigResultattypeForInnvilgetBarnetilsyn(
        beløpsperioder: List<BeløpsperiodeBarnetilsynDto>,
        vedtak: InnvilgelseBarnetilsyn
    ) {
        if (beløpsperioder.all { it.beregningsgrunnlag.kontantstøttebeløp >= it.beregningsgrunnlag.utgifter }) {
            brukerfeilHvis(vedtak.resultatType == ResultatType.INNVILGE) {
                "Kontantstøttebeløp overstiger utgiftsbeløp for alle perioder - kan ikke innvilge. Husk å trykk Beregn før du lagrer vedtaket."
            }
        } else {
            brukerfeilHvis(vedtak.resultatType == ResultatType.INNVILGE_UTEN_UTBETALING) {
                "Vedtaket har ugyldig resultattype. Husk å trykk Beregn før du lagrer vedtaket."
            }
        }
    }

    fun slåSammenAndelerSomSkalVidereføres(
        beløpsperioder: List<AndelTilkjentYtelse>,
        forrigeTilkjentYtelse: TilkjentYtelse,
        opphørsperioder: List<Månedsperiode>
    ): List<AndelTilkjentYtelse> {
        val fomPerioder = beløpsperioder.firstOrNull()?.periode?.fom ?: LocalDate.MAX
        val fomOpphørPerioder = opphørsperioder.firstOrNull()?.fomDato ?: LocalDate.MAX
        val nyePerioderUtenOpphør =
            forrigeTilkjentYtelse.taMedAndelerFremTilDato(minOf(fomPerioder, fomOpphørPerioder)) + beløpsperioder
        return vurderPeriodeForOpphør(nyePerioderUtenOpphør, opphørsperioder)
    }

    private fun andelerForSanksjonertRevurdering(
        forrigeTilkjenteYtelse: TilkjentYtelse,
        vedtak: Sanksjonert
    ): List<AndelTilkjentYtelse> {
        val andelerTilkjentYtelse = vurderPeriodeForOpphør(
            forrigeTilkjenteYtelse.andelerTilkjentYtelse,
            listOf(vedtak.periode.tilPeriode())
        )
        brukerfeilHvis(andelerTilkjentYtelse.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }
        return andelerTilkjentYtelse
    }

    fun vurderPeriodeForOpphør(
        andelTilkjentYtelser: List<AndelTilkjentYtelse>,
        opphørsperioder: List<Månedsperiode>
    ): List<AndelTilkjentYtelse> {
        return andelTilkjentYtelser.map {
            val tilkjentPeriode = it.periode.toMånedsperiode()
            if (opphørsperioder.none { periode -> periode.overlapper(tilkjentPeriode) }) {
                listOf(it)
            } else if (opphørsperioder.any { periode -> periode.inneholder(tilkjentPeriode) }) {
                listOf()
            } else {
                val overlappendeOpphør = opphørsperioder.first { periode -> periode.overlapper(tilkjentPeriode) }

                if (overlappendeOpphør.overlapperKunIStartenAv(tilkjentPeriode)) {
                    vurderPeriodeForOpphør(
                        listOf(it.copy(periode = tilkjentPeriode.copy(fom = overlappendeOpphør.tom.plusMonths(1)).toDatoperiode())),
                        opphørsperioder
                    )
                } else if (overlappendeOpphør.overlapperKunISluttenAv(tilkjentPeriode)) {
                    vurderPeriodeForOpphør(
                        listOf(it.copy(periode = tilkjentPeriode.copy(tom = overlappendeOpphør.fom.minusMonths(1)).toDatoperiode())),
                        opphørsperioder
                    )
                } else { // periode blir delt i to av opphold.
                    vurderPeriodeForOpphør(
                        listOf(
                            it.copy(periode = tilkjentPeriode.copy(tom = overlappendeOpphør.fom.minusMonths(1)).toDatoperiode()),
                            it.copy(periode = tilkjentPeriode.copy(fom = overlappendeOpphør.tom.plusMonths(1)).toDatoperiode())
                        ),
                        opphørsperioder
                    )
                }
            }
        }.flatten()
    }

    private fun andelerForOpphør(
        forrigeTilkjentYtelse: TilkjentYtelse,
        opphørFom: YearMonth
    ): List<AndelTilkjentYtelse> {
        brukerfeilHvis(
            forrigeTilkjentYtelse.andelerTilkjentYtelse.maxOfOrNull { it.periode.tom }?.isBefore(opphørFom.atDay(1)) ?: false
        ) {
            "Kan ikke opphøre frem i tiden"
        }

        brukerfeilHvis(
            forrigeTilkjentYtelse.andelerTilkjentYtelse.isEmpty() &&
                forrigeTilkjentYtelse.startmåned <= opphørFom
        ) {
            "Forrige vedtak er allerede opphørt fra ${forrigeTilkjentYtelse.startmåned}"
        }

        return forrigeTilkjentYtelse.taMedAndelerFremTilDato(opphørFom.atDay(1))
    }

    private fun hentForrigeTilkjenteYtelse(saksbehandling: Saksbehandling): TilkjentYtelse {
        val forrigeBehandlingId = saksbehandling.forrigeBehandlingId
            ?: error("Finner ikke forrige behandling til behandling=${saksbehandling.id}")
        return tilkjentYtelseService.hentForBehandling(forrigeBehandlingId)
    }
}
