package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/patch-g-omregning-behandlinger"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Unprotected
@Validated
class PatchGOmregningBehandlingerController(
    private val behandlingRepository: BehandlingRepository,
    private val taskRepository: TaskRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("opprett-tasks")
    fun patchBehandlingsstatistikk(@RequestBody liveRun: LiveRun): Ressurs<String> {
        val behandlingerMedÅrsakGOmregning = behandlingRepository.findByÅrsak(BehandlingÅrsak.G_OMREGNING)

        logger.info(
            "Patch behandlingsstatistikk: Funnet ${behandlingerMedÅrsakGOmregning.size} behandlinger med årsak G-Omregning. " +
                "Oppretter task med status påbegynt og ferdig for disse."
        )

        behandlingerMedÅrsakGOmregning.forEach {
            val taskPåbegynt = BehandlingsstatistikkTask.opprettTask(
                it.id,
                Hendelse.PÅBEGYNT,
                it.sporbar.opprettetTid,
                "VL",
                null
            )

            val taskFerdigstilt = BehandlingsstatistikkTask.opprettTask(
                it.id,
                Hendelse.FERDIG,
                it.sporbar.opprettetTid,
                "VL",
                null
            )
            if (liveRun.skalPersistere) {
                taskRepository.save(taskPåbegynt)
                taskRepository.save(taskFerdigstilt)
            }
        }

        return if (liveRun.skalPersistere) Ressurs.success("patch send behandlingsstatistikk kjørt")
        else Ressurs.success("Patch ikke kjørt.")
    }
}

data class LiveRun(val skalPersistere: Boolean)
