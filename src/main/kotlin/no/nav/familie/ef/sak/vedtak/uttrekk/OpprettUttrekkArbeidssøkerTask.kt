package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.DatoFormat.DATE_FORMAT_ISO_YEAR_MONTH
import no.nav.familie.ef.sak.felles.util.EnvUtil
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.time.YearMonth

/**
 * Si at det er januar nå.
 * Vi oppretter då en task for uttrekk i januar, som får triggertid i februar
 * Vi henter då ut om personen er registrert som arbeidssøker i siste januar
 */
@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettUttrekkArbeidssøkerTask.TYPE,
    beskrivelse = "Oppretter uttrekk av arbeidssøkere",
)
class OpprettUttrekkArbeidssøkerTask(
    private val uttrekkArbeidssøkerService: UttrekkArbeidssøkerService,
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
) : AsyncTaskStep {
    private val logger = Logg.getLogger(this::class)

    override fun doTask(task: Task) {
        val årMåned = YearMonth.parse(task.payload)
        val uttrekk = uttrekkArbeidssøkerService.hentArbeidssøkereForUttrekk(årMåned)
        val aktiveIdenter = fagsakService.hentAktiveIdenter(uttrekk.map { it.fagsakId }.toSet())

        var feilede = 0
        var antallOk = 0
        uttrekk.forEach {
            if (uttrekkArbeidssøkerService.uttrekkFinnes(årMåned, it.fagsakId)) {
                return@forEach
            }
            try {
                uttrekkArbeidssøkerService.opprettUttrekkArbeidssøkere(
                    årMåned = årMåned,
                    fagsakId = it.fagsakId,
                    behandlingIdForVedtak = it.behandlingIdForVedtak,
                    personIdent =
                        aktiveIdenter[it.fagsakId]
                            ?: error("Kunne ikke finne fagsakID"),
                )
                ++antallOk
            } catch (ex: Exception) {
                val errorMelding = "Sjekk av utrekkArbeidssøker feiler fagsak=${it.fagsakId} behandling=${it.behandlingId}"
                logger.warn(errorMelding)
                logger.warn("$errorMelding - ${ex.message}", ex)
                ++feilede
            }
        }

        logger.info("Opprettet uttrekk av arbeidssøkere for $antallOk av ${uttrekk.size}")
        if (feilede > 0 && !EnvUtil.erIDev()) {
            error("Kunne ikke opprette $feilede av ${uttrekk.size} uttrekk")
        }
    }

    override fun onCompletion(task: Task) {
        opprettTaskForNesteMåned(task)
    }

    fun opprettTaskForNesteMåned(task: Task) {
        val årMåned = YearMonth.parse(task.payload)
        taskService.save(opprettTask(årMåned.plusMonths(1)))
    }

    companion object {
        const val TYPE = "opprettUttrekkArbeidssøker"

        fun opprettTask(utrekksmåned: YearMonth): Task {
            val triggerTid = utrekksmåned.plusMonths(1).atDay(1).atTime(5, 0)
            return Task(TYPE, utrekksmåned.format(DATE_FORMAT_ISO_YEAR_MONTH)).medTriggerTid(triggerTid)
        }
    }
}
