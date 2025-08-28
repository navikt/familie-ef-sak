package no.nav.familie.ef.sak.behandling.revurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.beregning.BeregningUtils
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.beregning.tilInntekt
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.isEqualOrBefore
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.fraDomene
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.felles.Opplysningskilde
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleAutomatiskInntektsendringTask.TYPE,
    beskrivelse = "Skal automatisk opprette en ny behandling ved automatisk inntektsendring",
)
class BehandleAutomatiskInntektsendringTask(
    private val revurderingService: RevurderingService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val årsakRevurderingsRepository: ÅrsakRevurderingsRepository,
    private val automatiskRevurderingService: AutomatiskRevurderingService,
    private val featureToggleService: FeatureToggleService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val toggle = featureToggleService.isEnabled(Toggle.BEHANDLE_AUTOMATISK_INNTEKTSENDRING)

        val personIdent = objectMapper.readValue<PayloadBehandleAutomatiskInntektsendringTask>(task.payload).personIdent
        val fagsak =
            fagsakService.finnFagsak(
                personIdenter = setOf(personIdent),
                stønadstype = StønadType.OVERGANGSSTØNAD,
            )
        if (fagsak == null) {
            throw IllegalStateException("Finner ikke fagsak for personIdent=$personIdent på stønadstype=${StønadType.OVERGANGSSTØNAD} under automatisk inntektsendring")
        }
        secureLogger.info("Kan opprette automatisk inntektsendringsbehandling med $personIdent stønadstype=${StønadType.OVERGANGSSTØNAD} faksakId ${fagsak.id}")

        if (toggle) {
            opprettAutomatiskRevurderingForInntektsendring(fagsak.id, personIdent)
        } else {
            logAutomatiskRevurderingForInntektsendring(fagsak, personIdent)
        }
    }

    private fun opprettAutomatiskRevurderingForInntektsendring(
        fagsakId: UUID,
        personIdent: String,
    ) {
        val behandling =
            revurderingService.opprettRevurderingManuelt(
                RevurderingDto(
                    fagsakId = fagsakId,
                    behandlingsårsak = BehandlingÅrsak.AUTOMATISK_INNTEKTSENDRING,
                    kravMottatt = LocalDate.now(),
                    vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE,
                ),
            )
        val inntektResponse = automatiskRevurderingService.lagreInntektResponse(personIdent, behandling.id)
        val forrigeBehandling = behandling.forrigeBehandlingId?.let { behandlingService.hentBehandling(it) } ?: throw IllegalStateException("Burde vært en forrigeBehandlingId etter automatisk revurdering for behandlingId: ${behandling.id}")

        val forrigeVedtak =
            if (forrigeBehandling.erGOmregning()) {
                val vedtakFørGOmregning = vedtakService.hentVedtak(forrigeBehandling.forrigeBehandlingId ?: throw IllegalStateException("Finner ikke forrigeBehandlingId for behandlingId som er en G-omregning: ${forrigeBehandling.id}"))
                val gOmregningVedtak = vedtakService.hentVedtak(forrigeBehandling.id)
                sammenslåVedtak(vedtakFørGOmregning, gOmregningVedtak)
            } else {
                vedtakService.hentVedtak(forrigeBehandling.id)
            }

        val perioder = oppdaterFørsteVedtaksperiodeMedRevurderesFraDato(forrigeVedtak, inntektResponse)
        val inntektsperioder = oppdaterInntektMedNyBeregnetForventetInntekt(forrigeVedtak, inntektResponse, perioder.first().periode.fom)
        val innvilgelseOvergangsstønad =
            InnvilgelseOvergangsstønad(
                periodeBegrunnelse = "Behandlingen er opprettet automatisk fordi inntekten har økt. Overgangsstønaden endres fra måneden etter at inntekten har økt minst 10 prosent.",
                inntektBegrunnelse = lagInntektsperiodeTekst(inntektsperioder, inntektResponse, forrigeVedtak, forrigeBehandling.erGOmregning()),
                perioder = perioder.fraDomene(),
                inntekter = inntektsperioder.tilInntekt(),
                samordningsfradragType = forrigeVedtak.samordningsfradragType,
            )

        årsakRevurderingsRepository.insert(ÅrsakRevurdering(behandlingId = behandling.id, opplysningskilde = Opplysningskilde.AUTOMATISK_OPPRETTET_BEHANDLING, årsak = Revurderingsårsak.ENDRING_INNTEKT, beskrivelse = null))
        vedtakService.lagreVedtak(vedtakDto = innvilgelseOvergangsstønad, behandlingId = behandling.id, stønadstype = StønadType.OVERGANGSSTØNAD)
        logger.info("Opprettet behandling for automatisk inntektsendring: ${behandling.id}")
    }

    private fun sammenslåVedtak(
        vedtakFørGOmregning: Vedtak,
        gOmregningVedtak: Vedtak,
    ): Vedtak {
        val inntektsperioderFraVedtakFørGOmregning =
            vedtakFørGOmregning.inntekter?.inntekter
                ?: throw IllegalStateException("Fant ikke inntektsperioder for behandlingId: ${vedtakFørGOmregning.behandlingId}")

        val inntektsperioderFraGOmregning = gOmregningVedtak.inntekter?.inntekter ?: emptyList()

        val justerteInntektsperioderFørGOmregning =
            inntektsperioderFraVedtakFørGOmregning.map { v1 ->
                val overlappende = inntektsperioderFraGOmregning.firstOrNull { v2 -> v1.periode.overlapper(v2.periode) }

                if (overlappende != null) {
                    if (v1.periode.fom == overlappende.periode.fom) {
                        overlappende
                    } else {
                        val nyPeriode = Månedsperiode(v1.periode.fom, overlappende.periode.fom.minusMonths(1))
                        if (nyPeriode.fom <= nyPeriode.tom) {
                            v1.copy(periode = nyPeriode)
                        } else {
                            throw IllegalStateException("Feil ved avkorting av periode. Ugyldig start- og sluttdato for periode: $nyPeriode i behandlingId: ${vedtakFørGOmregning.behandlingId}")
                        }
                    }
                } else {
                    v1
                }
            }

        val sammenslått = justerteInntektsperioderFørGOmregning + inntektsperioderFraGOmregning

        return vedtakFørGOmregning.copy(inntekter = InntektWrapper(sammenslått))
    }

    private fun logAutomatiskRevurderingForInntektsendring(
        fagsak: Fagsak,
        personIdent: String,
    ) {
        val inntektResponse = automatiskRevurderingService.hentInntektResponse(personIdent)
        val inntektPrMåned = inntektResponse.inntektsmåneder.map { LogInntekt(it.måned, it.totalInntekt()) }
        secureLogger.info("Månedlig inntekt for fagsak eksternId=${fagsak.eksternId} : $inntektPrMåned")
        val forventetInntekt = inntektResponse.forventetMånedsinntekt()
        val behandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        if (behandling != null) {
            val forrigeBehandling = behandling.forrigeBehandlingId?.let { behandlingService.hentBehandling(it) } ?: throw IllegalStateException("Burde vært en forrigeBehandlingId etter automatisk revurdering for behandlingId: ${behandling.id}")
            val forrigeVedtak =
                if (forrigeBehandling.erGOmregning()) {
                    val vedtakFørGOmregning = vedtakService.hentVedtak(forrigeBehandling.forrigeBehandlingId ?: throw IllegalStateException("Finner ikke forrigeBehandlingId for behandlingId som er en G-omregning: ${forrigeBehandling.id}"))
                    val gOmregningVedtak = vedtakService.hentVedtak(forrigeBehandling.id)
                    sammenslåVedtak(vedtakFørGOmregning, gOmregningVedtak)
                } else {
                    vedtakService.hentVedtak(forrigeBehandling.id)
                }
            val perioder = oppdaterFørsteVedtaksperiodeMedRevurderesFraDato(forrigeVedtak, inntektResponse)
            val inntektsperioder = oppdaterInntektMedNyBeregnetForventetInntekt(forrigeVedtak, inntektResponse, perioder.first().periode.fom)
            logger.info("Ville opprettet inntektsperioder for fagsak eksternId: ${fagsak.eksternId} - nye inntektsperioder: " + inntektsperioder)
            logger.info("Ville opprettet følgende vedtaksperioder for fagsak eksternId: ${fagsak.eksternId} - nye vedtaksperioder: $perioder med ny forventet månedsinntekt: $forventetInntekt")
        } else {
            logger.info("Fant ikke siste iverksatte behandling for fagsakId: ${fagsak.id}")
        }
    }

    fun oppdaterFørsteVedtaksperiodeMedRevurderesFraDato(
        forrigeVedtak: Vedtak,
        inntektResponse: InntektResponse,
    ): List<Vedtaksperiode> {
        val førstePeriode = forrigeVedtak.perioder?.perioder?.first()
        val førstePeriodeMedRevurderesFraDato =
            førstePeriode?.copy(
                datoFra =
                    inntektResponse
                        .førsteMånedMed10ProsentInntektsøkning(forrigeVedtak)
                        .atDay(1)
                        ?.plusMonths(1) ?: førstePeriode.datoFra,
            ) as Vedtaksperiode
        val perioder =
            if (forrigeVedtak.perioder.perioder.size > 1) {
                listOf(førstePeriodeMedRevurderesFraDato) + forrigeVedtak.perioder.perioder.subList(1, forrigeVedtak.perioder.perioder.size)
            } else {
                listOf(førstePeriodeMedRevurderesFraDato)
            }
        return perioder
    }

    fun oppdaterInntektMedNyBeregnetForventetInntekt(
        forrigeVedtak: Vedtak,
        inntektResponse: InntektResponse,
        revurderesFra: YearMonth,
    ): List<Inntektsperiode> {
        if (revurderesFra.isEqualOrBefore(YearMonth.now())) {
            val cutoffPeriode =
                if (inntektResponse.skalMedberegneInntektFraInneværendeMåned()) {
                    Månedsperiode(YearMonth.now().minusMonths(2), YearMonth.now())
                } else {
                    Månedsperiode(YearMonth.now().minusMonths(3), YearMonth.now().minusMonths(1))
                }

            val inntektsperioder =
                generateSequence(revurderesFra) { måned -> måned.plusMonths(1) }
                    .takeWhile { måned -> måned.isEqualOrBefore(cutoffPeriode.tom) }
                    .map { måned ->
                        Inntektsperiode(
                            periode = Månedsperiode(måned),
                            månedsinntekt = BigDecimal(inntektResponse.totalInntektForÅrMåned(måned)),
                            inntekt = BigDecimal(0),
                            dagsats = BigDecimal(0),
                            samordningsfradrag = BigDecimal(0),
                        )
                    }.toList()

            val inntektsperiodeFremover =
                Inntektsperiode(
                    periode = Månedsperiode(cutoffPeriode.tom.plusMonths(1), forrigeVedtak.perioder?.perioder?.maxOf { it.periode.tom } ?: throw IllegalStateException("Mangler vedtaksperioder")),
                    månedsinntekt = BigDecimal(inntektResponse.forventetMånedsinntekt()),
                    inntekt = BigDecimal(0),
                    dagsats = BigDecimal(0),
                    samordningsfradrag = BigDecimal(0),
                )
            val sammenslåttInntektsperioder = slåSammenPerioderMedLikInntekt(inntektsperioder)

            return sammenslåttInntektsperioder + listOf(inntektsperiodeFremover)
        }

        val forventetÅrsinntekt = inntektResponse.forventetMånedsinntekt() * 12
        val inntekterMinimum3MndTilbake = forrigeVedtak.inntekter?.inntekter?.filter { it.periode.fomDato <= YearMonth.now().minusMonths(3).atDay(1) } ?: emptyList()
        val nyesteInntektsperiode = inntekterMinimum3MndTilbake.maxBy { it.periode.fomDato }
        val oppdatertInntektsperiode = nyesteInntektsperiode.copy(inntekt = BigDecimal(forventetÅrsinntekt))
        return forrigeVedtak.inntekter
            ?.inntekter
            ?.minus(nyesteInntektsperiode)
            ?.plus(oppdatertInntektsperiode) ?: emptyList()
    }

    private fun slåSammenPerioderMedLikInntekt(perioder: List<Inntektsperiode>): List<Inntektsperiode> {
        if (perioder.isEmpty()) return emptyList()
        val sorted = perioder.sortedBy { it.periode.fom }
        val sammenslåttInntektsperioder = mutableListOf<Inntektsperiode>()
        var inntektsperiode = sorted.first()

        for (nesteInntektsperiode in sorted.drop(1)) {
            val periodeTom = inntektsperiode.periode.tom
            val nestePeriodeFom = nesteInntektsperiode.periode.fom

            val skalSlåSammenPerioder =
                inntektsperiode.månedsinntekt == nesteInntektsperiode.månedsinntekt &&
                    inntektsperiode.inntekt == nesteInntektsperiode.inntekt &&
                    inntektsperiode.dagsats == nesteInntektsperiode.dagsats &&
                    inntektsperiode.samordningsfradrag == nesteInntektsperiode.samordningsfradrag &&
                    periodeTom.plusMonths(1) == nestePeriodeFom

            if (skalSlåSammenPerioder) {
                inntektsperiode = inntektsperiode.copy(periode = Månedsperiode(inntektsperiode.periode.fom, nesteInntektsperiode.periode.tom))
            } else {
                sammenslåttInntektsperioder.add(inntektsperiode)
                inntektsperiode = nesteInntektsperiode
            }
        }
        sammenslåttInntektsperioder.add(inntektsperiode)
        return sammenslåttInntektsperioder
    }

    private fun lagInntektsperiodeTekst(
        inntektsperioder: List<Inntektsperiode>,
        inntektResponse: InntektResponse,
        forrigeVedtak: Vedtak,
        erForrigeBehandlingGOmregning: Boolean,
    ): String {
        val førsteMånedMed10ProsentEndring =
            inntektsperioder
                .minBy { it.periode.fom }
                .periode.fom
                .minusMonths(1)

        val forrigeForventetInntektsperiode = forrigeVedtak.inntekter?.inntekter?.first() ?: throw IllegalStateException("Fant ikke tidligere forventet inntekt for måned: $førsteMånedMed10ProsentEndring")
        val periodeForFørsteMånedMed10ProsentEndring = forrigeVedtak.inntekter.inntekter.first { it.periode.inneholder(førsteMånedMed10ProsentEndring) }
        val forrigeForventetÅrsinntekt = BeregningUtils.beregnTotalinntekt(periodeForFørsteMånedMed10ProsentEndring).toInt()
        val tiProsentOppOgNed = BeregningUtils.beregn10ProsentOppOgNedIMånedsinntektFraÅrsinntekt(periodeForFørsteMånedMed10ProsentEndring)

        val sisteInntektsperiode = forrigeVedtak.inntekter.inntekter.last()
        val forrigeForventetÅrsinntektG = BeregningUtils.beregnTotalinntekt(sisteInntektsperiode).toInt()
        val tiProsentOppOgNedG = BeregningUtils.beregn10ProsentOppOgNedIMånedsinntektFraÅrsinntekt(sisteInntektsperiode)

        val beløpFørsteMåned10ProsentEndring = inntektResponse.totalInntektForÅrMåned(førsteMånedMed10ProsentEndring)

        val forventetInntekt = inntektsperioder.maxBy { it.periode.fom }

        if (forrigeForventetÅrsinntekt == 0) {
            return FlettefelterForInntektsbegrunnelseForInntektUnderHalvG(
                førsteMånedMed10ProsentEndring = førsteMånedMed10ProsentEndring,
                forventetÅrsinntektNår10ProsentEndring = forrigeForventetÅrsinntekt,
                månedsinntektFørsteMåned10ProsentEndring = beløpFørsteMåned10ProsentEndring,
                harFeriepenger = inntektResponse.finnesFeriepengerFraOgMedÅrMåned(YearMonth.now().minusMonths(3)),
            ).genererInntektsbegrunnelse
        }

        return FlettefelterForInntektsbegrunnelse(
            kontrollperiodeFraOgMed = if (forrigeForventetInntektsperiode.periode.fom.isBefore(YearMonth.now().minusYears(1))) YearMonth.now().minusYears(1) else forrigeForventetInntektsperiode.periode.fom,
            kontrollperiodeTilOgMed = YearMonth.now().minusMonths(1),
            førsteMånedMed10ProsentEndring = førsteMånedMed10ProsentEndring,
            månedsinntektFørsteMåned10ProsentEndring = inntektResponse.totalInntektForÅrMåned(førsteMånedMed10ProsentEndring),
            forventetÅrsinntektNår10ProsentEndring = forrigeForventetÅrsinntekt,
            tiProsentOppOgNedFraForventetÅrsinntekt = tiProsentOppOgNed,
            nyForventetInntektFraOgMedDato = forventetInntekt.periode.fom,
            harFeriepenger = inntektResponse.finnesFeriepengerFraOgMedÅrMåned(YearMonth.now().minusMonths(3)),
            inntektsberegningGOmregning =
                InntektsberegningGOmregning(
                    erForrigeBehandlingGOmregning = erForrigeBehandlingGOmregning,
                    forrigeForventetÅrsinntektG = forrigeForventetÅrsinntektG,
                    tiProsentOppOgNed = tiProsentOppOgNedG,
                ),
        ).genererInntektsbegrunnelse
    }

    companion object {
        const val TYPE = "behandleAutomatiskInntektsendringTask"

        fun opprettTask(payload: String): Task {
            val payloadObject = objectMapper.readValue(payload, PayloadBehandleAutomatiskInntektsendringTask::class.java)

            return Task(
                type = TYPE,
                payload = payload,
                properties =
                    Properties().apply {
                        this["personIdent"] = payloadObject.personIdent
                    },
            )
        }
    }
}

data class PayloadBehandleAutomatiskInntektsendringTask(
    val personIdent: String,
    val ukeÅr: String,
)

data class LogInntekt(
    val årMåned: YearMonth,
    val inntekt: Double,
)

fun YearMonth.tilNorskFormat(): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("no-NO"))
    return this.format(formatter)
}

fun YearMonth.tilNorskFormatUtenÅr(): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("no-NO"))
    return this.format(formatter)
}
