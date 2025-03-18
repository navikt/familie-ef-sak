package no.nav.familie.ef.sak.behandling.revurdering

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleAutomatiskInntektsendringTask.TYPE,
    beskrivelse = "Skal automatisk opprette en ny behandling ved automatisk inntektsendring",
)
class BehandleAutomatiskInntektsendringTask(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
) : AsyncTaskStep {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val personIdent = task.payload
        val fagsak =
            fagsakService.finnFagsak(
                personIdenter = setOf(personIdent),
                stønadstype = StønadType.OVERGANGSSTØNAD,
            )

//        if (fagsak != null) {
//            behandlingService.opprettBehandling(
//                behandlingType = BehandlingType.REVURDERING,
//                fagsakId = fagsak.id,
//                behandlingsårsak = BehandlingÅrsak.AUTOMATISK_INNTEKTSENDRING,
//            )
//        } else {
//            secureLogger.error("Finner ikke fagsak for personIdent=$personIdent på stønadstype=${StønadType.OVERGANGSSTØNAD} under automatisk inntektsendring")
//        }

        secureLogger.info("LOGGER --- Kan opprette behandling med $personIdent stønadstype=${StønadType.OVERGANGSSTØNAD} faksakId ${fagsak?.id}")
    }

    companion object {
        const val TYPE = "behandleAutomatiskInntektsendringTask"

        fun opprettTask(personIdent: String): Task =
            Task(
                type = TYPE,
                payload = personIdent,
                properties =
                    Properties().apply {
                        this["personIdent"] = personIdent
                    },
            )
    }
}
