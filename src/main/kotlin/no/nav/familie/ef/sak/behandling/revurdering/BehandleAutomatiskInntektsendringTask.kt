package no.nav.familie.ef.sak.behandling.revurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.beregning.tilInntekt
import no.nav.familie.ef.sak.fagsak.FagsakService
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
        secureLogger.info("Kan opprette behandling med $personIdent stønadstype=${StønadType.OVERGANGSSTØNAD} faksakId ${fagsak?.id}")

        if (toggle) {
            if (fagsak != null) {
                val behandling =
                    revurderingService.opprettRevurderingManuelt(
                        RevurderingDto(
                            fagsakId = fagsak.id,
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
                        inntekter = inntektsperioder.tilInntekt() ?: emptyList(),
                        samordningsfradragType = forrigeVedtak.samordningsfradragType,
                    )

                årsakRevurderingsRepository.insert(ÅrsakRevurdering(behandlingId = behandling.id, opplysningskilde = Opplysningskilde.AUTOMATISK_OPPRETTET_BEHANDLING, årsak = Revurderingsårsak.ENDRING_INNTEKT, beskrivelse = null))
                vedtakService.lagreVedtak(vedtakDto = innvilgelseOvergangsstønad, behandlingId = behandling.id, stønadstype = StønadType.OVERGANGSSTØNAD)
                logger.info("Opprettet behandling for automatisk inntektsendring: ${behandling.id}")
            } else {
                secureLogger.error("Finner ikke fagsak for personIdent=$personIdent på stønadstype=${StønadType.OVERGANGSSTØNAD} under automatisk inntektsendring")
            }
        } else {
            logger.info("Toggle for automatisering av inntekt er AV. Ville opprettet revurdering for fagsak=${fagsak?.id} med en forventetInntekt på X og revurdert fra dato: Y")
        }
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
                        .førsteMånedMed10ProsentØkning()
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
        val forventetInntekt = inntektResponse.forventetInntektMånedsinntekt()
        val inntekterMinimum3MndTilbake = forrigeVedtak.inntekter?.inntekter?.filter { it.periode.fomDato <= YearMonth.now().minusMonths(3).atDay(1) } ?: emptyList()
        val nyesteInntektsperiode = inntekterMinimum3MndTilbake.maxBy { it.periode.fomDato }
        val oppdatertInntektsperiode = nyesteInntektsperiode.copy(inntekt = BigDecimal(forventetInntekt))
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
