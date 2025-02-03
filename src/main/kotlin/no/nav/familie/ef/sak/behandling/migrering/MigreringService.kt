package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.behandlingsflyt.task.SjekkMigrertStatusIInfotrygdTask
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.dto.MigrerRequestDto
import no.nav.familie.ef.sak.fagsak.dto.MigreringInfo
import no.nav.familie.ef.sak.felles.util.DatoUtil
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infotrygd.InfotrygdStønadPerioderDto
import no.nav.familie.ef.sak.infotrygd.SummertInfotrygdPeriodeDto
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdAktivitetstype
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

@Service
class MigreringService(
    private val taskService: TaskService,
    private val fagsakService: FagsakService,
    private val fagsakPersonService: FagsakPersonService,
    private val behandlingService: BehandlingService,
    private val iverksettService: IverksettService,
    private val iverksettClient: IverksettClient,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val vurderingService: VurderingService,
    private val beregnYtelseSteg: BeregnYtelseSteg,
    private val iverksettingDtoMapper: IverksettingDtoMapper,
    private val featureToggleService: FeatureToggleService,
    private val infotrygdService: InfotrygdService,
    private val beregningService: BeregningService,
    private val simuleringService: SimuleringService,
    private val infotrygdPeriodeValideringService: InfotrygdPeriodeValideringService,
    private val barnRepository: BarnRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentMigreringInfo(
        fagsakPersonId: UUID,
        kjøremåned: YearMonth = kjøremåned(),
    ): MigreringInfo {
        val periode =
            try {
                val fagsakPerson = fagsakPersonService.hentPerson(fagsakPersonId)
                hentGjeldendePeriodeOgValiderState(fagsakPerson, StønadType.OVERGANGSSTØNAD, kjøremåned)
            } catch (e: MigreringException) {
                logger.info("Kan ikke migrere fagsakPerson=$fagsakPersonId årsak=${e.type}")
                secureLogger.info("Kan ikke migrere fagsakPerson=$fagsakPersonId - ${e.årsak}")
                return MigreringInfo(
                    kanMigreres = false,
                    e.årsak,
                )
            }
        logger.info("Kan migrere fagsakPerson=$fagsakPersonId")

        val fra = periode.stønadsperiode.fom
        val vedtaksperioder = vedtaksperioder(periode.stønadsperiode, erReellArbeidssøker(periode))
        val inntekter = inntekter(fra, periode.inntektsgrunnlag, periode.samordningsfradrag)
        val beregnYtelse = beregningService.beregnYtelse(vedtaksperioder.tilPerioder(), inntekter.tilInntektsperioder())
        return MigreringInfo(
            kanMigreres = true,
            stønadsperiode = periode.stønadsperiode,
            inntektsgrunnlag = periode.inntektsgrunnlag,
            samordningsfradrag = periode.samordningsfradrag,
            beløpsperioder = beregnYtelse,
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun migrerOvergangsstønadAutomatisk(personIdent: String) {
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, StønadType.OVERGANGSSTØNAD)
        migrerFagsakPerson(fagsak.fagsakPersonId, StønadType.OVERGANGSSTØNAD, kunAktivStønad = true)
    }

    /**
     * Henter data fra infotrygd og oppretter migrering
     */
    @Transactional
    fun migrerOvergangsstønad(
        fagsakPersonId: UUID,
        request: MigrerRequestDto,
    ): UUID {
        try {
            return migrerFagsakPerson(
                fagsakPersonId = fagsakPersonId,
                stønadType = StønadType.OVERGANGSSTØNAD,
                ignorerFeilISimulering = request.ignorerFeilISimulering,
            )
        } catch (e: MigreringException) {
            logger.warn("Kan ikke migrere fagsakPerson=$fagsakPersonId årsak=${e.type}")
            secureLogger.warn("Kan ikke migrere fagsakPerson=$fagsakPersonId - ${e.årsak}")
            throw ApiFeil(e.årsak, HttpStatus.BAD_REQUEST)
        }
    }

    /**
     * Henter data fra infotrygd og oppretter migrering
     */
    @Transactional
    fun migrerBarnetilsyn(
        fagsakPersonId: UUID,
        request: MigrerRequestDto,
    ): UUID {
        brukerfeilHvisIkke(featureToggleService.isEnabled(Toggle.MIGRERING_BARNETILSYN)) {
            "Feature toggle for migrering av barnetilsyn er ikke aktivert"
        }
        try {
            return migrerFagsakPerson(
                fagsakPersonId = fagsakPersonId,
                stønadType = StønadType.BARNETILSYN,
                ignorerFeilISimulering = request.ignorerFeilISimulering,
            )
        } catch (e: MigreringException) {
            logger.warn("Kan ikke migrere fagsakPerson=$fagsakPersonId årsak=${e.type}")
            secureLogger.warn("Kan ikke migrere fagsakPerson=$fagsakPersonId - ${e.årsak}")
            throw ApiFeil(e.årsak, HttpStatus.BAD_REQUEST)
        }
    }

    private fun migrerFagsakPerson(
        fagsakPersonId: UUID,
        stønadType: StønadType,
        kunAktivStønad: Boolean = false,
        ignorerFeilISimulering: Boolean = false,
    ): UUID {
        val fagsakPerson = fagsakPersonService.hentPerson(fagsakPersonId)
        val personIdent = fagsakPerson.hentAktivIdent()
        val kjøremåned = kjøremåned()
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, stønadType)
        val periode = hentGjeldendePeriodeOgValiderState(fagsakPerson, stønadType, kjøremåned)
        if (kunAktivStønad && YearMonth.now() > periode.stønadsperiode.tom) {
            secureLogger.info("Har ikke aktiv stønad $periode")
            throw MigreringException(
                "Har ikke aktiv stønad (${periode.stønadsperiode.tom})",
                MigreringExceptionType.INGEN_AKTIV_STØNAD,
            )
        }
        return when (stønadType) {
            StønadType.OVERGANGSSTØNAD -> opprettMigreringOvergangsstønad(fagsak, periode, ignorerFeilISimulering)
            StønadType.BARNETILSYN -> opprettMigreringBarnetilsyn(fagsak, periode, ignorerFeilISimulering)
            StønadType.SKOLEPENGER -> error("Kan ikke migrere skolepenger")
        }.id
    }

    private fun opprettMigreringOvergangsstønad(
        fagsak: Fagsak,
        periode: SummertInfotrygdPeriodeDto,
        ignorerFeilISimulering: Boolean,
    ) = opprettMigrering(
        fagsak = fagsak,
        periode = periode.stønadsperiode,
        inntektsgrunnlag = periode.inntektsgrunnlag,
        samordningsfradrag = periode.samordningsfradrag,
        erReellArbeidssøker = erReellArbeidssøker(periode),
        ignorerFeilISimulering = ignorerFeilISimulering,
    )

    private fun opprettMigreringBarnetilsyn(
        fagsak: Fagsak,
        periode: SummertInfotrygdPeriodeDto,
        ignorerFeilISimulering: Boolean,
    ) = opprettMigrering(
        fagsak = fagsak,
        periode = periode.stønadsperiode,
        ignorerFeilISimulering = ignorerFeilISimulering,
    ) { saksbehandling, grunnlagsdata ->
        val behandlingBarn = opprettBehandlingBarn(saksbehandling, grunnlagsdata, periode)
        InnvilgelseBarnetilsyn(
            begrunnelse = null,
            perioder =
                listOf(
                    UtgiftsperiodeDto(
                        årMånedFra = periode.stønadsperiode.fom,
                        årMånedTil = periode.stønadsperiode.tom,
                        barn = behandlingBarn.map { it.id },
                        utgifter = periode.utgifterBarnetilsyn,
                        sanksjonsårsak = null,
                        periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                        aktivitetstype = null,
                    ),
                ),
            perioderKontantstøtte = emptyList(),
            kontantstøtteBegrunnelse = null,
            tilleggsstønad = TilleggsstønadDto(false, begrunnelse = null),
        )
    }

    private fun opprettBehandlingBarn(
        saksbehandling: Saksbehandling,
        grunnlagsdata: GrunnlagsdataMedMetadata,
        periode: SummertInfotrygdPeriodeDto,
    ): List<BehandlingBarn> {
        val barnIdenter = periode.barnIdenter
        val grunnlagsbarn = grunnlagsdata.grunnlagsdata.barn.associateBy { it.personIdent }
        val behandlingBarn =
            barnIdenter.map { barnIdent ->
                val barnFraGrunnlag = grunnlagsbarn[barnIdent] ?: error("Finner ikke barn=$barnIdent i grunnlagsdata")
                BehandlingBarn(
                    behandlingId = saksbehandling.id,
                    personIdent = barnIdent,
                    navn = barnFraGrunnlag.navn.visningsnavn(),
                )
            }
        return barnRepository.insertAll(behandlingBarn)
    }

    /**
     * Skal kun kalles direkte fra denne klassen eller [TestSaksbehandlingController]
     */
    @Transactional
    fun opprettMigrering(
        fagsak: Fagsak,
        periode: Månedsperiode,
        inntektsgrunnlag: Int,
        samordningsfradrag: Int,
        erReellArbeidssøker: Boolean = false,
        ignorerFeilISimulering: Boolean = false,
    ): Behandling =
        opprettMigrering(
            fagsak,
            periode,
            ignorerFeilISimulering = ignorerFeilISimulering,
        ) { _, _ ->
            val inntekter = inntekter(periode.fom, inntektsgrunnlag, samordningsfradrag)
            val vedtaksperioder = vedtaksperioder(periode, erReellArbeidssøker)
            InnvilgelseOvergangsstønad(
                periodeBegrunnelse = null,
                inntektBegrunnelse = null,
                perioder = vedtaksperioder,
                inntekter = inntekter,
            )
        }

    @Transactional
    fun opprettMigrering(
        fagsak: Fagsak,
        periode: Månedsperiode,
        ignorerFeilISimulering: Boolean = false,
        vedtak: (saksbehandling: Saksbehandling, grunnlagsdata: GrunnlagsdataMedMetadata) -> VedtakDto,
    ): Behandling {
        fagsakService.settFagsakTilMigrert(fagsak.id)
        val behandling = behandlingService.opprettMigrering(fagsak.id)
        logger.info(
            "Migrerer fagsakPerson=${fagsak.fagsakPersonId} fagsak=${fagsak.id} behandling=${behandling.id} " +
                "fra=${periode.fomDato} til=${periode.tomDato}",
        )
        iverksettService.startBehandling(behandling, fagsak)

        val grunnlagsdata = grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        vurderingService.opprettVilkårForMigrering(behandling)

        val saksbehandling = behandlingService.hentSaksbehandling(behandling.id)
        beregnYtelseSteg.utførSteg(saksbehandling, vedtak.invoke(saksbehandling, grunnlagsdata))

        validerSimulering(fagsak, behandling, ignorerFeilISimulering = ignorerFeilISimulering)

        behandlingService.oppdaterResultatPåBehandling(behandling.id, BehandlingResultat.INNVILGET)
        behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)

        val iverksettDto = iverksettingDtoMapper.tilDtoMaskineltBehandlet(saksbehandling)
        iverksettClient.iverksettUtenBrev(iverksettDto)
        taskService.save(PollStatusFraIverksettTask.opprettTask(behandling.id))

        if (periode.tom >= DatoUtil.årMånedNå()) {
            taskService.save(
                SjekkMigrertStatusIInfotrygdTask.opprettTask(
                    behandling.id,
                    periode.fom.minusMonths(1),
                    fagsak.hentAktivIdent(),
                ),
            )
        }

        return behandlingService.hentBehandling(behandling.id)
    }

    private fun validerSimulering(
        fagsak: Fagsak,
        behandling: Behandling,
        ignorerFeilISimulering: Boolean,
    ) {
        val simulering = simuleringService.hentLagretSimmuleringsresultat(behandling.id)
        val oppsummering = simulering.oppsummering
        val inneholderEtterbetaling = oppsummering.etterbetaling.compareTo(BigDecimal.ZERO) != 0

        if (oppsummering.feilutbetaling.compareTo(BigDecimal.ZERO) != 0) {
            throw MigreringException(
                "Feilutbetaling er ${oppsummering.feilutbetaling}",
                MigreringExceptionType.SIMULERING_FEILUTBETALING,
            )
        } else if (inneholderEtterbetaling && !ignorerFeilISimulering) {
            throw MigreringException(
                "Etterbetaling er ${oppsummering.etterbetaling}",
                MigreringExceptionType.SIMULERING_ETTERBETALING,
            )
        } else if (inneholderDebettrekk(simulering)) {
            throw MigreringException(
                "Simuleringen inneholder posteringstypen TREKK med betalingstypen DEBET. " +
                    "Dette blir en uønsket utbetaling pga en feil. Denne kan migreres på nytt i neste måned.",
                MigreringExceptionType.SIMULERING_DEBET_TREKK,
            )
        }
        if (inneholderEtterbetaling) {
            logger.warn(
                "Migrering inneholder etterbetaling fagsakPersonId=${fagsak.fagsakPersonId} " +
                    "fagsak=${fagsak.id} behandling=${behandling.id} etterbetaling=${oppsummering.etterbetaling}",
            )
        }
    }

    // Kan slettes når TØB fikset TOB-1739
    private fun inneholderDebettrekk(simulering: BeriketSimuleringsresultat) =
        simulering.detaljer.simuleringMottaker.any { simuleringMottaker ->
            simuleringMottaker.simulertPostering.any {
                it.posteringType == PosteringType.TREKK && it.betalingType == BetalingType.DEBIT
            }
        }

    /**
     * Sjekker att perioden som har kode [InfotrygdEndringKode.OVERTFØRT_NY_LØSNING] har opphør i måneden jobbet blir kjørt.
     * Den sjekker også att de summerte periodene sin max(stønadFom) går til den samme måneden
     */
    fun erOpphørtIInfotrygd(
        behandlingId: UUID,
        opphørsmåned: YearMonth,
    ): Boolean {
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val perioder = infotrygdService.hentDtoPerioder(personIdent).overgangsstønad
        val sisteSummertePerioden = perioder.summert.maxByOrNull { it.stønadsperiode.tom }

        if (sisteSummertePerioden == null ||
            sisteSummertePerioden.opphørsdato != null ||
            sisteSummertePerioden.stønadsperiode.tom <= opphørsmåned
        ) {
            logger.info(
                "erOpphørtIInfotrygd behandling=$behandlingId erOpphørt=true - " +
                    "sisteSummertePeriodenTom=${sisteSummertePerioden?.stønadsperiode?.tom}",
            )
            return true
        }
        loggIkkeOpphørt(behandlingId, perioder, sisteSummertePerioden, opphørsmåned)
        return false
    }

    private fun loggIkkeOpphørt(
        behandlingId: UUID,
        perioder: InfotrygdStønadPerioderDto,
        sisteSummertePerioden: SummertInfotrygdPeriodeDto,
        opphørsmåned: YearMonth,
    ) {
        val overførtNyLøsningOpphørsdato =
            perioder.perioder.find { it.kode == InfotrygdEndringKode.OVERTFØRT_NY_LØSNING }?.opphørsdato
        val logMessage =
            "erOpphørtIInfotrygd behandling=$behandlingId erOpphørt=false - " +
                "sistePeriodenTom=$overførtNyLøsningOpphørsdato " +
                "sisteSummertePeriodeTom=${sisteSummertePerioden.stønadsperiode.tom} " +
                "opphørsmåned=$opphørsmåned"
        logger.warn(logMessage)
        val periodeInformasjon =
            perioder.perioder
                .sortedWith(compareBy<InfotrygdPeriode>({ it.stønadId }, { it.vedtakId }, { it.stønadFom }).reversed())
                .map {
                    "InfotrygdPeriode(stønadId=${it.stønadId}, vedtakId=${it.vedtakId}, kode=${it.kode}, " +
                        "stønadFom=${it.stønadFom}, stønadTom=${it.stønadTom}, opphørsdato=${it.opphørsdato})"
                }
        secureLogger.info("$logMessage $periodeInformasjon")
    }

    private fun hentGjeldendePeriodeOgValiderState(
        fagsakPerson: FagsakPerson,
        stønadType: StønadType,
        kjøremåned: YearMonth,
    ): SummertInfotrygdPeriodeDto {
        val personIdent = fagsakPerson.hentAktivIdent()
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(fagsakPerson.id)
        val fagsak =
            when (stønadType) {
                StønadType.OVERGANGSSTØNAD -> fagsaker.overgangsstønad
                StønadType.BARNETILSYN -> fagsaker.barnetilsyn
                StønadType.SKOLEPENGER -> error("Har ikke støtte for å migrere skolepenger")
            }
        fagsak?.let { validerFagsakOgBehandling(it) }
        return infotrygdPeriodeValideringService.hentPeriodeForMigrering(personIdent, stønadType, kjøremåned)
    }

    private fun validerFagsakOgBehandling(fagsak: Fagsak) {
        if (fagsak.stønadstype == StønadType.SKOLEPENGER) {
            throw MigreringException(
                "Kan ikke migrere skolepenger",
                MigreringExceptionType.FEIL_STØNADSTYPE,
            )
        } else if (fagsak.migrert) {
            throw MigreringException("Fagsak er allerede migrert", MigreringExceptionType.ALLEREDE_MIGRERT)
        } else {
            val behandlinger =
                behandlingService
                    .hentBehandlinger(fagsak.id)
                    .filterNot { it.erAvsluttet() && it.resultat == BehandlingResultat.HENLAGT }
            if (behandlinger.isNotEmpty()) {
                throw MigreringException(
                    "Fagsaken har allerede behandlinger",
                    MigreringExceptionType.HAR_ALLEREDE_BEHANDLINGER,
                )
            }
        }
    }

    private fun kjøremåned() = YearMonth.now()

    private fun inntekter(
        fra: YearMonth,
        inntektsgrunnlag: Int,
        samordningsfradrag: Int,
    ) = listOf(
        Inntekt(
            årMånedFra = fra,
            forventetInntekt = BigDecimal(inntektsgrunnlag),
            samordningsfradrag = BigDecimal(samordningsfradrag),
        ),
    )

    private fun erReellArbeidssøker(periode: SummertInfotrygdPeriodeDto): Boolean = periode.aktivitet == InfotrygdAktivitetstype.TILMELDT_SOM_REELL_ARBEIDSSØKER

    private fun vedtaksperioder(
        periode: Månedsperiode,
        erReellArbeidssøker: Boolean,
    ): List<VedtaksperiodeDto> {
        val aktivitet = if (erReellArbeidssøker) AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER else AktivitetType.MIGRERING
        return listOf(
            VedtaksperiodeDto(
                årMånedFra = periode.fom,
                årMånedTil = periode.tom,
                periode = periode,
                aktivitet = aktivitet,
                periodeType = VedtaksperiodeType.MIGRERING,
                sanksjonsårsak = null,
            ),
        )
    }
}
