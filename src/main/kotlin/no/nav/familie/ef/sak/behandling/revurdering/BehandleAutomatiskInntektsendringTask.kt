package no.nav.familie.ef.sak.behandling.revurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.beregning.tilInntekt
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.vedtak.VedtakService
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
import java.time.LocalDate
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

                val forrigeBehandling = behandling.forrigeBehandlingId?.let { behandlingService.hentBehandling(it) } ?: throw IllegalStateException("Burde vært en forrigeBehandlingId etter automatisk revurdering for behandlingId: ${behandling.id}")
                val forrigeVedtak = vedtakService.hentVedtak(forrigeBehandling.id)
                val innvilgelseOvergangsstønad =
                    InnvilgelseOvergangsstønad(
                        periodeBegrunnelse = forrigeVedtak.periodeBegrunnelse,
                        inntektBegrunnelse = forrigeVedtak.inntektBegrunnelse,
                        perioder = forrigeVedtak.perioder?.perioder?.fraDomene() ?: emptyList(),
                        inntekter = forrigeVedtak.inntekter?.inntekter?.tilInntekt() ?: emptyList(),
                        samordningsfradragType = forrigeVedtak.samordningsfradragType,
                    )

                årsakRevurderingsRepository.insert(ÅrsakRevurdering(behandlingId = behandling.id, opplysningskilde = Opplysningskilde.OPPLYSNINGER_INTERNE_KONTROLLER, årsak = Revurderingsårsak.ENDRING_INNTEKT, beskrivelse = null))
                vedtakService.lagreVedtak(vedtakDto = innvilgelseOvergangsstønad, behandlingId = behandling.id, stønadstype = StønadType.OVERGANGSSTØNAD)
                automatiskRevurderingService.lagreInntektResponse(personIdent, behandling.id)
                logger.info("Opprettet behandling for automatisk inntektsendring: ${behandling.id}")
            } else {
                secureLogger.error("Finner ikke fagsak for personIdent=$personIdent på stønadstype=${StønadType.OVERGANGSSTØNAD} under automatisk inntektsendring")
            }
        } else {
            logger.info("Toggle for automatisering av inntekt er AV. Ville opprettet revurdering for fagsak=${fagsak?.id} med en forventetInntekt på X og revurdert fra dato: Y")
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
