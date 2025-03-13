package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillOppgavetypePåBehandlingTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Skal ferdigstille oppgave i EF-sak og Gosys hvis det er flere oppgaver av samme type på behandling",
)
class FerdigstillOppgavetypePåBehandlingTask(
    private val oppgaveService: OppgaveService,
    private val oppgaveRepository: OppgaveRepository,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue(task.payload, ForvaltningFerdigstillRequest::class.java)
        val (behandlingId, oppgavetype) = payload

        logger.info("Ferdigstiller oppgavetype $oppgavetype for behandling $behandlingId")

        if (oppgavetype != Oppgavetype.BehandleSak && oppgavetype != Oppgavetype.GodkjenneVedtak) {
            throw IllegalArgumentException("Kan kun ferdigstille oppgavetype BehandleSak eller GodkjenneVedtak")
        }

        val efOppgaver = oppgaveService.finnAlleOppgaverPåBehandlingSomIkkeErFerdigstiltOgHarLikOppgavetype(behandlingId, oppgavetype)

        if (efOppgaver != null && efOppgaver.size > 1) {
            val sisteOppgave = efOppgaver.last()
            sisteOppgave.let {
                // Setter oppgave til ferdigstilt i EF
                oppgaveRepository.update(it.copy(erFerdigstilt = true))
                // Ferdigstiller oppgave i Gosys
                oppgaveService.ferdigstillOppgave(it.id.mostSignificantBits)
            }
        }
    }

    companion object {
        const val TYPE = "FerdigstillOppgavetypePåBehandlingTask"

        fun opprettTask(
            request: ForvaltningFerdigstillRequest,
        ): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(request),
                properties = Properties(),
            )
    }
}
