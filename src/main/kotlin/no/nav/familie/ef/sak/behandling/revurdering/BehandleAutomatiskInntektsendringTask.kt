package no.nav.familie.ef.sak.behandling.revurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.beregning.tilInntekt
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.fraDomene
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.felles.Opplysningskilde
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
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
        val forrigeVedtak = vedtakService.hentVedtak(forrigeBehandling.id)

        val perioder = oppdaterFørsteVedtaksperiodeMedRevurderesFraDato(forrigeVedtak, inntektResponse)
        val inntektsperioder = oppdaterInntektMedNyBeregnetForventetInntekt(forrigeVedtak, inntektResponse)
        val innvilgelseOvergangsstønad =
            InnvilgelseOvergangsstønad(
                periodeBegrunnelse = forrigeVedtak.periodeBegrunnelse,
                inntektBegrunnelse = forrigeVedtak.inntektBegrunnelse,
                perioder = perioder.fraDomene(),
                inntekter = inntektsperioder.tilInntekt(),
                samordningsfradragType = forrigeVedtak.samordningsfradragType,
            )

        årsakRevurderingsRepository.insert(ÅrsakRevurdering(behandlingId = behandling.id, opplysningskilde = Opplysningskilde.AUTOMATISK_OPPRETTET_BEHANDLING, årsak = Revurderingsårsak.ENDRING_INNTEKT, beskrivelse = null))
        vedtakService.lagreVedtak(vedtakDto = innvilgelseOvergangsstønad, behandlingId = behandling.id, stønadstype = StønadType.OVERGANGSSTØNAD)
        logger.info("Opprettet behandling for automatisk inntektsendring: ${behandling.id}")
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
        val startdatoForrigeVedtak =
            if (behandling != null) {
                vedtakService
                    .hentVedtak(behandling.id)
                    .perioder
                    ?.perioder
                    ?.firstOrNull()
                    ?.periode
                    ?.fom ?: YearMonth.now()
            } else {
                logger.info("Fant ikke siste iverksatte behandling for fagsakId: ${fagsak.id}")
                YearMonth.now()
            }

        logger.info("Toggle for automatisering av inntekt er AV. Ville opprettet revurdering for fagsak eksternId=${fagsak.eksternId} med en forventetInntekt på $forventetInntekt og revurdert fra dato: ${inntektResponse.revurderesFraDato(startdatoForrigeVedtak)}")
    }

    private fun oppdaterFørsteVedtaksperiodeMedRevurderesFraDato(
        forrigeVedtak: Vedtak,
        inntektResponse: InntektResponse,
    ): List<Vedtaksperiode> {
        val førstePeriode = forrigeVedtak.perioder?.perioder?.first()
        val førstePeriodeMedRevurderesFraDato =
            førstePeriode?.copy(
                datoFra =
                    inntektResponse
                        .førsteMånedOgInntektMed10ProsentØkning(førstePeriode.periode.fom)
                        ?.first
                        ?.atDay(1)
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

    private fun oppdaterInntektMedNyBeregnetForventetInntekt(
        forrigeVedtak: Vedtak,
        inntektResponse: InntektResponse,
    ): List<Inntektsperiode> {
        val forventetÅrsinntekt = inntektResponse.forventetMånedsinntekt() * 12
        val inntekterMinimum3MndTilbake = forrigeVedtak.inntekter?.inntekter?.filter { it.periode.fomDato <= YearMonth.now().minusMonths(3).atDay(1) } ?: emptyList()
        val nyesteInntektsperiode = inntekterMinimum3MndTilbake.maxBy { it.periode.fomDato }
        val oppdatertInntektsperiode = nyesteInntektsperiode.copy(inntekt = BigDecimal(forventetÅrsinntekt))
        return forrigeVedtak.inntekter
            ?.inntekter
            ?.minus(nyesteInntektsperiode)
            ?.plus(oppdatertInntektsperiode) ?: emptyList()
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
