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
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.fagsak.dto.MigreringInfo
import no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeUtil.filtrerOgSorterPerioderFraInfotrygd
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infotrygd.InfotrygdStønadPerioderDto
import no.nav.familie.ef.sak.infotrygd.SummertInfotrygdPeriodeDto
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

@Service
class MigreringService(
        private val taskRepository: TaskRepository,
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val iverksettService: IverksettService,
        private val iverksettClient: IverksettClient,
        private val grunnlagsdataService: GrunnlagsdataService,
        private val vurderingService: VurderingService,
        private val beregnYtelseSteg: BeregnYtelseSteg,
        private val iverksettingDtoMapper: IverksettingDtoMapper,
        private val featureToggleService: FeatureToggleService,
        private val infotrygdService: InfotrygdService,
        private val beregningService: BeregningService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    private enum class MigreringExceptionType {
        ÅPEN_SAK,
        FLERE_IDENTER,
        FLERE_AKTIVE_PERIODER,
        HAR_OPPHØRSDATO,
        MANGLER_AKTIVE_PERIODER,
        HAR_ALLEREDE_BEHANDLINGER,
        FEIL_STØNADSTYPE,
        FLERE_IDENTER_VEDTAK,
        ALLEREDE_MIGRERT
    }

    private class MigreringException(val årsak: String, val type: MigreringExceptionType) : RuntimeException()

    fun hentMigreringInfo(fagsakId: UUID, kjøremåned: YearMonth = kjøremåned()): MigreringInfo {
        val periode = try {
            validerSakerIInfotrygd(fagsakId)
            hentGjeldendePeriodeOgValiderState(fagsakId, kjøremåned)
        } catch (e: MigreringException) {
            logger.info("Kan ikke migrere fagsak=$fagsakId årsak=${e.type}")
            if (e.type == MigreringExceptionType.FLERE_IDENTER || e.type == MigreringExceptionType.FLERE_IDENTER_VEDTAK) {
                secureLogger.info("Kan ikke migrere fagsak=$fagsakId - ${e.årsak}")
            }
            return MigreringInfo(kanMigreres = false, e.årsak)
        }
        logger.info("Kan migrere fagsak=$fagsakId")

        val fra = fra(kjøremåned, periode)
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
    fun migrerFagsak(fagsakId: UUID) {
        val personIdent = fagsakService.hentAktivIdent(fagsakId)
        val kjøremåned = kjøremåned()
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.OVERGANGSSTØNAD)
        validerSakerIInfotrygd(fagsakId)
        val periode = hentGjeldendePeriodeOgValiderState(fagsakId, kjøremåned)
        opprettMigrering(fagsak = fagsak,
                         fra = fra(kjøremåned, periode),
                         til = til(periode),
                         inntektsgrunnlag = periode.inntektsgrunnlag,
                         samordningsfradrag = periode.samordningsfradrag)
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
        logger.info("Migrerer fagsak=${fagsak.id} behandling=${behandling.id} fra=$fra til=$til")
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

        behandlingService.oppdaterResultatPåBehandling(behandling.id, BehandlingResultat.INNVILGET)
        behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        val iverksettDto = iverksettingDtoMapper.tilMigreringDto(behandling)
        iverksettClient.iverksettMigrering(iverksettDto)
        taskRepository.save(PollStatusFraIverksettTask.opprettTask(behandling.id))
        taskRepository.save(SjekkMigrertStatusIInfotrygdTask.opprettTask(behandling.id,
                                                                         fra.minusMonths(1),
                                                                         fagsak.hentAktivIdent()))

        return behandlingService.hentBehandling(behandling.id)
    }

    /**
     * Sjekker att perioden som har kode [InfotrygdEndringKode.OVERTFØRT_NY_LØSNING] har opphør i måneden jobbet blir kjørt.
     * Den sjekker også att de summerte periodene sin max(stønadFom) går til den samme måneden
     */
    fun erOpphørtIInfotrygd(behandlingId: UUID, opphørsmåned: YearMonth): Boolean {
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val perioder = hentPerioder(personIdent)
        val perioderStønadTom = filtrerOgSorterPerioderFraInfotrygd(perioder.perioder)
                .find { it.kode == InfotrygdEndringKode.OVERTFØRT_NY_LØSNING }?.opphørsdato
        val maxStønadTom = perioder.summert.maxOf { it.stønadTom }
        val stønadTomErLike =
                perioderStønadTom == maxStønadTom // liten ekstrasjekk, som verifiserer att summertePerioder er riktig
        val stønadTomErFørEllerLikOpphørsmåned = YearMonth.of(maxStønadTom.year, maxStønadTom.month) <= opphørsmåned
        val erOpphørtIInfotrygd = stønadTomErLike && stønadTomErFørEllerLikOpphørsmåned
        if (!erOpphørtIInfotrygd) {
            logger.warn("erOpphørtIInfotrygd - Datoer ikke like behandling=$behandlingId " +
                        "sistePeriodenTom=$perioderStønadTom " +
                        "summertMaxTom=$maxStønadTom " +
                        "opphørsmåned=$opphørsmåned")
        }
        return erOpphørtIInfotrygd
    }

    private fun hentGjeldendePeriodeOgValiderState(fagsakId: UUID, kjøremåned: YearMonth): SummertInfotrygdPeriodeDto {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val personIdent = fagsak.hentAktivIdent()
        validerFagsakOgBehandling(fagsak)
        val gjeldendePerioder = hentGjeldendePerioderFraInfotrygdOgValider(personIdent, kjøremåned)
        if (gjeldendePerioder.isEmpty()) {
            throw MigreringException("Har 0 aktive perioder", MigreringExceptionType.MANGLER_AKTIVE_PERIODER)
        } else if (gjeldendePerioder.size > 1) {
            throw MigreringException("Har fler enn 1 (${gjeldendePerioder.size}) aktiv periode",
                                     MigreringExceptionType.FLERE_AKTIVE_PERIODER)
        }
        val periode = gjeldendePerioder.single()
        if (periode.opphørsdato != null) { // Håndterer ikke denne nå
            throw MigreringException("Har opphørsdato (${periode.opphørsdato}) i perioden",
                                     MigreringExceptionType.HAR_OPPHØRSDATO)
        }
        return periode
    }

    private fun validerSakerIInfotrygd(fagsakId: UUID): List<InfotrygdSak> {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val personIdent = fagsak.hentAktivIdent()
        val sakerForOvergangsstønad =
                infotrygdService.hentSaker(personIdent).saker.filter { it.stønadType == StønadType.OVERGANGSSTØNAD }
        sakerForOvergangsstønad.find { it.resultat == InfotrygdSakResultat.ÅPEN_SAK }?.let {
            throw MigreringException("Har åpen sak. ${lagSakFeilinfo(it)}", MigreringExceptionType.ÅPEN_SAK)
        }
        sakerForOvergangsstønad.find { it.personIdent != personIdent }?.let {
            throw MigreringException("Finnes sak med annen personIdent for personen. ${lagSakFeilinfo(it)} " +
                                     "personIdent=${it.personIdent}. " +
                                     "Disse sakene må oppdateres med aktivt fnr i Infotrygd",
                                     MigreringExceptionType.FLERE_IDENTER)
        }
        return sakerForOvergangsstønad
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

    private fun hentGjeldendePerioderFraInfotrygdOgValider(personIdent: String,
                                                           kjøremåned: YearMonth): List<SummertInfotrygdPeriodeDto> {

        val perioder = hentPerioder(personIdent)
        perioder.perioder.find { it.personIdent != personIdent }?.let {
            throw MigreringException("Det finnes perioder som inneholder annet fnr=${it.personIdent}. " +
                                     "Disse vedtaken må endres til aktivt fnr i Infotrygd",
                                     MigreringExceptionType.FLERE_IDENTER_VEDTAK)
        }
        return perioder.summert.filter { it.stønadTom >= førsteDagenINesteMåned(kjøremåned) }
    }

    private fun hentPerioder(personIdent: String): InfotrygdStønadPerioderDto {
        val allePerioder = infotrygdService.hentDtoPerioder(personIdent)
        return allePerioder.overgangsstønad
    }

    private fun kjøremåned() = YearMonth.now()
    private fun førsteDagenINesteMåned(yearMonth: YearMonth) = yearMonth.plusMonths(1).atDay(1)

    private fun til(periode: SummertInfotrygdPeriodeDto): YearMonth =
            YearMonth.of(periode.stønadTom.year, periode.stønadTom.month)

    fun fra(kjøremåned: YearMonth, periode: SummertInfotrygdPeriodeDto): YearMonth {
        val fra = maxOf(førsteDagenINesteMåned(kjøremåned), periode.stønadFom)
        return YearMonth.of(fra.year, fra.month)
    }

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