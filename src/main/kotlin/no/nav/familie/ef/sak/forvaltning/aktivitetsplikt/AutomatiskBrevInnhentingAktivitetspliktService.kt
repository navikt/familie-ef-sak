package no.nav.familie.ef.sak.forvaltning.aktivitetsplikt

import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Year

@Service
class AutomatiskBrevInnhentingAktivitetspliktService(
    private val taskService: TaskService,
    private val oppgaveService: OppgaveService,
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    val oppgaveAktivitetspliktFrist = LocalDate.parse("2025-05-17")

    @Transactional
    fun opprettTasks(
        liveRun: Boolean,
        taskLimit: Int,
    ) {
        val mappeId = hentUtdanningsmappeId()

        val oppgaveFrist = oppgaveAktivitetspliktFrist
        val opppgaver =
            oppgaveService.hentOppgaver(
                FinnOppgaveRequest(
                    tema = Tema.ENF,
                    fristFomDato = oppgaveFrist,
                    fristTomDato = oppgaveFrist,
                    mappeId = mappeId.toLong(),
                    limit = taskLimit.toLong(),
                ),
            )

        val oppgaverUtenTilordnetRessurs = opppgaver.oppgaver.filter { it.tilordnetRessurs.isNullOrBlank() }

        logger.info("Fant ${oppgaverUtenTilordnetRessurs.size} oppgaver for utsending av automatisk brev ifb innhenting av aktivitetsplikt")

        oppgaverUtenTilordnetRessurs.forEach {
            val oppgaveId = it.id ?: throw Feil("Mangler oppgaveid")
            if (liveRun) {
                if (harOpprettetTaskTidligere(oppgaveId)) {
                    logger.warn("Oppretter ikke task for oppgave=$oppgaveId da denne er opprettet tidligere.")
                } else {
                    logger.info("Oppretter task for oppgaveId=${it.id}")
                    taskService.save(
                        SendAktivitetspliktBrevTilIverksettTask.opprettTask(
                            oppgaveId,
                            Year.now(),
                        ),
                    )
                }
            } else {
                logger.info("Dry run. Fant oppgave=$oppgaveId")
            }
        }
    }

    private fun harOpprettetTaskTidligere(
        oppgaveId: Long,
    ) = taskService.finnTaskMedPayloadOgType(
        SendAktivitetspliktBrevTilIverksettTask.opprettTaskPayload(oppgaveId, Year.now()),
        SendAktivitetspliktBrevTilIverksettTask.TYPE,
    ) != null

    private fun hentUtdanningsmappeId() = oppgaveService.finnMapper(OppgaveUtil.ENHET_NR_NAY).single { it.navn == "64 Utdanning" }.id
}
