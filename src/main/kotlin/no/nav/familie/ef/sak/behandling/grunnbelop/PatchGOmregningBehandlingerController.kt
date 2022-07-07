package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTaskPayload
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.BehandlingMetode
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

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
    @Transactional
    fun patchBehandlingsstatistikk(@RequestBody liveRun: LiveRun): Ressurs<String> {
        val behandlingerMedÅrsakGOmregning = behandlingRepository.findByÅrsak(BehandlingÅrsak.G_OMREGNING)

        logger.info(
            "Patch behandlingsstatistikk: Fant ${behandlingerMedÅrsakGOmregning.size} behandlinger med årsak G-omregning." +
                "Hvorav ${behandlingerMedÅrsakGOmregning.filter { it.sporbar.opprettetAv == "VL" }.size} opprettet automatisk. " +
                "Oppretter task med status påbegynt og ferdig for de som er opprettet automatisk. (skal persistere ${liveRun.skalPersistere})"
        )

        behandlingerMedÅrsakGOmregning.filter { it.sporbar.opprettetAv == "VL" }.forEach {
            val taskPåbegynt = opprettTask(
                it.id,
                Hendelse.PÅBEGYNT,
                it.sporbar.opprettetTid
            )

            val taskFerdigstilt = opprettTask(
                it.id,
                Hendelse.FERDIG,
                it.sporbar.endret.endretTid
            )
            if (liveRun.skalPersistere) {
                taskRepository.save(taskPåbegynt)
                taskRepository.save(taskFerdigstilt)
            }
        }

        return if (liveRun.skalPersistere) Ressurs.success("Patch send behandlingsstatistikk kjørt")
        else Ressurs.success("Patch ikke kjørt.")
    }
}

data class LiveRun(val skalPersistere: Boolean)

private fun opprettTask(
    behandlingId: UUID,
    hendelse: Hendelse,
    hendelseTidspunkt: LocalDateTime = LocalDateTime.now()
): Task =
    Task(
        type = BehandlingsstatistikkTask.TYPE,
        payload = objectMapper.writeValueAsString(
            BehandlingsstatistikkTaskPayload(
                behandlingId,
                hendelse,
                hendelseTidspunkt,
                "VL",
                null,
                BehandlingMetode.BATCH
            )
        )
    ).copy(
        metadataWrapper = PropertiesWrapper(
            Properties().apply {
                this["saksbehandler"] = "VL"
                this["behandlingId"] = behandlingId.toString()
                this["hendelse"] = hendelse.name
                this["hendelseTidspunkt"] = hendelseTidspunkt.toString()
                this["oppgaveId"] = ""
                this[MDCConstants.MDC_CALL_ID] = IdUtils.generateId()
            }
        )
    )
