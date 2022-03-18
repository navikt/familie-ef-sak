package no.nav.familie.ef.sak.vedtak.uttrekk

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.DatoFormat.DATE_FORMAT_ISO_YEAR_MONTH
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.Companion.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
@TaskStepBeskrivelse(
        taskStepType = OpprettUttrekkArbeidssøkerTask.TYPE,
        beskrivelse = "Oppretter uttrekk av arbeidssøkere"
)
class OpprettUttrekkArbeidssøkerTask(
        private val uttrekkArbeidssøkerService: UttrekkArbeidssøkerService,
        private val fagsakService: FagsakService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val årMåned = objectMapper.readValue<YearMonth>(task.payload)
        val uttrekk = uttrekkArbeidssøkerService.hentArbeidssøkereForUttrekk(årMåned)
        val aktiveIdenter = fagsakService.hentAktiveIdenter(uttrekk.map { it.fagsakId }.toSet())

        var feilede = 0
        uttrekk.forEach {
            if (uttrekkArbeidssøkerService.uttrekkFinnes(årMåned, it.fagsakId)) {
                return@forEach
            }
            try {
                uttrekkArbeidssøkerService.opprettUttrekkArbeidssøkere(årMåned = årMåned,
                                                                       fagsakId = it.fagsakId,
                                                                       behandlingIdForVedtak = it.behandlingIdForVedtak,
                                                                       personIdent = aktiveIdenter[it.fagsakId]
                                                                                     ?: error("Kunne ikke finne fagsakID"))
            } catch (ex: Exception) {
                val errorMelding = "Sjekk av utrekkArbeidssøker feiler fagsak=$it.fagsakId behandling=$it.behandlingId"
                logger.error(errorMelding)
                secureLogger.error("$errorMelding - ${ex.message}", ex)
                ++feilede
            }
        }
        if (feilede > 0) {
            error("Kunne ikke opprette $feilede av ${uttrekk.size} uttrekk")
        }
    }

    override fun onCompletion(task: Task) {
        opprettTaskForNesteMåned()
    }

    fun opprettTaskForNesteMåned() {
        val nesteMåned = YearMonth.now().plusMonths(1)
        val triggerTid = nesteMåned.atDay(1).atTime(5, 0)
        taskRepository.save(Task(TYPE, nesteMåned.format(DATE_FORMAT_ISO_YEAR_MONTH)).medTriggerTid(triggerTid))
    }

    companion object {

        const val TYPE = "opprettUttrekkArbeidssøker"
    }
}
