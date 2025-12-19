package no.nav.familie.ef.sak.arbeidsforhold

import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdClient
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = LoggArbeidsforholdForPersonTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Logger arbeidsforhold for gitt person",
)
class LoggArbeidsforholdForPersonTask(
    private val arbeidsforholdClient: ArbeidsforholdClient,
) : AsyncTaskStep {
    private val logger = Logg.getLogger(this::class)

    override fun doTask(task: Task) {
        val personIdent = task.payload
        logger.info("Logg arbeidsforhold for person $personIdent")
        val arbeidsforhold = arbeidsforholdClient.hentArbeidsforhold(personIdent)
        arbeidsforhold.forEach { arbeidsforhold ->
            logger.info("Arbeidsforhold for person $personIdent: $arbeidsforhold")
        }
    }

    companion object {
        const val TYPE = "loggArbeidsforholdForPersonTask"

        fun opprettTask(payload: String): Task =
            Task(
                type = TYPE,
                payload = payload,
                properties =
                    Properties().apply {
                        this["personIdent"] = payload
                    },
            )
    }
}
