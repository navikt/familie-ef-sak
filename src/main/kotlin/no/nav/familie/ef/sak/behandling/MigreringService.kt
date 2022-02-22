package no.nav.familie.ef.sak.behandling

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
import no.nav.familie.ef.sak.infotrygd.InfotrygdStønadPerioderDto
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
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
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
        private val simuleringService: SimuleringService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    private enum class MigreringExceptionType {
        ÅPEN_SAK,
        FLERE_IDENTER,
        FLERE_AKTIVE_PERIODER,
        MANGLER_AKTIVE_PERIODER,
        HAR_ALLEREDE_BEHANDLINGER,
        FEIL_STØNADSTYPE,
        FLERE_IDENTER_VEDTAK,
        ALLEREDE_MIGRERT,
        MANGLER_PERIODER,
        FEIL_FOM_DATO,
        FEIL_TOM_DATO,
        SIMULERING_FEILUTBETALING,
        SIMULERING_ETTERBETALING,
        BELØP_0,
    }

    private class MigreringException(val årsak: String, val type: MigreringExceptionType) : RuntimeException(årsak)

    fun hentMigreringInfo(fagsakPersonId: UUID, kjøremåned: YearMonth = kjøremåned()): MigreringInfo {
        val periode = try {
            val fagsakPerson = fagsakPersonService.hentPerson(fagsakPersonId)
            validerSakerIInfotrygd(fagsakPerson)
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

    /**
     * Henter data fra infotrygd og oppretter migrering
     */
    @Transactional
    fun migrerOvergangsstønad(fagsakPersonId: UUID): UUID {
        val fagsakPerson = fagsakPersonService.hentPerson(fagsakPersonId)
        validerSakerIInfotrygd(fagsakPerson)
        val personIdent = fagsakPerson.hentAktivIdent()
        val kjøremåned = kjøremåned()
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.OVERGANGSSTØNAD)
        try {
            val periode = hentGjeldendePeriodeOgValiderState(fagsakPerson, kjøremåned)
            return opprettMigrering(fagsak = fagsak,
                                    fra = fra(periode),
                                    til = til(periode),
                                    inntektsgrunnlag = periode.inntektsgrunnlag,
                                    samordningsfradrag = periode.samordningsfradrag).id
        } catch (e: MigreringException) {
            logger.warn("Kan ikke migrere fagsakPerson=$fagsakPersonId årsak=${e.type}")
            secureLogger.warn("Kan ikke migrere fagsakPerson=$fagsakPersonId - ${e.årsak}")
            throw ApiFeil(e.årsak, HttpStatus.BAD_REQUEST)
        }
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
        val perioder = hentPerioder(personIdent)
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
        val perioder = hentPerioder(personIdent)
        validerHarKunEnIdentPåPerioder(perioder, personIdent)
        return periodeFremITidenEllerBakITiden(perioder, kjøremåned)
    }

    private fun periodeFremITidenEllerBakITiden(perioder: InfotrygdStønadPerioderDto,
                                                kjøremåned: YearMonth): SummertInfotrygdPeriodeDto {
        val gjeldendePerioder = perioder.summert
        val perioderFremITiden = gjeldendePerioder.filter { it.stønadTom >= førsteDagenINesteMåned(kjøremåned) }
        if (perioderFremITiden.isNotEmpty()) {
            return gjeldendePeriodeFremITiden(perioderFremITiden, kjøremåned)
        }

        if (!featureToggleService.isEnabled("familie-ef-sak.migrering-bak-i-tiden")) {
            throw MigreringException("Mangler aktive perioder", MigreringExceptionType.MANGLER_AKTIVE_PERIODER)
        }

        val sistePeriode = gjeldendePerioder.maxByOrNull { it.stønadFom }
                           ?: throw MigreringException("Har ikke noen perioder å migrere",
                                                       MigreringExceptionType.MANGLER_PERIODER)

        return sisteMånedenPåPeriodeBakITiden(sistePeriode)
    }

    /**
     * Perioder frem i tiden migreres fra neste måned
     */
    private fun gjeldendePeriodeFremITiden(gjeldendePerioder: List<SummertInfotrygdPeriodeDto>,
                                           kjøremåned: YearMonth): SummertInfotrygdPeriodeDto {
        if (gjeldendePerioder.size > 1) {
            throw MigreringException("Har fler enn 1 (${gjeldendePerioder.size}) aktiv periode",
                                     MigreringExceptionType.FLERE_AKTIVE_PERIODER)
        }
        val periode = gjeldendePerioder.single()
        validerFomDato(periode)
        validerTomDato(periode)
        return periode.copy(stønadFom = maxOf(kjøremåned.atDay(1), periode.stønadFom))
    }

    /**
     * Henter siste måneden for en periode bak i tiden
     * Hvis det kun er 1 måned, så valideres det att fom-dato ikke er annet enn 1 i måneden
     */
    private fun sisteMånedenPåPeriodeBakITiden(periode: SummertInfotrygdPeriodeDto): SummertInfotrygdPeriodeDto {
        val stønadTom = periode.stønadTom
        val stønadFom = periode.stønadFom
        val tomMåned = YearMonth.of(stønadTom.year, stønadTom.month)
        val nyFomDato = tomMåned.atDay(1)
        validerTomDato(periode)
        if (stønadFom > nyFomDato) {
            throw MigreringException("Startdato er annet enn første i måneden, dato=$stønadFom",
                                     MigreringExceptionType.FEIL_FOM_DATO)
        }
        if (periode.beløp == 0) {
            throw MigreringException("Beløp er 0 på siste perioden, har ikke støtte for det ennå. fom=$stønadFom",
                                     MigreringExceptionType.BELØP_0)
        }
        return periode.copy(stønadFom = YearMonth.of(stønadTom.year, stønadTom.month).atDay(1))
    }

    private fun validerFomDato(periode: SummertInfotrygdPeriodeDto) {
        if (periode.stønadFom.dayOfMonth != 1) {
            throw MigreringException("Startdato er annet enn første i måneden, dato=${periode.stønadFom}",
                                     MigreringExceptionType.FEIL_FOM_DATO)
        }
    }

    private fun validerTomDato(periode: SummertInfotrygdPeriodeDto) {
        val dato = periode.stønadTom
        if (YearMonth.of(dato.year, dato.month).atEndOfMonth() != dato) {
            throw MigreringException("Sluttdato er annet enn siste i måneden, dato=$dato",
                                     MigreringExceptionType.FEIL_TOM_DATO)
        }
        if (dato.isBefore(LocalDate.now().minusMonths(12))) {
            throw MigreringException("Kan ikke migrere når forrige utbetaling i infotrygd er mer enn 1 år tilbake i tid, dato=$dato",
                                     MigreringExceptionType.FEIL_TOM_DATO)
        }
    }

    private fun validerHarKunEnIdentPåPerioder(perioder: InfotrygdStønadPerioderDto,
                                               personIdent: String) {
        perioder.perioder.find { it.personIdent != personIdent }?.let {
            throw MigreringException("Det finnes perioder som inneholder annet fnr=${it.personIdent}. " +
                                     "Disse vedtaken må endres til aktivt fnr i Infotrygd",
                                     MigreringExceptionType.FLERE_IDENTER_VEDTAK)
        }
    }

    private fun validerSakerIInfotrygd(fagsakPerson: FagsakPerson): List<InfotrygdSak> {
        val personIdent = fagsakPerson.hentAktivIdent()
        val sakerForOvergangsstønad =
                infotrygdService.hentSaker(personIdent).saker.filter { it.stønadType == StønadType.OVERGANGSSTØNAD }
        validerFinnesIkkeÅpenSak(sakerForOvergangsstønad)
        validerSakerInneholderKunEnIdent(sakerForOvergangsstønad, personIdent)
        return sakerForOvergangsstønad
    }

    private fun validerSakerInneholderKunEnIdent(sakerForOvergangsstønad: List<InfotrygdSak>,
                                                 personIdent: String) {
        sakerForOvergangsstønad.find { it.personIdent != personIdent }?.let {
            throw MigreringException("Finnes sak med annen personIdent for personen. ${lagSakFeilinfo(it)} " +
                                     "personIdent=${it.personIdent}. " +
                                     "Disse sakene må oppdateres med aktivt fnr i Infotrygd",
                                     MigreringExceptionType.FLERE_IDENTER)
        }
    }

    private fun validerFinnesIkkeÅpenSak(sakerForOvergangsstønad: List<InfotrygdSak>) {
        sakerForOvergangsstønad.find { it.resultat == InfotrygdSakResultat.ÅPEN_SAK }?.let {
            throw MigreringException("Har åpen sak. ${lagSakFeilinfo(it)}", MigreringExceptionType.ÅPEN_SAK)
        }
    }

    private fun lagSakFeilinfo(sak: InfotrygdSak): String {
        return "saksblokk=${sak.saksblokk} saksnr=${sak.saksnr} " +
               "registrertDato=${sak.registrertDato} mottattDato=${sak.mottattDato}"
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

    private fun hentPerioder(personIdent: String): InfotrygdStønadPerioderDto {
        val allePerioder = infotrygdService.hentDtoPerioder(personIdent)
        return allePerioder.overgangsstønad
    }

    private fun kjøremåned() = YearMonth.now()
    private fun førsteDagenINesteMåned(yearMonth: YearMonth) = yearMonth.plusMonths(1).atDay(1)

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