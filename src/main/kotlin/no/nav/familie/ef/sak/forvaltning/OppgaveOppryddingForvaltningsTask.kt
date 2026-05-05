package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = OppgaveOppryddingForvaltningsTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Rydd fremleggsoppgaver som ligger med frist 18.mai i revurderingsmappa",
)
class OppgaveOppryddingForvaltningsTask(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    final val mappenavnProd = "41 Revurdering"
    final val mappenavnDev = "41 - Revurdering"
    val mappeNavn = listOf(mappenavnDev, mappenavnProd)

    override fun doTask(task: Task) {
        val runType = jsonMapper.readValue<OppgaveOppryddingForvaltningsController.RunType>(task.payload)
        logger.info("Starter opprydding av oppgaver")

        val mappeId = hentFremleggMappeId()
        logger.info("Funnet mappe: $mappeId")

        val oppgaveFrist = LocalDate.of(2024, 5, 18)
        val oppgaver =
            oppgaveService.hentOppgaver(
                FinnOppgaveRequest(
                    tema = Tema.ENF,
                    fristFomDato = oppgaveFrist,
                    fristTomDato = oppgaveFrist,
                    mappeId = mappeId.toLong(),
                    limit = 1000,
                ),
            )

        logger.info("Antall oppgaver funnet: ${oppgaver.antallTreffTotalt}. Antall oppgaver hentet ut: ${oppgaver.oppgaver.size}")

        if (runType.liveRun) {
            oppgaver.oppgaver.forEach { oppgave ->
                val oppgaveId = oppgave.id
                if (oppgaveId == null) {
                    logger.error("Kan ikke ferdigstille oppgave - mangler ID")
                    secureLogger.info("Kan ikke ferdigstille oppgave pga manglende ID: $oppgave")
                } else if (oppgave.oppgavetype != Oppgavetype.Fremlegg.value) {
                    logger.error("Kan ikke ferdigstille oppgave - feil type")
                    secureLogger.info("Kan ikke ferdigstille oppgave pga feil oppgavetype: $oppgave")
                } else {
                    secureLogger.info("Ferdigstiller oppgave $oppgaveId")
                    oppgaveService.ferdigstillOppgave(oppgaveId)
                }
            }
        }
    }

    companion object {
        const val TYPE = "oppryddingAvForvaltningsoppgaver"

        fun opprettTask(runType: OppgaveOppryddingForvaltningsController.RunType): Task =
            Task(
                TYPE,
                jsonMapper.writeValueAsString(runType),
                Properties(),
            )
    }

    private fun hentFremleggMappeId() = oppgaveService.finnMapper(OppgaveUtil.ENHET_NR_NAY).single { mappeNavn.contains(it.navn) }.id
}
