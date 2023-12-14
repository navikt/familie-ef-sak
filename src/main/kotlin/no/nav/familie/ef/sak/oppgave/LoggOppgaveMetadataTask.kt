package no.nav.familie.ef.sak.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(
    taskStepType = LoggOppgaveMetadataTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Finn og logg metadata for oppgave knyttet til behandling",
)
class LoggOppgaveMetadataTask(
    private val tilordnetRessursService: TilordnetRessursService
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        logger.info("Henter oppgave for behandling ${task.payload}")
        val oppgave = tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandling(UUID.fromString(task.payload))

        when (oppgave) {
            null -> logger.info("Fant ikke oppgave for behandling ${task.payload}")
            else -> secureLogger.info("Oppgave hentet for behandling ${task.payload}: ${oppgave.toLogString()}")
        }
    }

    companion object {

        const val TYPE = "loggOppgaveMetadataTask"
        fun opprettTask(behandlingId: UUID): Task {
            return Task(
                TYPE,
                behandlingId.toString(),
                Properties(),
            )
        }
    }
}

// Uten beskrivelse, bnr, identer, aktørid og metadata
fun Oppgave.toLogString(): String {
    return "Oppgave(aktivDato=$aktivDato, behandlesAvApplikasjon=$behandlesAvApplikasjon, behandlingstema=$behandlingstema, behandlingstype=$behandlingstype, endretAv=$endretAv, endretAvEnhetsnr=$endretAvEnhetsnr, endretTidspunkt=$endretTidspunkt, ferdigstiltTidspunkt=$ferdigstiltTidspunkt, fristFerdigstillelse=$fristFerdigstillelse, id=$id, journalpostId=$journalpostId, journalpostkilde=$journalpostkilde, mappeId=$mappeId, oppgavetype=$oppgavetype, opprettetAv=$opprettetAv, opprettetAvEnhetsnr=$opprettetAvEnhetsnr, opprettetTidspunkt=$opprettetTidspunkt, orgnr=$orgnr, prioritet=$prioritet, saksreferanse=$saksreferanse, samhandlernr=$samhandlernr, status=$status, tema=$tema, temagruppe=$temagruppe, tildeltEnhetsnr=$tildeltEnhetsnr, tilordnetRessurs=$tilordnetRessurs, versjon=$versjon)"
}

