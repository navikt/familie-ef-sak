package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
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
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.fagsak.dto.MigreringInfo
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infotrygd.SummertInfotrygdPeriodeDto
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.prosessering.domene.TaskRepository
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
        private val taskRepository: TaskRepository,
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
        private val infotrygdPeriodeValideringService: InfotrygdPeriodeValideringService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentMigreringInfo(fagsakPersonId: UUID, kjøremåned: YearMonth = kjøremåned()): MigreringInfo {
        val periode = try {
            val fagsakPerson = fagsakPersonService.hentPerson(fagsakPersonId)
            hentGjeldendePeriodeOgValiderState(fagsakPerson, kjøremåned)
        } catch (e: MigreringException) {
            logger.info("Kan ikke migrere fagsakPerson=$fagsakPersonId årsak=${e.type}")
            secureLogger.info("Kan ikke migrere fagsakPerson=$fagsakPersonId - ${e.årsak}")
            return MigreringInfo(kanMigreres = false, e.årsak)
        }
        logger.info("Kan migrere fagsakPerson=$fagsakPersonId")

        val fra = fra(periode)
        val til = til(periode)
        val vedtaksperioder = vedtaksperioder(fra, til)
        val inntekter = inntekter(fra, periode.inntektsgrunnlag, periode.samordningsfradrag)
        val beregnYtelse = beregningService.beregnYtelse(vedtaksperioder.tilPerioder(), inntekter.tilInntektsperioder())
        return MigreringInfo(kanMigreres = true,
                             stønadFom = fra,
                             stønadTom = til,
                             inntektsgrunnlag = periode.inntektsgrunnlag,
                             samordningsfradrag = periode.samordningsfradrag,
                             beløpsperioder = beregnYtelse)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun migrerOvergangsstønadAutomatisk(ident: String) {
        val fagsak = fagsakService.hentEllerOpprettFagsak(ident, Stønadstype.OVERGANGSSTØNAD)
        migrerOvergangsstønadForFagsakPerson(fagsak.fagsakPersonId)
    }

    /**
     * Henter data fra infotrygd og oppretter migrering
     */
    @Transactional
    fun migrerOvergangsstønad(fagsakPersonId: UUID): UUID {
        try {
            return migrerOvergangsstønadForFagsakPerson(fagsakPersonId)
        } catch (e: MigreringException) {
            logger.warn("Kan ikke migrere fagsakPerson=$fagsakPersonId årsak=${e.type}")
            secureLogger.warn("Kan ikke migrere fagsakPerson=$fagsakPersonId - ${e.årsak}")
            throw ApiFeil(e.årsak, HttpStatus.BAD_REQUEST)
        }
    }

    private fun migrerOvergangsstønadForFagsakPerson(fagsakPersonId: UUID): UUID {
        val fagsakPerson = fagsakPersonService.hentPerson(fagsakPersonId)
        val personIdent = fagsakPerson.hentAktivIdent()
        val kjøremåned = kjøremåned()
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.OVERGANGSSTØNAD)
        val periode = hentGjeldendePeriodeOgValiderState(fagsakPerson, kjøremåned)
        return opprettMigrering(fagsak = fagsak,
                                fra = fra(periode),
                                til = til(periode),
                                inntektsgrunnlag = periode.inntektsgrunnlag,
                                samordningsfradrag = periode.samordningsfradrag).id
    }

    /**
     * Skal kun kalles direkte fra denne klassen eller [TestSaksbehandlingController]
     */
    @Transactional
    fun opprettMigrering(fagsak: Fagsak,
                         fra: YearMonth,
                         til: YearMonth,
                         inntektsgrunnlag: Int,
                         samordningsfradrag: Int): Behandling {
        feilHvisIkke(featureToggleService.isEnabled("familie.ef.sak.migrering")) {
            "Feature toggle for migrering er disabled"
        }
        fagsakService.settFagsakTilMigrert(fagsak.id)
        val behandling = behandlingService.opprettMigrering(fagsak.id)
        logger.info("Migrerer fagsakPerson=${fagsak.fagsakPersonId} fagsak=${fagsak.id} behandling=${behandling.id} " +
                    "fra=$fra til=$til")
        iverksettService.startBehandling(behandling, fagsak)

        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        vurderingService.opprettVilkårForMigrering(behandling)

        val vedtaksperioder = vedtaksperioder(fra, til)
        val inntekter = inntekter(fra, inntektsgrunnlag, samordningsfradrag)
        beregnYtelseSteg.utførSteg(behandling, Innvilget(resultatType = ResultatType.INNVILGE,
                                                         periodeBegrunnelse = null,
                                                         inntektBegrunnelse = null,
                                                         perioder = vedtaksperioder,
                                                         inntekter = inntekter))
        validerSimulering(behandling)

        behandlingService.oppdaterResultatPåBehandling(behandling.id, BehandlingResultat.INNVILGET)
        behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        val iverksettDto = iverksettingDtoMapper.tilMigreringDto(behandling)
        iverksettClient.iverksettMigrering(iverksettDto)
        taskRepository.save(PollStatusFraIverksettTask.opprettTask(behandling.id))

        if (til >= YearMonth.now()) {
            taskRepository.save(SjekkMigrertStatusIInfotrygdTask.opprettTask(behandling.id,
                                                                             fra.minusMonths(1),
                                                                             fagsak.hentAktivIdent()))
        }

        return behandlingService.hentBehandling(behandling.id)
    }

    private fun validerSimulering(behandling: Behandling) {
        val simulering = simuleringService.hentLagretSimuleringsresultat(behandling.id)
        if (simulering.feilutbetaling.compareTo(BigDecimal.ZERO) != 0) {
            throw MigreringException("Feilutbetaling er ${simulering.feilutbetaling}",
                                     MigreringExceptionType.SIMULERING_FEILUTBETALING)
        } else if (simulering.etterbetaling.compareTo(BigDecimal.ZERO) != 0) {
            throw MigreringException("Etterbetaling er ${simulering.etterbetaling}",
                                     MigreringExceptionType.SIMULERING_ETTERBETALING)
        }
    }

    /**
     * Sjekker att perioden som har kode [InfotrygdEndringKode.OVERTFØRT_NY_LØSNING] har opphør i måneden jobbet blir kjørt.
     * Den sjekker også att de summerte periodene sin max(stønadFom) går til den samme måneden
     */
    fun erOpphørtIInfotrygd(behandlingId: UUID, opphørsmåned: YearMonth): Boolean {
        val opphørsdato = opphørsmåned.atEndOfMonth()
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val perioder = infotrygdService.hentDtoPerioder(personIdent).overgangsstønad
        val overførtNyLøsningOpphørsdato =
                perioder.perioder.find { it.kode == InfotrygdEndringKode.OVERTFØRT_NY_LØSNING }?.opphørsdato
        val maxStønadTom = perioder.summert.maxOf { it.stønadTom }

        val summertMaxTomErFørOpphørsmåned = maxStønadTom <= opphørsdato
        val overførtTilNyLøsningOpphørErFørOpphørsdato =
                overførtNyLøsningOpphørsdato != null && overførtNyLøsningOpphørsdato <= opphørsdato
        val erOpphørtIInfotrygd = summertMaxTomErFørOpphørsmåned && overførtTilNyLøsningOpphørErFørOpphørsdato
        if (!erOpphørtIInfotrygd) {
            val logMessage = "erOpphørtIInfotrygd - Datoer ikke like behandling=$behandlingId " +
                             "sistePeriodenTom=$overførtNyLøsningOpphørsdato " +
                             "summertMaxTom=$maxStønadTom " +
                             "opphørsmåned=$opphørsmåned"
            logger.warn(logMessage)
            val periodeInformasjon = perioder.perioder
                    .sortedWith(compareBy<InfotrygdPeriode>({ it.stønadId }, { it.vedtakId }, { it.stønadFom }).reversed())
                    .map {
                        "InfotrygdPeriode(stønadId=${it.stønadId}, vedtakId=${it.vedtakId}, kode=${it.kode}, " +
                        "stønadFom=${it.stønadFom}, stønadTom=${it.stønadTom}, opphørsdato=${it.opphørsdato})"
                    }
            secureLogger.info("$logMessage $periodeInformasjon")
        }
        return erOpphørtIInfotrygd
    }

    private fun hentGjeldendePeriodeOgValiderState(fagsakPerson: FagsakPerson,
                                                   kjøremåned: YearMonth): SummertInfotrygdPeriodeDto {
        val personIdent = fagsakPerson.hentAktivIdent()
        val fagsak = fagsakService.finnFagsakerForFagsakPersonId(fagsakPerson.id).overgangsstønad
        fagsak?.let { validerFagsakOgBehandling(it) }
        return infotrygdPeriodeValideringService.hentPeriodeForMigrering(personIdent, kjøremåned)
    }

    private fun validerFagsakOgBehandling(fagsak: Fagsak) {
        if (fagsak.stønadstype != Stønadstype.OVERGANGSSTØNAD) {
            throw MigreringException("Håndterer ikke andre stønadstyper enn overgangsstønad",
                                     MigreringExceptionType.FEIL_STØNADSTYPE)
        } else if (fagsak.migrert) {
            throw MigreringException("Fagsak er allerede migrert", MigreringExceptionType.ALLEREDE_MIGRERT)
        } else if (behandlingService.hentBehandlinger(fagsak.id).any { it.type != BehandlingType.BLANKETT }) {
            throw MigreringException("Fagsaken har allerede behandlinger", MigreringExceptionType.HAR_ALLEREDE_BEHANDLINGER)
        }
    }

    private fun kjøremåned() = YearMonth.now()

    private fun til(periode: SummertInfotrygdPeriodeDto): YearMonth =
            YearMonth.of(periode.stønadTom.year, periode.stønadTom.month)

    fun fra(periode: SummertInfotrygdPeriodeDto): YearMonth =
            YearMonth.of(periode.stønadFom.year, periode.stønadFom.month)

    private fun inntekter(fra: YearMonth,
                          inntektsgrunnlag: Int,
                          samordningsfradrag: Int) =
            listOf(Inntekt(årMånedFra = fra,
                           forventetInntekt = BigDecimal(inntektsgrunnlag),
                           samordningsfradrag = BigDecimal(samordningsfradrag)))

    private fun vedtaksperioder(fra: YearMonth,
                                til: YearMonth) =
            listOf(VedtaksperiodeDto(årMånedFra = fra,
                                     årMånedTil = til,
                                     aktivitet = AktivitetType.MIGRERING,
                                     periodeType = VedtaksperiodeType.MIGRERING))
}