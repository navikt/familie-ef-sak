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
import no.nav.familie.ef.sak.oppfølgingsoppgave.OppfølgingsoppgaveService
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseSkolepenger
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.OpphørSkolepenger
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonert
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakSkolepengerDto
import no.nav.familie.ef.sak.vedtak.dto.erSammenhengende
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import no.nav.familie.ef.sak.vedtak.historikk.erAktivVedtaksperiode
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
    private val oppfølgingsoppgaveService: OppfølgingsoppgaveService,
) : BehandlingSteg<VedtakDto> {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val midlertidigOpphørFeilmelding = "Kan ikke starte vedtaket med opphørsperiode for en førstegangsbehandling"

    override fun stegType(): StegType = StegType.BEREGNE_YTELSE

    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: VedtakDto,
    ) {
        validerStønadstype(saksbehandling, data)
        validerGyldigeVedtaksperioder(saksbehandling, data)

        val aktivIdent = fagsakService.fagsakMedOppdatertPersonIdent(saksbehandling.fagsakId).hentAktivIdent()
        val saksbehandlingMedOppdatertIdent = saksbehandling.copy(ident = aktivIdent)
        nullstillEksisterendeVedtakPåBehandling(saksbehandlingMedOppdatertIdent.id)
        vedtakService.lagreVedtak(
            vedtakDto = data,
            behandlingId = saksbehandlingMedOppdatertIdent.id,
            stønadstype = saksbehandlingMedOppdatertIdent.stønadstype,
        )

        when (data) {
            is InnvilgelseOvergangsstønad -> {
                validerGyldigStartperiodeForOvergangsstønad(data, saksbehandlingMedOppdatertIdent)
                validerOmregningService.validerHarSammePerioderSomTidligereVedtak(data, saksbehandlingMedOppdatertIdent)
                validerSanksjoner(data, saksbehandlingMedOppdatertIdent)
                opprettTilkjentYtelseForInnvilgetOvergangsstønad(data, saksbehandlingMedOppdatertIdent)
                validerOmregningService.validerHarGammelGOgKanLagres(saksbehandlingMedOppdatertIdent)
                simuleringService.hentOgLagreSimuleringsresultat(saksbehandlingMedOppdatertIdent)
            }

            is InnvilgelseBarnetilsyn -> {
                validerGyldigStartperiodeForBarnetilsyn(data, saksbehandlingMedOppdatertIdent)
                validerSanksjoner(data, saksbehandlingMedOppdatertIdent)
                opprettTilkjentYtelseForInnvilgetBarnetilsyn(data, saksbehandlingMedOppdatertIdent)
                simuleringService.hentOgLagreSimuleringsresultat(saksbehandlingMedOppdatertIdent)
            }

            is VedtakSkolepengerDto -> {
                opprettTilkjentYtelseForInnvilgetSkolepenger(data, saksbehandlingMedOppdatertIdent)
                simuleringService.hentOgLagreSimuleringsresultat(saksbehandlingMedOppdatertIdent)
            }

            is Opphør -> {
                oppfølgingsoppgaveService.slettOppfølgingsoppgave(saksbehandling.id)
                validerStartTidEtterSanksjon(data.opphørFom, saksbehandlingMedOppdatertIdent)
                opprettTilkjentYtelseForOpphørtBehandling(saksbehandlingMedOppdatertIdent, data)
                simuleringService.hentOgLagreSimuleringsresultat(saksbehandlingMedOppdatertIdent)
            }

            is Avslå -> {
                oppfølgingsoppgaveService.slettOppfølgingsoppgave(saksbehandling.id)
                simuleringService.slettSimuleringForBehandling(saksbehandlingMedOppdatertIdent)
                tilbakekrevingService.slettTilbakekreving(saksbehandlingMedOppdatertIdent.id)
            }

            is Sanksjonert -> {
                oppfølgingsoppgaveService.slettOppfølgingsoppgave(saksbehandling.id)
                opprettTilkjentYtelseForSanksjonertBehandling(data, saksbehandlingMedOppdatertIdent)
            }
        }
    }

    private fun validerGyldigStartperiodeForOvergangsstønad(
        vedtak: InnvilgelseOvergangsstønad,
        behandling: Saksbehandling,
    ) {
        if (behandling.type == FØRSTEGANGSBEHANDLING) {
            val starterMedMidlertidigOpphør = vedtak.perioder.firstOrNull()?.erMidlertidigOpphørEllerSanksjon() == true
            feilHvis(starterMedMidlertidigOpphør) { midlertidigOpphørFeilmelding }
        }
    }

    private fun validerGyldigStartperiodeForBarnetilsyn(
        vedtak: InnvilgelseBarnetilsyn,
        behandling: Saksbehandling,
    ) {
        if (behandling.type == FØRSTEGANGSBEHANDLING) {
            val starterMedMidlertidigOpphør = vedtak.perioder.firstOrNull()?.erMidlertidigOpphørEllerSanksjon == true
            feilHvis(starterMedMidlertidigOpphør) { midlertidigOpphørFeilmelding }
        }
    }

    private fun validerSanksjoner(
        innvilget: InnvilgelseBarnetilsyn,
        behandling: Saksbehandling,
    ) {
        if (behandling.erMaskinellOmregning) {
            return
        }

        val nyeSanksjonsperioder =
            innvilget.perioder
                .filter { it.periodetype == PeriodetypeBarnetilsyn.SANKSJON_1_MND }
                .map { it.periode to (it.sanksjonsårsak ?: error("Mangler sanksjonsårsak")) }
        validerHarIkkeLagtTilSanksjonsperioder(behandling, nyeSanksjonsperioder)
    }

    private fun validerSanksjoner(
        innvilget: InnvilgelseOvergangsstønad,
        behandling: Saksbehandling,
    ) {
        if (behandling.erMaskinellOmregning) {
            return
        }

        val nyeSanksjonsperioder =
            innvilget.perioder
                .filter { it.periodeType == VedtaksperiodeType.SANKSJON }
                .map { it.periode to (it.sanksjonsårsak ?: error("Mangler sanksjonsårsak")) }
        validerHarIkkeLagtTilSanksjonsperioder(behandling, nyeSanksjonsperioder)
    }

    private fun validerHarIkkeLagtTilSanksjonsperioder(
        behandling: Saksbehandling,
        nyeSanksjonsperioder: List<Pair<Månedsperiode, Sanksjonsårsak>>,
    ) {
        val historikk = andelsHistorikkService.hentHistorikk(behandling.fagsakId, null)
        val historiskeSanksjonsperioder =
            historikk
                .filter { it.erAktivVedtaksperiode() }
                .filter { it.erSanksjon }
                .map { it.andel.periode to it.sanksjonsårsak }
                .toSet()

        val nySanksjonsperiodeUtenTreff = nyeSanksjonsperioder.find { !historiskeSanksjonsperioder.contains(it) }
        feilHvis(nySanksjonsperiodeUtenTreff != null) {
            logger.error("Ny sanksjonsperiode uten treff=$nySanksjonsperiodeUtenTreff historikk=$historiskeSanksjonsperioder")
            "Nye eller endrede sanksjonsperioder ($nySanksjonsperiodeUtenTreff) som ikke finnes i historikken"
        }
    }

    private fun validerStartTidEtterSanksjon(
        vedtakFom: YearMonth,
        behandling: Saksbehandling,
    ) {
        val nyesteSanksjonsperiode =
            andelsHistorikkService
                .hentHistorikk(behandling.fagsakId, null)
                .filter { it.erAktivVedtaksperiode() }
                .lastOrNull {
                    it.periodeType == VedtaksperiodeType.SANKSJON ||
                        it.periodetypeBarnetilsyn == PeriodetypeBarnetilsyn.SANKSJON_1_MND
                }
        nyesteSanksjonsperiode?.andel?.stønadFra?.let { sanksjonsdato ->
            feilHvis(sanksjonsdato >= vedtakFom.atDay(1)) {
                "Systemet støtter ikke revurdering før sanksjonsperioden. Kontakt brukerstøtte for videre bistand"
            }
        }
    }

    private fun validerGyldigeVedtaksperioder(
        saksbehandling: Saksbehandling,
        data: VedtakDto,
    ) {
        if (data is InnvilgelseOvergangsstønad) {
            val harOpphørsperioder = data.perioder.any { it.periodeType == VedtaksperiodeType.MIDLERTIDIG_OPPHØR }
            val harInnvilgedePerioder = data.perioder.any { !it.erMidlertidigOpphørEllerSanksjon() }
            brukerfeilHvis(harOpphørsperioder && !harInnvilgedePerioder) {
                "Må ha innvilgelsesperioder i tillegg til opphørsperioder"
            }
            brukerfeilHvis(
                !saksbehandling.erMigrering &&
                    !saksbehandling.erOmregning &&
                    harPeriodeEllerAktivitetMigrering(data),
            ) {
                "Kan ikke inneholde aktivitet eller periode av type migrering"
            }
            val perioderFørFødsel = data.perioder.filter { it.periodeType === VedtaksperiodeType.PERIODE_FØR_FØDSEL }
            brukerfeilHvis(perioderFørFødsel.sumOf { it.periode.lengdeIHeleMåneder() } > 4) {
                "Vedtaket kan ikke inneholde mer enn 4 måneder med periodetypen: \"periode før fødsel\""
            }
        }
        if (data is InnvilgelseBarnetilsyn) {
            barnService.validerBarnFinnesPåBehandling(saksbehandling.id, data.perioder.flatMap { it.barn }.toSet())
            validerInnvilgelseBarnetilsyn(data.perioder, saksbehandling)
        }
    }

    private fun validerInnvilgelseBarnetilsyn(
        utgiftsperioder: List<UtgiftsperiodeDto>,
        saksbehandling: Saksbehandling,
    ) {
        validerAntallBarnOgUtgifterVedMidlertidigOpphør(utgiftsperioder, saksbehandling.id)
        validerTidligereVedtakVedMidlertidigOpphør(utgiftsperioder, saksbehandling)
        validerSammenhengendePerioder(utgiftsperioder)
    }

    private fun validerAntallBarnOgUtgifterVedMidlertidigOpphør(
        utgiftsperioder: List<UtgiftsperiodeDto>,
        behandlingId: UUID,
    ) {
        brukerfeilHvis(utgiftsperioder.any { it.erMidlertidigOpphørEllerSanksjon && it.barn.isNotEmpty() }) {
            "Kan ikke ta med barn på en periode som er et midlertidig opphør eller sanksjon, på behandling=$behandlingId"
        }
        brukerfeilHvis(utgiftsperioder.any { it.erMidlertidigOpphørEllerSanksjon && it.utgifter > 0 }) {
            "Kan ikke ha utgifter større enn null på en periode som er et midlertidig opphør eller sanksjon, på behandling=$behandlingId"
        }
        brukerfeilHvis(utgiftsperioder.any { !it.erMidlertidigOpphørEllerSanksjon && it.barn.isEmpty() }) {
            "Må ha med minst et barn på en periode som ikke er et midlertidig opphør eller sanksjon, på behandling=$behandlingId"
        }
        brukerfeilHvis(utgiftsperioder.any { !it.erMidlertidigOpphørEllerSanksjon && it.utgifter <= 0 }) {
            "Kan ikke ha null utgifter på en periode som ikke er et midlertidig opphør eller sanksjon, på behandling=$behandlingId"
        }
    }

    private fun validerTidligereVedtakVedMidlertidigOpphør(
        utgiftsperioder: List<UtgiftsperiodeDto>,
        saksbehandling: Saksbehandling,
    ) {
        val førstePeriodeErMidlertidigOpphør = utgiftsperioder.first().periodetype == PeriodetypeBarnetilsyn.OPPHØR
        brukerfeilHvis(førstePeriodeErMidlertidigOpphør && saksbehandling.forrigeBehandlingId == null) {
            "Første periode kan ikke være en opphørsperiode, på førstegangsbehandling=${saksbehandling.id}"
        }
        val harIkkeInnvilgetBeløp =
            if (saksbehandling.forrigeBehandlingId != null) tilkjentYtelseService.hentForBehandling(saksbehandling.forrigeBehandlingId).andelerTilkjentYtelse.all { it.beløp == 0 } else true
        brukerfeilHvis(harIkkeInnvilgetBeløp && førstePeriodeErMidlertidigOpphør) {
            "Første periode kan ikke ha et nullbeløp dersom det ikke har blitt innvilget beløp på et tidligere vedtak, på behandling=${saksbehandling.id}"
        }
    }

    private fun validerSammenhengendePerioder(utgiftsperioder: List<UtgiftsperiodeDto>) {
        brukerfeilHvis(!utgiftsperioder.erSammenhengende()) {
            "Periodene må være sammenhengende"
        }
    }

    private fun validerStønadstype(
        saksbehandling: Saksbehandling,
        data: VedtakDto,
    ) {
        when (saksbehandling.stønadstype) {
            StønadType.OVERGANGSSTØNAD -> {
                validerGyldigeVedtakstyper(
                    saksbehandling.stønadstype,
                    data,
                    InnvilgelseOvergangsstønad::class,
                    Avslå::class,
                    Opphør::class,
                    Sanksjonert::class,
                )
            }

            StønadType.BARNETILSYN -> {
                validerGyldigeVedtakstyper(
                    saksbehandling.stønadstype,
                    data,
                    InnvilgelseBarnetilsyn::class,
                    Avslå::class,
                    Opphør::class,
                    Sanksjonert::class,
                )
            }

            StønadType.SKOLEPENGER -> {
                validerGyldigeVedtakstyper(
                    saksbehandling.stønadstype,
                    data,
                    InnvilgelseSkolepenger::class,
                    Avslå::class,
                    OpphørSkolepenger::class,
                )
            }
        }
    }

    private fun validerGyldigeVedtakstyper(
        stønadstype: StønadType,
        data: VedtakDto,
        vararg vedtakstype: KClass<out VedtakDto>,
    ) {
        feilHvis(vedtakstype.none { it.isInstance(data) }) {
            "Stønadstype=$stønadstype har ikke støtte for ${data.javaClass.simpleName}"
        }
    }

    private fun harPeriodeEllerAktivitetMigrering(data: InnvilgelseOvergangsstønad) = data.perioder.any { it.periodeType == VedtaksperiodeType.MIGRERING || it.aktivitet == AktivitetType.MIGRERING }

    private fun nullstillEksisterendeVedtakPåBehandling(behandlingId: UUID) {
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(behandlingId)
        vedtakService.slettVedtakHvisFinnes(behandlingId)
    }

    private fun opprettTilkjentYtelseForOpphørtBehandling(
        saksbehandling: Saksbehandling,
        vedtak: Opphør,
    ) {
        brukerfeilHvis(saksbehandling.type != REVURDERING) { "Kan kun opphøre ved revurdering" }
        val opphørsdato = vedtak.opphørFom.atDay(1)
        val forrigeTilkjenteYtelse = hentForrigeTilkjenteYtelse(saksbehandling)
        val nyeAndeler = andelerForOpphør(forrigeTilkjenteYtelse, opphørsdato)
        val nyStartdato = beregnNyttStartdatoForRevurdering(nyeAndeler, opphørsdato, forrigeTilkjenteYtelse)
        tilkjentYtelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                personident = saksbehandling.ident,
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = nyeAndeler,
                samordningsfradragType = null,
                startdato = nyStartdato,
                grunnbeløpsmåned = forrigeTilkjenteYtelse.grunnbeløpsmåned,
            ),
        )
    }

    private fun beregnNyttStartdatoForRevurdering(
        nyeAndeler: List<AndelTilkjentYtelse>,
        opphørsdato: LocalDate,
        forrigeTilkjenteYtelse: TilkjentYtelse,
    ): LocalDate {
        val opphørsdatoHvisFørTidligereAndeler = if (nyeAndeler.isEmpty()) opphørsdato else null
        @Suppress("FoldInitializerAndIfToElvis")
        if (opphørsdatoHvisFørTidligereAndeler == null) return forrigeTilkjenteYtelse.startdato

        return minOf(opphørsdatoHvisFørTidligereAndeler, forrigeTilkjenteYtelse.startdato)
    }

    private fun opprettTilkjentYtelseForInnvilgetOvergangsstønad(
        vedtak: InnvilgelseOvergangsstønad,
        saksbehandling: Saksbehandling,
    ) {
        brukerfeilHvis(!saksbehandling.erOmregning && !vedtak.perioder.map { it.periode }.erSammenhengende()) {
            "Periodene må være sammenhengende"
        }

        val andelerTilkjentYtelse: List<AndelTilkjentYtelse> =
            lagBeløpsperioderForInnvilgelseOvergangsstønad(vedtak, saksbehandling)
        brukerfeilHvis(andelerTilkjentYtelse.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }

        val (nyeAndeler, startdato) =
            when (saksbehandling.type) {
                FØRSTEGANGSBEHANDLING -> {
                    andelerTilkjentYtelse to startdatoForFørstegangsbehandling(andelerTilkjentYtelse)
                }

                REVURDERING -> {
                    nyeAndelerForRevurderingAvOvergangsstønadMedStartdato(
                        saksbehandling,
                        vedtak,
                        andelerTilkjentYtelse,
                    )
                }
            }

        tilkjentYtelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                personident = saksbehandling.ident,
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = nyeAndeler,
                samordningsfradragType = vedtak.samordningsfradragType,
                startdato = startdato,
            ),
        )
    }

    private fun opprettTilkjentYtelseForInnvilgetBarnetilsyn(
        vedtak: InnvilgelseBarnetilsyn,
        saksbehandling: Saksbehandling,
    ) {
        // TODO: Må periodene være sammenhengende?
        //  brukerfeilHvis(!vedtak.perioder.erSammenhengende()) { "Periodene må være sammenhengende" }
        val andelerTilkjentYtelse: List<AndelTilkjentYtelse> =
            lagBeløpsperioderForInnvilgelseBarnetilsyn(vedtak, saksbehandling)
        brukerfeilHvis(andelerTilkjentYtelse.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }

        val (nyeAndeler, startdato) =
            when (saksbehandling.type) {
                FØRSTEGANGSBEHANDLING -> {
                    andelerTilkjentYtelse to startdatoForFørstegangsbehandling(andelerTilkjentYtelse)
                }

                REVURDERING -> {
                    nyeAndelerForRevurderingAvBarnetilsynMedStartdato(
                        saksbehandling,
                        vedtak,
                        andelerTilkjentYtelse,
                    )
                }
            }

        tilkjentYtelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                personident = saksbehandling.ident,
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = nyeAndeler,
                startdato = startdato,
            ),
        )
    }

    private fun opprettTilkjentYtelseForInnvilgetSkolepenger(
        vedtak: VedtakSkolepengerDto,
        saksbehandling: Saksbehandling,
    ) {
        val forrigeTilkjentYtelse =
            saksbehandling.forrigeBehandlingId?.let { forrigeBehandlingId ->
                tilkjentYtelseService.hentForBehandling(forrigeBehandlingId)
            }
        val andelerTilkjentYtelse = lagBeløpsperioderForInnvilgelseSkolepenger(vedtak, saksbehandling)
        validerSkolepenger(saksbehandling, vedtak, andelerTilkjentYtelse, forrigeTilkjentYtelse)
        val (nyeAndeler, startdato) =
            when (saksbehandling.type) {
                FØRSTEGANGSBEHANDLING -> {
                    andelerTilkjentYtelse to startdatoForFørstegangsbehandling(andelerTilkjentYtelse)
                }

                // Burde kanskje summere tidligere forbrukt fra andeler, per skoleår
                REVURDERING -> {
                    val startdatoNyeAndeler = andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
                    val nyttStartdato =
                        min(forrigeTilkjentYtelse?.startdato, startdatoNyeAndeler)
                            ?: error("Må ha startdato fra forrige behandling eller sende inn andeler")
                    andelerTilkjentYtelse to nyttStartdato
                }
            }

        tilkjentYtelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                personident = saksbehandling.ident,
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = nyeAndeler,
                startdato = startdato,
            ),
        )
    }

    private fun validerSkolepenger(
        saksbehandling: Saksbehandling,
        vedtak: VedtakSkolepengerDto,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        forrigeTilkjentYtelse: TilkjentYtelse?,
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
                vedtak.erOpphør(),
        ) {
            "Kan ikke opphøre når det ikke finnes noen perioder å opphøre"
        }
    }

    private fun startdatoForFørstegangsbehandling(andelerTilkjentYtelse: List<AndelTilkjentYtelse>): LocalDate =
        andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
            ?: error("Må ha med en periode i førstegangsbehandling")

    private fun nyeAndelerForRevurderingAvOvergangsstønadMedStartdato(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseOvergangsstønad,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    ): Pair<List<AndelTilkjentYtelse>, LocalDate> {
        val opphørsperioder = finnOpphørsperioder(vedtak)

        val forrigeTilkjenteYtelse =
            saksbehandling.forrigeBehandlingId?.let { hentForrigeTilkjenteYtelse(saksbehandling) }
        validerOpphørsperioder(opphørsperioder, finnInnvilgedePerioder(vedtak), forrigeTilkjenteYtelse)

        val nyeAndeler = beregnNyeAndelerForRevurdering(forrigeTilkjenteYtelse, andelerTilkjentYtelse, opphørsperioder)

        val forrigeStartdato = forrigeTilkjenteYtelse?.startdato
        val startdato = nyttStartdato(saksbehandling.id, vedtak.perioder.tilPerioder(), forrigeStartdato)
        return nyeAndeler to startdato
    }

    private fun nyeAndelerForRevurderingAvBarnetilsynMedStartdato(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseBarnetilsyn,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    ): Pair<List<AndelTilkjentYtelse>, LocalDate> {
        val opphørsperioder = finnOpphørsperioder(vedtak)

        val forrigeTilkjenteYtelse =
            saksbehandling.forrigeBehandlingId?.let { hentForrigeTilkjenteYtelse(saksbehandling) }
        validerOpphørsperioder(opphørsperioder, finnInnvilgedePerioder(vedtak), forrigeTilkjenteYtelse)

        val nyeAndeler = beregnNyeAndelerForRevurdering(forrigeTilkjenteYtelse, andelerTilkjentYtelse, opphørsperioder)

        val forrigeStartdato = forrigeTilkjenteYtelse?.startdato
        val startdato = nyttStartdato(saksbehandling.id, vedtak.perioder.tilPerioder(), forrigeStartdato)
        return nyeAndeler to startdato
    }

    private fun validerOpphørsperioder(
        opphørsperioder: List<Månedsperiode>,
        vedtaksperioder: List<Månedsperiode>,
        forrigeTilkjenteYtelse: TilkjentYtelse?,
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
        opphørsperioder: List<Månedsperiode>,
    ) = forrigeTilkjenteYtelse?.let {
        slåSammenAndelerSomSkalVidereføres(andelerTilkjentYtelse, forrigeTilkjenteYtelse, opphørsperioder)
    } ?: andelerTilkjentYtelse

    private fun nyttStartdato(
        behandlingId: UUID,
        perioder: List<Månedsperiode>,
        forrigeStartdato: LocalDate?,
    ): LocalDate {
        val startdato = min(perioder.minOfOrNull { it.fomDato }, forrigeStartdato)
        feilHvis(startdato == null) {
            "Klarer ikke å beregne startdato for behandling=$behandlingId"
        }
        return startdato
    }

    private fun opprettTilkjentYtelseForSanksjonertBehandling(
        vedtak: Sanksjonert,
        behandling: Saksbehandling,
    ) {
        brukerfeilHvis(behandling.forrigeBehandlingId == null) {
            "Kan ikke opprette sanksjon når det ikke finnes en tidligere behandling"
        }
        val erAlleredeSanksjonertOppgittMåned =
            andelsHistorikkService
                .hentHistorikk(behandling.fagsakId, null)
                .filter { it.erAktivVedtaksperiode() }
                .any { it.erSanksjon && it.andel.periode == vedtak.periode.tilPeriode() }
        brukerfeilHvis(erAlleredeSanksjonertOppgittMåned) {
            "Behandlingen er allerede sanksjonert ${vedtak.periode.fom}"
        }

        val forrigeTilkjenteYtelse = hentForrigeTilkjenteYtelse(behandling)
        val andelerTilkjentYtelse = andelerForSanksjonertRevurdering(forrigeTilkjenteYtelse, vedtak)

        tilkjentYtelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                personident = behandling.ident,
                behandlingId = behandling.id,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                startdato = forrigeTilkjenteYtelse.startdato,
            ),
        )
    }

    private fun finnOpphørsperioder(vedtak: InnvilgelseOvergangsstønad) = vedtak.perioder.filter { it.periodeType == VedtaksperiodeType.MIDLERTIDIG_OPPHØR }.tilPerioder()

    private fun finnOpphørsperioder(vedtak: InnvilgelseBarnetilsyn) = vedtak.perioder.filter { it.utgifter == 0 }.tilPerioder()

    private fun finnInnvilgedePerioder(vedtak: InnvilgelseOvergangsstønad) =
        vedtak.perioder
            .filterNot { it.erMidlertidigOpphørEllerSanksjon() }
            .tilPerioder()

    private fun finnInnvilgedePerioder(vedtak: InnvilgelseBarnetilsyn) = vedtak.perioder.filter { it.utgifter != 0 }.tilPerioder()

    private fun lagBeløpsperioderForInnvilgelseOvergangsstønad(
        vedtak: InnvilgelseOvergangsstønad,
        saksbehandling: Saksbehandling,
    ) = beregningService
        .beregnYtelse(finnInnvilgedePerioder(vedtak), vedtak.inntekter.tilInntektsperioder())
        .map {
            AndelTilkjentYtelse(
                beløp = it.beløp.toInt(),
                periode = it.periode,
                kildeBehandlingId = saksbehandling.id,
                personIdent = saksbehandling.ident,
                samordningsfradrag = it.beregningsgrunnlag?.samordningsfradrag?.toInt() ?: 0,
                inntekt = it.beregningsgrunnlag?.inntekt?.toInt() ?: 0,
                inntektsreduksjon = it.beregningsgrunnlag?.avkortningPerMåned?.toInt() ?: 0,
            )
        }

    private fun lagBeløpsperioderForInnvilgelseBarnetilsyn(
        vedtak: InnvilgelseBarnetilsyn,
        saksbehandling: Saksbehandling,
    ): List<AndelTilkjentYtelse> {
        val beløpsperioder =
            beregningBarnetilsynService.beregnYtelseBarnetilsyn(
                vedtak.perioder.filter { !it.erMidlertidigOpphørEllerSanksjon },
                kontantstøttePerioder = vedtak.perioderKontantstøtte,
                tilleggsstønadsperioder = vedtak.tilleggsstønad.perioder,
                erMigrering = saksbehandling.erMigrering,
            )
        validerRiktigResultattypeForInnvilgetBarnetilsyn(beløpsperioder, vedtak)
        return beløpsperioder
            .map {
                AndelTilkjentYtelse(
                    beløp = it.beløp,
                    periode = it.periode,
                    kildeBehandlingId = saksbehandling.id,
                    inntekt = 0,
                    samordningsfradrag = 0,
                    inntektsreduksjon = 0,
                    personIdent = saksbehandling.ident,
                )
            }
    }

    private fun lagBeløpsperioderForInnvilgelseSkolepenger(
        vedtak: VedtakSkolepengerDto,
        saksbehandling: Saksbehandling,
    ): List<AndelTilkjentYtelse> =
        beregningSkolepengerService
            .beregnYtelse(
                vedtak.skoleårsperioder,
                saksbehandling.id,
                vedtak.erOpphør(),
            ).perioder
            .filter { it.beløp > 0 }
            .map {
                AndelTilkjentYtelse(
                    beløp = it.beløp,
                    stønadFom = it.årMånedFra.atDay(1),
                    stønadTom = it.årMånedFra.atEndOfMonth(),
                    kildeBehandlingId = saksbehandling.id,
                    inntekt = 0,
                    samordningsfradrag = 0,
                    inntektsreduksjon = 0,
                    personIdent = saksbehandling.ident,
                )
            }

    private fun validerRiktigResultattypeForInnvilgetBarnetilsyn(
        beløpsperioder: List<BeløpsperiodeBarnetilsynDto>,
        vedtak: InnvilgelseBarnetilsyn,
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
        opphørsperioder: List<Månedsperiode>,
    ): List<AndelTilkjentYtelse> {
        val fomPerioder = beløpsperioder.firstOrNull()?.stønadFom ?: LocalDate.MAX
        val fomOpphørPerioder = opphørsperioder.firstOrNull()?.fomDato ?: LocalDate.MAX
        val nyePerioderUtenOpphør =
            forrigeTilkjentYtelse.taMedAndelerFremTilDato(minOf(fomPerioder, fomOpphørPerioder)) + beløpsperioder
        return vurderPeriodeForOpphør(nyePerioderUtenOpphør, opphørsperioder)
    }

    private fun andelerForSanksjonertRevurdering(
        forrigeTilkjenteYtelse: TilkjentYtelse,
        vedtak: Sanksjonert,
    ): List<AndelTilkjentYtelse> {
        val andelerTilkjentYtelse =
            vurderPeriodeForOpphør(
                forrigeTilkjenteYtelse.andelerTilkjentYtelse,
                listOf(vedtak.periode.tilPeriode()),
            )
        brukerfeilHvis(andelerTilkjentYtelse.isEmpty()) { "Innvilget vedtak må ha minimum en beløpsperiode" }
        return andelerTilkjentYtelse
    }

    fun vurderPeriodeForOpphør(
        andelTilkjentYtelser: List<AndelTilkjentYtelse>,
        opphørsperioder: List<Månedsperiode>,
    ): List<AndelTilkjentYtelse> =
        andelTilkjentYtelser
            .map {
                val tilkjentPeriode = it.periode
                if (opphørsperioder.none { periode -> periode.overlapper(tilkjentPeriode) }) {
                    listOf(it)
                } else if (opphørsperioder.any { periode -> periode.inneholder(tilkjentPeriode) }) {
                    listOf()
                } else {
                    val overlappendeOpphør = opphørsperioder.first { periode -> periode.overlapper(tilkjentPeriode) }

                    if (overlappendeOpphør.overlapperKunIStartenAv(tilkjentPeriode)) {
                        vurderPeriodeForOpphør(
                            listOf(it.copy(stønadFom = overlappendeOpphør.tomDato.plusDays(1))),
                            opphørsperioder,
                        )
                    } else if (overlappendeOpphør.overlapperKunISluttenAv(tilkjentPeriode)) {
                        vurderPeriodeForOpphør(
                            listOf(it.copy(stønadTom = overlappendeOpphør.fomDato.minusDays(1))),
                            opphørsperioder,
                        )
                    } else { // periode blir delt i to av opphold.
                        vurderPeriodeForOpphør(
                            listOf(
                                it.copy(stønadTom = overlappendeOpphør.fomDato.minusDays(1)),
                                it.copy(stønadFom = overlappendeOpphør.tomDato.plusDays(1)),
                            ),
                            opphørsperioder,
                        )
                    }
                }
            }.flatten()

    private fun andelerForOpphør(
        forrigeTilkjentYtelse: TilkjentYtelse,
        opphørFom: LocalDate,
    ): List<AndelTilkjentYtelse> {
        brukerfeilHvis(
            forrigeTilkjentYtelse.andelerTilkjentYtelse.maxOfOrNull { it.stønadTom }?.isBefore(opphørFom) ?: false,
        ) {
            "Kan ikke opphøre fra et tidspunkt bruker ikke har stønad."
        }

        brukerfeilHvis(
            forrigeTilkjentYtelse.andelerTilkjentYtelse.isEmpty() &&
                forrigeTilkjentYtelse.startdato <= opphørFom,
        ) {
            "Forrige vedtak er allerede opphørt fra ${forrigeTilkjentYtelse.startdato}"
        }

        return forrigeTilkjentYtelse.taMedAndelerFremTilDato(opphørFom)
    }

    private fun hentForrigeTilkjenteYtelse(saksbehandling: Saksbehandling): TilkjentYtelse {
        val forrigeBehandlingId =
            saksbehandling.forrigeBehandlingId
                ?: error("Finner ikke forrige behandling til behandling=${saksbehandling.id}")
        return tilkjentYtelseService.hentForBehandling(forrigeBehandlingId)
    }
}
