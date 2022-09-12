package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.opplysninger.mapper.finnBesteMatchPåFødselsnummerForTermindato
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BarnFyllerÅrOppfølgingsoppgaveService(
    private val gjeldendeBarnRepository: GjeldendeBarnRepository,
    private val oppgaveRepository: OppgaveRepository,
    private val taskRepository: TaskRepository,
    private val pdlClient: PdlClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun opprettTasksForAlleBarnSomHarFyltÅr(dryRun: Boolean = false) {
        val dagensDato = LocalDate.now()
        val alleBarnIGjeldendeBehandlinger =
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, dagensDato) +
                gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(StønadType.OVERGANGSSTØNAD, dagensDato)

        logger.info("Antall barn i gjeldende behandlinger: ${alleBarnIGjeldendeBehandlinger.size}")

        val skalOpprettes = lagOpprettOppgaveForBarn(alleBarnIGjeldendeBehandlinger)
        logger.info("Oppretter oppgave for ${skalOpprettes.size} barn. (dry-run: $dryRun)")

        if (!dryRun) {
            opprettOppgaveTasksForBarn(skalOpprettes)
        } else {
            skalOpprettes.forEach {
                secureLogger.info(
                    "Ville opprettet oppgave for barn med fødselsnummer: " +
                        "${it.fødselsnummer} med alder ${it.alder}"
                )
            }
        }
        logger.info("Oppretting av oppfølgingsoppgave-tasks ferdig")
    }

    private fun hentFødselsnummerTilTermindatoBarn(barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>): List<BarnTilUtplukkForOppgave> {
        val barnUtenFødselsnummer =
            barnTilUtplukkForOppgave.filter { it.termindatoBarn != null && it.fødselsnummerBarn == null }
        return barnTilUtplukkForOppgave.filter { it.fødselsnummerBarn != null } +
            finnFødselsnummerTilTerminbarn(barnUtenFødselsnummer)
    }

    private fun finnFødselsnummerTilTerminbarn(barnMedTermindato: List<BarnTilUtplukkForOppgave>): List<BarnTilUtplukkForOppgave> {
        val pdlPersonMedForelderBarnRelasjon =
            pdlClient.hentPersonForelderBarnRelasjon(barnMedTermindato.map { it.fødselsnummerSøker })

        return barnMedTermindato.map { barn ->
            val termindato = barn.termindatoBarn!!
            val søkerPdlData = pdlPersonMedForelderBarnRelasjon[barn.fødselsnummerSøker]
                ?: error("Finner ikke pdldata for søker=${barn.fødselsnummerSøker}")
            val forelderBarnRelasjoner = søkerPdlData.forelderBarnRelasjon.mapNotNull { it.relatertPersonsIdent }
            val besteMatch = finnBesteMatchPåFødselsnummerForTermindato(forelderBarnRelasjoner, termindato)

            barn.copy(fødselsnummerBarn = besteMatch)
        }
    }

    private fun lagOpprettOppgaveForBarn(barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>): Set<OpprettOppgaveForBarn> {
        val barn = hentFødselsnummerTilTermindatoBarn(barnTilUtplukkForOppgave)
        val oppgaverForBarn = opprettOppgaveForBarn(barn)
        val opprettedeOppgaver = finnOpprettedeOppgaver(oppgaverForBarn)

        return oppgaverForBarn
            .filterNot { opprettedeOppgaver.contains(FødselsnummerOgAlder(it.fødselsnummer, it.alder)) }
            .toSet()
    }

    private fun opprettOppgaveForBarn(barn: List<BarnTilUtplukkForOppgave>): List<OpprettOppgaveForBarn> {
        return barn.mapNotNull {
            Alder.fromFødselsdato(fødselsdato(it))?.let { alder ->
                OpprettOppgaveForBarn(
                    it.fødselsnummerBarn!!,
                    it.fødselsnummerSøker,
                    alder,
                    it.behandlingId
                )
            }
        }
    }

    private fun finnOpprettedeOppgaver(oppgaverForBarn: List<OpprettOppgaveForBarn>): Set<FødselsnummerOgAlder> {
        if (oppgaverForBarn.isEmpty()) return emptySet()

        return oppgaveRepository.findByTypeAndAlderIsNotNullAndBarnPersonIdenter(
            Oppgavetype.InnhentDokumentasjon,
            oppgaverForBarn.map { it.fødselsnummer }
        ).mapNotNull {
            if (it.barnPersonIdent != null && it.alder != null) {
                FødselsnummerOgAlder(it.barnPersonIdent, it.alder)
            } else {
                null
            }
        }.toSet()
    }

    private fun opprettOppgaveTasksForBarn(oppgaver: Set<OpprettOppgaveForBarn>) {
        if (oppgaver.isEmpty()) return

        oppgaver.map {
            OpprettOppgavePayload(
                it.behandlingId,
                it.fødselsnummer,
                it.fødselsnummerSøker,
                it.alder
            )
        }.forEach {
            try {
                taskRepository.save(OpprettOppfølgingsoppgaveForBarnFyltÅrTask.opprettTask(it))
            } catch (e: DbActionExecutionException) {
                if (e.cause is DuplicateKeyException) {
                    logger.info(
                        "Oppgave finnes allerede for barn fylt ${it.alder} " +
                            "på behandling ${it.behandlingId}. Oppretter ikke task."
                    )
                } else {
                    throw e
                }
            }
        }
    }

    private fun fødselsdato(barnTilUtplukkForOppgave: BarnTilUtplukkForOppgave): LocalDate? {
        return barnTilUtplukkForOppgave.fødselsnummerBarn?.let {
            Fødselsnummer(it).fødselsdato
        }
    }

    private data class FødselsnummerOgAlder(val fødselsnummer: String, val alder: Alder)
}
