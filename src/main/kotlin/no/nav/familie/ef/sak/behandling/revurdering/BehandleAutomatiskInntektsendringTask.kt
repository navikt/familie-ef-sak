package no.nav.familie.ef.sak.behandling.revurdering

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

data class PayloadBehandleAutomatiskInntektsendringTask(
    val personIdent: String,
    val ukeÅr: String,
)

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleAutomatiskInntektsendringTask.TYPE,
    beskrivelse = "Skal automatisk opprette en ny behandling ved automatisk inntektsendring",
)
class BehandleAutomatiskInntektsendringTask(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val featureToggleService: FeatureToggleService,
    private val automatiskRevurderingService: AutomatiskRevurderingService,
) : AsyncTaskStep {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val toggle = featureToggleService.isEnabled(Toggle.BEHANDLE_AUTOMATISK_INNTEKTSENDRING)

        val personIdent = task.payload
        val fagsak =
            fagsakService.finnFagsak(
                personIdenter = setOf(personIdent),
                stønadstype = StønadType.OVERGANGSSTØNAD,
            )

        loggInfoOpprett(personIdent, fagsak)

        if (toggle) {
            if (fagsak != null) {
                loggInfoOpprett(personIdent, fagsak)

                val behandling =
                    behandlingService.opprettBehandling(
                        behandlingType = BehandlingType.REVURDERING,
                        fagsakId = fagsak.id,
                        behandlingsårsak = BehandlingÅrsak.AUTOMATISK_INNTEKTSENDRING,
                    )

                automatiskRevurderingService.lagreInntektResponse(personIdent, behandling.id)
            } else {
                secureLogger.error("Finner ikke fagsak for personIdent=$personIdent på stønadstype=${StønadType.OVERGANGSSTØNAD} under automatisk inntektsendring")
            }
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

    fun loggInfoOpprett(
        personIdent: String,
        fagsak: Fagsak?,
    ) {
        secureLogger.info("Kan opprette behandling med $personIdent stønadstype=${StønadType.OVERGANGSSTØNAD} faksakId ${fagsak?.id}")
    }
}
