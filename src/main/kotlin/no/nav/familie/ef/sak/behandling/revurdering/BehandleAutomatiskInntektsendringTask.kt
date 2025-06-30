package no.nav.familie.ef.sak.behandling.revurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.beregning.BeregningUtils
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
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
import java.text.NumberFormat
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
        vedtak1: Vedtak,
        vedtak2: Vedtak,
    ): Vedtak {
        val inntekter1 =
            vedtak1.inntekter?.inntekter
                ?: throw IllegalStateException("Fant ikke inntektsperioder for behandlingId: ${vedtak1.behandlingId}")

        val inntekter2 = vedtak2.inntekter?.inntekter ?: emptyList()

        val justerteInntekter1 =
            inntekter1.mapNotNull { v1 ->
                val overlappende = inntekter2.firstOrNull { v2 -> v1.periode.overlapper(v2.periode) }

                if (overlappende != null) {
                    val nyPeriode = Månedsperiode(v1.periode.fom, overlappende.periode.fom.minusMonths(1))
                    if (nyPeriode.fom <= nyPeriode.tom) {
                        v1.copy(periode = nyPeriode)
                    } else {
                        throw IllegalStateException("Feil ved avkorting av periode. Ugyldig start- og sluttdato for periode: $nyPeriode i behandlingId: ${vedtak1.behandlingId}")
                    }
                } else {
                    v1
                }
            }

        val sammenslått = justerteInntekter1 + inntekter2

        return vedtak1.copy(inntekter = InntektWrapper(sammenslått))
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
            val forrigeVedtak =
                if (behandling.erGOmregning()) {
                    val vedtakFørGOmregning = vedtakService.hentVedtak(behandling.forrigeBehandlingId ?: throw IllegalStateException("Finner ikke forrigeBehandlingId for behandlingId som er en G-omregning: ${behandling.id}"))
                    val gOmregningVedtak = vedtakService.hentVedtak(behandling.id)
                    sammenslåVedtak(vedtakFørGOmregning, gOmregningVedtak)
                } else {
                    vedtakService.hentVedtak(behandling.id)
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
        if (revurderesFra.isBefore(YearMonth.now())) {
            val inntektsperioder =
                generateSequence(revurderesFra) { måned -> måned.plusMonths(1) }
                    .takeWhile { måned -> måned.isEqualOrBefore(YearMonth.now().minusMonths(1)) }
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
                    periode = Månedsperiode(YearMonth.now(), forrigeVedtak.perioder?.perioder?.maxOf { it.periode.tom } ?: throw IllegalStateException("Mangler vedtaksperioder")),
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

    fun slåSammenPerioderMedLikInntekt(perioder: List<Inntektsperiode>): List<Inntektsperiode> {
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
        forrigeBehandlingGOmregning: Boolean,
    ): String {
        val førsteMånedMed10ProsentEndring =
            inntektsperioder
                .minBy { it.periode.fom }
                .periode.fom
                .minusMonths(1)

        val forrigeForventetInntektsperiode = forrigeVedtak.inntekter?.inntekter?.first { it.periode.inneholder(førsteMånedMed10ProsentEndring) } ?: throw IllegalStateException("Fant ikke tidligere forventet inntekt for måned: $førsteMånedMed10ProsentEndring")
        val forrigeForventetÅrsinntekt = BeregningUtils.beregnTotalinntekt(forrigeForventetInntektsperiode).toInt()
        val tiProsentOppOgNed = BeregningUtils.beregn10ProsentOppOgNedIMånedsinntektFraÅrsinntekt(forrigeForventetInntektsperiode)

        val sisteInntektsperiode = forrigeVedtak.inntekter.inntekter.last()
        val forrigeForventetÅrsinntektG = BeregningUtils.beregnTotalinntekt(sisteInntektsperiode).toInt()
        val tiProsentOppOgNedG = BeregningUtils.beregn10ProsentOppOgNedIMånedsinntektFraÅrsinntekt(sisteInntektsperiode)

        val beløpFørsteMåned10ProsentEndring = inntektResponse.totalInntektForÅrMåned(førsteMånedMed10ProsentEndring)

        val forventetInntekt = inntektsperioder.maxBy { it.periode.fom }
        val forventetInntektFraMåned = forventetInntekt.periode.fom

        val tekst =
            """
            Periode som er kontrollert: ${inntektResponse.inntektsmåneder.minBy { it.måned }.måned.tilNorskFormat()} til ${inntektResponse.inntektsmåneder.maxBy { it.måned }.måned.tilNorskFormat()}.
            
            Forventet årsinntekt fra ${førsteMånedMed10ProsentEndring.tilNorskFormat()}: ${forrigeForventetÅrsinntekt.tilNorskFormat()} kroner.
            - 10 % opp: ${tiProsentOppOgNed.opp.tilNorskFormat()} kroner per måned.
            - 10 % ned: ${tiProsentOppOgNed.ned.tilNorskFormat()} kroner per måned.
            ${tekstTypeForGOmregningOppOgNed(forrigeBehandlingGOmregning, forrigeForventetÅrsinntektG, tiProsentOppOgNedG)}
            Inntekten i ${førsteMånedMed10ProsentEndring.tilNorskFormat()} er ${beløpFørsteMåned10ProsentEndring.tilNorskFormat()} kroner. Inntekten har økt minst 10 prosent denne måneden og alle månedene etter dette. Stønaden beregnes på nytt fra måneden etter 10 prosent økning.
            
            Har lagt til grunn faktisk inntekt bakover i tid. Fra og med ${forventetInntektFraMåned.tilNorskFormat()} er stønaden beregnet ut ifra gjennomsnittlig inntekt for ${forventetInntektFraMåned.minusMonths(3).månedTilNorskFormat()}, ${forventetInntektFraMåned.minusMonths(2).månedTilNorskFormat()} og ${forventetInntektFraMåned.minusMonths(1).månedTilNorskFormat()}.
            
            A-inntekt er lagret.
            """.trimIndent()

        return tekst
    }

    fun YearMonth.månedTilNorskFormat(): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("no-NO"))
        return this.format(formatter)
    }

    fun Int.tilNorskFormat(): String {
        val formatter = NumberFormat.getInstance(Locale.forLanguageTag("no-NO"))
        return formatter.format(this)
    }

    fun tekstTypeForGOmregningOppOgNed(
        forrigeBehandlingGOmregning: Boolean,
        forrigeForventetÅrsinntektG: Int,
        tiProsentOppOgNedG: BeregningUtils.TiProsentOppOgNed,
    ): String {
        if (forrigeBehandlingGOmregning) {
            return """
            Forventet årsinntekt fra ${Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed.tilNorskFormat()}: ${forrigeForventetÅrsinntektG.tilNorskFormat()} kroner.
            - 10 % opp: ${tiProsentOppOgNedG.opp.tilNorskFormat()} kroner per måned.
            - 10 % ned: ${tiProsentOppOgNedG.ned.tilNorskFormat()} kroner per måned.
            """
        } else {
            return """"""
        }
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
