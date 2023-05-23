package no.nav.familie.ef.sak.karakterutskrift

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Year
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = KarakterutskriftBrevTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Automatisk utsendt brev for innhenting av karakterutskrift",
)
class KarakterutskriftBrevTask(
    private val fagsakService: FagsakService,
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue<AutomatiskBrevKarakterutskriftPayload>(task.payload)
        val oppgave = oppgaveService.hentOppgave(payload.oppgaveId)
        val ident = OppgaveUtil.finnPersonidentForOppgave(oppgave) ?: throw Feil("Fant ikke ident for oppgave=${oppgave.id}")
        val fagsakId = fagsakService.finnFagsak(setOf(ident), StønadType.SKOLEPENGER)?.id
            ?: throw Feil("Fant ikke fagsak for oppgave med id=${oppgave.id}")

        // TODO: valider har fagsak med løpende behandling?

        // TODO: opprett brev

        // TODO: journalfør brev

        // TODO: distribuer brev

        // TODO: oppdater oppgave

        throw Feil("Task for innhenting av karakterutskrift er ikke implementert")
    }

    companion object {

        fun opprettTask(oppgaveId: Long, karakterutskriftBrevtype: KarakterutskriftBrevtype, år: Year): Task {
            val payload = objectMapper.writeValueAsString(
                AutomatiskBrevKarakterutskriftPayload(
                    oppgaveId,
                    karakterutskriftBrevtype,
                    år,
                ),
            )

            val properties = Properties().apply {
                setProperty("oppgaveId", oppgaveId.toString())
                setProperty(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
            }

            return Task(TYPE, payload, properties)
        }

        const val TYPE = "KarakterutskriftBrev"
    }
}

data class AutomatiskBrevKarakterutskriftPayload(
    val oppgaveId: Long,
    val brevtype: KarakterutskriftBrevtype,
    val år: Year,
)

enum class KarakterutskriftBrevtype {
    HOVEDPERIODE,
    UTVIDET,
}
