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

        val skalOpprettes = finnBarnIAktuellAlderUtenOppgave(alleBarnIGjeldendeBehandlinger)
        logger.info("Oppretter oppgave for ${skalOpprettes.size} barn. (dry-run: $dryRun)")

        if (!dryRun) {
            opprettOppgaveTasksForBarn(skalOpprettes)
        } else {
            skalOpprettes.forEach {
                secureLogger.info(
                    "Ville opprettet oppgave for barn med fødselsnummer: " +
                        "${it.fødselsnummer} med alder ${it.aktivitetspliktigAlder}",
                )
            }
        }
        logger.info("Oppretting av oppfølgingsoppgave-tasks ferdig")
    }

    // map grunnlagsdata barn til BehandlingMedBarnIAktivitetspliktigAlder
    // map termindato-barn til BehandlingMedBarnIAktivitetspliktigAlder (hent data fra pdl)
    private fun finnBarnIAktuellAlder(barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>): Set<BehandlingMedBarnIAktivitetspliktigAlder> {
        val barnFraGrunnlagsdataIAktuellAlder = mapTilBehandlingMedBarnIAktivitetspliktigAlderAvGrunnlagsdatabarn(barnTilUtplukkForOppgave)
        val terminbarnIAktuellAlder = finnTerminbarn(barnTilUtplukkForOppgave).hentPDLDataOgmapTilBehandlingMedBarnIAktivitetspliktigAlder()
        return barnFraGrunnlagsdataIAktuellAlder + terminbarnIAktuellAlder
    }

    private fun List<BarnTilUtplukkForOppgave>.hentPDLDataOgmapTilBehandlingMedBarnIAktivitetspliktigAlder(): Set<BehandlingMedBarnIAktivitetspliktigAlder> {
        val forelderBarn: Map<ForelderIdentDto, List<BarnMedFødselsdatoDto>> = hentForelderMedBarnFor(this)

        return this
            .mapNotNull { barn ->
                val alleBarnaTilForelder = forelderBarn[ForelderIdentDto(barn.fødselsnummerSøker)] ?: emptyList()
                val besteMatch = finnBesteMatchPåFødselsnummerForTermindato(alleBarnaTilForelder, barn.termindatoBarn ?: error("Termindato er null"))

                besteMatch?.let {
                    val alderForOppfølgingsoppgave = AktivitetspliktigAlder.fromFødselsdato(besteMatch.fødselsdato)
                    alderForOppfølgingsoppgave?.let {
                        BehandlingMedBarnIAktivitetspliktigAlder(
                            fødselsnummer = besteMatch.barnIdent,
                            fødselsnummerSøker = barn.fødselsnummerSøker,
                            aktivitetspliktigAlder = alderForOppfølgingsoppgave,
                            behandlingId = barn.behandlingId,
                        )
                    }
                }
            }.toSet()
    }

    private fun mapTilBehandlingMedBarnIAktivitetspliktigAlderAvGrunnlagsdatabarn(barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>) =
        barnTilUtplukkForOppgave
            .filter { it.fødselsnummerBarn != null }
            .mapNotNull { barn ->
                val alder = AktivitetspliktigAlder.fromFødselsdato(hentFødselsdatoFraGrunnlagsdata(barn))
                if (alder != null) {
                    BehandlingMedBarnIAktivitetspliktigAlder(
                        fødselsnummer = barn.fødselsnummerBarn ?: error("Fødselsnummer skal være satt her pga filter ovenfor"),
                        fødselsnummerSøker = barn.fødselsnummerSøker,
                        aktivitetspliktigAlder = alder,
                        behandlingId = barn.behandlingId,
                    )
                } else {
                    null
                }
            }
            .distinctBy { it.behandlingId }
            .toSet()

    private fun finnTerminbarn(barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>) = barnTilUtplukkForOppgave.filter { it.termindatoBarn != null && it.fødselsnummerBarn == null }

    private fun hentFødselsdatoFraGrunnlagsdata(barn: BarnTilUtplukkForOppgave): LocalDate? {
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(barn.behandlingId)
        val barnAvGrunnlagsdata = grunnlagsdata.grunnlagsdata.barn.filter { it.personIdent == barn.fødselsnummerBarn }
        return barnAvGrunnlagsdata
            .first()
            .fødsel
            .first()
            .fødselsdato
    }

    private fun hentForelderMedBarnFor(barnMedTermindato: List<BarnTilUtplukkForOppgave>): Map<ForelderIdentDto, List<BarnMedFødselsdatoDto>> {
        val forelderIdentMedBarn: List<ForelderMedBarnRelasjon> = hentForeldreMedBarn(barnMedTermindato)
        logger.info("Antall foreldre vi prøver å matche: ${forelderIdentMedBarn.size}")
        val barnMedFødselsdato: Map<String, LocalDate?> = hentBarnFødselsdatoer(forelderIdentMedBarn)
        return forelderIdentMedBarn.map { forelder -> ForelderIdentDto(forelder.forelderIdent) to forelder.barnIdenter.map { barneIdent -> BarnMedFødselsdatoDto(barneIdent, barnMedFødselsdato[barneIdent]) } }.toMap()
    }

    private fun hentForeldreMedBarn(barnMedTermindato: List<BarnTilUtplukkForOppgave>): List<ForelderMedBarnRelasjon> =
        personService
            .hentPersonForelderBarnRelasjon(barnMedTermindato.map { it.fødselsnummerSøker })
            .map { pdlPerson ->
                ForelderMedBarnRelasjon(
                    forelderIdent = pdlPerson.key,
                    barnIdenter =
                        pdlPerson.value.forelderBarnRelasjon
                            .filter { relasjon -> relasjon.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                            .mapNotNull { relasjon -> relasjon.relatertPersonsIdent },
                )
            }

    private fun hentBarnFødselsdatoer(
        forelderMedBarnRelasjoner: List<ForelderMedBarnRelasjon>,
    ): Map<String, LocalDate?> {
        // TODO ny person_fooedsel.graphql query?
        val barneIdentListe = forelderMedBarnRelasjoner.flatMap { relasjon -> relasjon.barnIdenter }
        return personService
            .hentPersonForelderBarnRelasjon(barneIdentListe)
            .map {
                it.key to
                    it.value.fødselsdato
                        .first()
                        .fødselsdato
            }.toMap()
    }

    private fun finnBarnIAktuellAlderUtenOppgave(barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>): Set<BehandlingMedBarnIAktivitetspliktigAlder> {
        val barnIAktuellAlder = finnBarnIAktuellAlder(barnTilUtplukkForOppgave)
        val opprettedeOppgaver = finnOpprettedeOppgaver(barnIAktuellAlder)
        return barnIAktuellAlder
            .filterNot {
                oppgaveOpprettetTidligere(opprettedeOppgaver, it)
            }.toSet()
    }

    private fun oppgaveOpprettetTidligere(
        opprettedeOppgaver: Set<FødselsnummerOgAlder>,
        it: BehandlingMedBarnIAktivitetspliktigAlder,
    ) = opprettedeOppgaver.contains(FødselsnummerOgAlder(it.fødselsnummer, it.aktivitetspliktigAlder))

    private fun finnOpprettedeOppgaver(oppgaverForBarn: Set<BehandlingMedBarnIAktivitetspliktigAlder>): Set<FødselsnummerOgAlder> {
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

    private fun opprettOppgaveTasksForBarn(oppgaver: Set<BehandlingMedBarnIAktivitetspliktigAlder>) {
        if (oppgaver.isEmpty()) return

        oppgaver
            .map {
                OpprettOppgavePayload(
                    it.behandlingId,
                    it.fødselsnummer,
                    it.fødselsnummerSøker,
                    it.aktivitetspliktigAlder,
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
        val alder: AktivitetspliktigAlder,
    )
}

data class ForelderIdentDto(
    val ident: String,
)

data class BarnMedFødselsdatoDto(
    val barnIdent: String,
    val fødselsdato: LocalDate?,
)

// TODO: Rename til noe litt mer særeget
data class ForelderMedBarnRelasjon(
    val forelderIdent: String,
    val barnIdenter: List<String>,
)
