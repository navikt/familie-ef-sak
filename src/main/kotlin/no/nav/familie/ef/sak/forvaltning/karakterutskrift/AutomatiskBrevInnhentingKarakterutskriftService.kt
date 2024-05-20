package no.nav.familie.ef.sak.forvaltning.karakterutskrift

import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
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
    fun opprettTasks(
        brevtype: FrittståendeBrevType,
        liveRun: Boolean,
        taskLimit: Int,
    ) {
        val mappeId = hentUtdanningsmappeId()

        val oppgaveFrist = utledOppgavefrist(brevtype)
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

        logger.info("Fant ${oppgaverUtenTilordnetRessurs.size} oppgaver for utsending av automatisk brev ifb innhenting av karakterutskrift")

        oppgaverUtenTilordnetRessurs.forEach {
            val oppgaveId = it.id ?: throw Feil("Mangler oppgaveid")
            if (liveRun) {
                if (harOpprettetTaskTidligere(oppgaveId, brevtype)) {
                    logger.warn("Oppretter ikke task for oppgave=$oppgaveId da denne er opprettet tidligere.")
                } else {
                    logger.info("Oppretter task for oppgaveId=${it.id} og brevtype=$brevtype")
                    taskService.save(
                        SendKarakterutskriftBrevTilIverksettTask.opprettTask(
                            oppgaveId,
                            brevtype,
                            Year.now(),
                        ),
                    )
                }
            } else {
                logger.info("Dry run. Fant oppgave=$oppgaveId og brevtype=$brevtype")
            }
        }
    }

    private fun harOpprettetTaskTidligere(
        oppgaveId: Long,
        brevtype: FrittståendeBrevType,
    ) =
        taskService.finnTaskMedPayloadOgType(
            SendKarakterutskriftBrevTilIverksettTask.opprettTaskPayload(oppgaveId, brevtype, Year.now()),
            SendKarakterutskriftBrevTilIverksettTask.TYPE,
        ) != null

    private fun hentUtdanningsmappeId() =
        oppgaveService.finnMapper(OppgaveUtil.ENHET_NR_NAY).single { it.navn == "64 Utdanning" }.id

    private fun utledOppgavefrist(brevtype: FrittståendeBrevType) =
        when (brevtype) {
            FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE -> fristHovedperiode
            FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_UTVIDET_PERIODE -> fristutvidet
            else -> throw Feil("Skal ikke opprette automatiske innhentingsbrev for frittstående brev av type $brevtype")
        }
}
