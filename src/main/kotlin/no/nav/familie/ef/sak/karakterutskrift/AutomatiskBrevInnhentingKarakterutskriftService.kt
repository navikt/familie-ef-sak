package no.nav.familie.ef.sak.karakterutskrift

import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil
import no.nav.familie.kontrakter.felles.Behandlingstema
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
class AutomatiskBrevInnhentingKarakterutskriftService(
    private val taskService: TaskService,
    private val oppgaveService: OppgaveService,
) {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    val fristHovedperiode = LocalDate.parse("2023-05-17")
    val fristutvidet = LocalDate.parse("2023-05-18")

    @Transactional
    fun opprettTasks(brevtype: KarakterutskriftBrevtype, liveRun: Boolean) {
        val mappeId = oppgaveService.finnMapper(OppgaveUtil.ENHET_NR_NAY)
            .single { it.navn == "64 Utdanning" }.id

        val oppgaveFrist = when (brevtype) {
            KarakterutskriftBrevtype.HOVEDPERIODE -> fristHovedperiode
            KarakterutskriftBrevtype.UTVIDET -> fristutvidet
        }
        val opppgaver = oppgaveService.hentOppgaver(
            FinnOppgaveRequest(
                tema = Tema.ENF,
                behandlingstema = Behandlingstema.Skolepenger,
                fristFomDato = oppgaveFrist,
                fristTomDato = oppgaveFrist,
                mappeId = mappeId.toLong(),
                limit = 5000,
            ),
        )

        logger.info("Fant ${opppgaver.oppgaver.size} oppgaver for utsending av automatisk brev ifb innhenting av karakterutskrift")

        opppgaver.oppgaver.forEach {
            val oppgaveId = it.id ?: throw Feil("Mangler oppgaveid")
            if (liveRun) {
                logger.info("Oppretter task for oppgaveId=${it.id} og brevtype=$brevtype")
                taskService.save(KarakterutskriftBrevTask.opprettTask(oppgaveId, brevtype, Year.now()))
            } else {
                logger.info("Dry run. Fant oppgave=$oppgaveId og brevtype=$brevtype")
            }
        }
    }
}
