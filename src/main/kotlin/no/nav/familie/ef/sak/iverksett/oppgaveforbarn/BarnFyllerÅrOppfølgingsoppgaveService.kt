package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.opplysninger.mapper.finnBesteMatchPåFødselsnummerForTermindato
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
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
    private val taskService: TaskService,
    private val personService: PersonService,
    private val grunnlagsdataService: GrunnlagsdataService,
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
                        "${it.fødselsnummer} med alder ${it.alder}",
                )
            }
        }
        logger.info("Oppretting av oppfølgingsoppgave-tasks ferdig")
    }

    private fun hentFødselsnummerTilTermindatoBarn(barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>): List<BarnTilUtplukkForOppgave> {
        val barnUtenFødselsnummer =
            barnTilUtplukkForOppgave.filter { it.termindatoBarn != null && it.fødselsnummerBarn == null }
        val barnMedFødselsnummer = barnTilUtplukkForOppgave.filter { it.fødselsnummerBarn != null }
        val barnUtenIdentOppdatertMedIdentFraPdl = finnFødselsnummerTilTerminbarn(barnUtenFødselsnummer)
        return barnMedFødselsnummer + barnUtenIdentOppdatertMedIdentFraPdl
    }

    private fun hentFødselsdatoFraGrunnlagsdata(barn: BarnTilUtplukkForOppgave): LocalDate? {
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(barn.behandlingId)
        val barnAvGrunnlagsdata = grunnlagsdata.grunnlagsdata.barn.filter { it.personIdent == barn.fødselsnummerBarn }
        return barnAvGrunnlagsdata
            .first()
            .fødsel
            .first()
            .fødselsdato
    }

    private fun finnFødselsnummerTilTerminbarn(barnMedTermindato: List<BarnTilUtplukkForOppgave>): List<BarnTilUtplukkForOppgave> {
        val forelderBarn: Map<ForelderIdentDto, List<BarnMedFødselsdatoDto>> = hentForelderMedBarnFor(barnMedTermindato)

        return barnMedTermindato.map { barn ->
            val alleBarnaTilForelder = forelderBarn[ForelderIdentDto(barn.fødselsnummerSøker)] ?: emptyList()
            val besteMatch = finnBesteMatchPåFødselsnummerForTermindato(alleBarnaTilForelder, barn.termindatoBarn ?: error("Termindato er null"))
            barn.copy(fødselsnummerBarn = besteMatch)
        }
    }

    private fun hentForelderMedBarnFor(barnMedTermindato: List<BarnTilUtplukkForOppgave>): Map<ForelderIdentDto, List<BarnMedFødselsdatoDto>> {
        val forelderIdentMedBarn: Map<ForelderIdentDto, List<String>> = hentForeldreMedBarn(barnMedTermindato)
        logger.info("Antall foreldre vi prøver å matche: ${forelderIdentMedBarn.keys.size}")
        val alleBarn = forelderIdentMedBarn.values.flatten()
        val barnMedFødselsdato: Map<String, LocalDate?> = hentBarnFødselsdatoer(alleBarn)
        logger.info("Antall barn hentet ut med fødselsdatoer: ${alleBarn.size}")
        return forelderIdentMedBarn.map { it.key to it.value.map { barneIdent -> BarnMedFødselsdatoDto(barneIdent, barnMedFødselsdato[it.key.ident]) } }.toMap()
    }

    private fun hentForeldreMedBarn(barnMedTermindato: List<BarnTilUtplukkForOppgave>) =
        personService
            .hentPersonForelderBarnRelasjon(barnMedTermindato.map { it.fødselsnummerSøker })
            .map {
                ForelderIdentDto(it.key) to
                    it.value.forelderBarnRelasjon
                        .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                        .mapNotNull { it.relatertPersonsIdent }
            }.toMap()

    private fun hentBarnFødselsdatoer(
        barneIdentListe: List<String>,
    ): Map<String, LocalDate?> {
        // TODO ny person_fooedsel.graphql query?
        return personService
            .hentPersonForelderBarnRelasjon(barneIdentListe)
            .map {
                it.key to
                    it.value.fødselsdato
                        .first()
                        .fødselsdato
            }.toMap()
    }

    private fun lagOpprettOppgaveForBarn(barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>): Set<OpprettOppgaveForBarn> {
        val barn = hentFødselsnummerTilTermindatoBarn(barnTilUtplukkForOppgave)
        val oppgaverForBarn = opprettOppgaveForBarn(barn)
        val opprettedeOppgaver = finnOpprettedeOppgaver(oppgaverForBarn)

        return oppgaverForBarn
            .filterNot { opprettedeOppgaver.contains(FødselsnummerOgAlder(it.fødselsnummer, it.alder)) }
            .toSet()
    }

    private fun opprettOppgaveForBarn(barn: List<BarnTilUtplukkForOppgave>): List<OpprettOppgaveForBarn> =
        barn.mapNotNull {
            Alder.fromFødselsdato(hentFødselsdatoFraGrunnlagsdata(it))?.let { alder ->
                OpprettOppgaveForBarn(
                    it.fødselsnummerBarn!!,
                    it.fødselsnummerSøker,
                    alder,
                    it.behandlingId,
                )
            }
        }

    private fun finnOpprettedeOppgaver(oppgaverForBarn: List<OpprettOppgaveForBarn>): Set<FødselsnummerOgAlder> {
        if (oppgaverForBarn.isEmpty()) return emptySet()

        return oppgaveRepository
            .findByTypeAndAlderIsNotNullAndBarnPersonIdenter(
                Oppgavetype.InnhentDokumentasjon,
                oppgaverForBarn.map { it.fødselsnummer },
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

        oppgaver
            .map {
                OpprettOppgavePayload(
                    it.behandlingId,
                    it.fødselsnummer,
                    it.fødselsnummerSøker,
                    it.alder,
                )
            }.forEach {
                try {
                    taskService.save(OpprettOppfølgingsoppgaveForBarnFyltÅrTask.opprettTask(it))
                } catch (e: DbActionExecutionException) {
                    if (e.cause is DuplicateKeyException) {
                        logger.info(
                            "Oppgave finnes allerede for barn fylt ${it.alder} " +
                                "på behandling ${it.behandlingId}. Oppretter ikke task.",
                        )
                    } else {
                        throw e
                    }
                }
            }
    }

    private data class FødselsnummerOgAlder(
        val fødselsnummer: String,
        val alder: Alder,
    )
}

data class ForelderIdentDto(
    val ident: String,
)

data class BarnMedFødselsdatoDto(
    val barnIdent: String,
    val fødselsdato: LocalDate?,
)
