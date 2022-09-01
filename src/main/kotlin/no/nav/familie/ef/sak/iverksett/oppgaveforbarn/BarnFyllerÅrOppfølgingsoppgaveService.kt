package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class BarnFyllerÅrOppfølgingsoppgaveService(
    private val gjeldendeBarnRepository: GjeldendeBarnRepository,
    private val oppgaveService: OppgaveService,
    private val oppgaveRepository: OppgaveRepository,
    private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient,
    private val taskRepository: TaskRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun opprettTasksForAlleBarnSomHarFyltÅr(dryRun: Boolean = false) {
        val dagensDato = LocalDate.now()
        val alleBarnIGjeldendeBehandlinger =
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, dagensDato) +
                gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(StønadType.OVERGANGSSTØNAD, dagensDato)

        logger.info("Antall barn i gjeldende behandlinger: ${alleBarnIGjeldendeBehandlinger.size}")

        val skalOpprettes = filtrerBarnSomHarFyltÅr(alleBarnIGjeldendeBehandlinger)
        logger.info("Ville opprettet oppgave for ${skalOpprettes.size} barn.")

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

    private fun filtrerBarnSomHarFyltÅr(barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>): List<OpprettOppgaveForBarn> {
        val barnPersonIdenter = barnTilUtplukkForOppgave.mapNotNull { it.fødselsnummerBarn }
        if (barnPersonIdenter.isEmpty()) return listOf()
        val opprettedeOppgaver = oppgaveRepository.findByTypeAndAlderIsNotNullAndBarnPersonIdenter(Oppgavetype.InnhentDokumentasjon, barnPersonIdenter)

        return barnTilUtplukkForOppgave.mapNotNull { barn ->
            val barnetsAlder = Alder.fromFødselsdato(fødselsdato(barn))
            if (barnetsAlder != null && barn.fødselsnummerBarn != null && opprettedeOppgaver.none { it.barnPersonIdent == barn.fødselsnummerBarn && it.alder == barnetsAlder }) {
                OpprettOppgaveForBarn(
                    barn.fødselsnummerBarn,
                    barn.fødselsnummerSøker,
                    barnetsAlder,
                    barn.behandlingId
                )
            } else {
                null
            }
        }
    }

    private fun opprettOppgaveTasksForBarn(opprettOppgaverForBarn: List<OpprettOppgaveForBarn>) {
        if (opprettOppgaverForBarn.isEmpty()) return
        val gjeldendeBarnList =
            gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(opprettOppgaverForBarn.map { it.behandlingId }).toSet()

        gjeldendeBarnList.forEach { gjeldendeBarn ->
            val opprettOppgaveForEksternId =
                opprettOppgaverForBarn.firstOrNull { it.fødselsnummer == gjeldendeBarn.barnPersonIdent }

            val finnesOppgave = oppgaveRepository.findByBehandlingIdAndBarnPersonIdentAndAlder(
                gjeldendeBarn.behandlingId,
                gjeldendeBarn.barnPersonIdent,
                opprettOppgaveForEksternId?.alder
            ) != null

            if (!finnesOppgave && opprettOppgaveForEksternId != null) {
                val opprettOppgaveRequest = lagOppgaveRequestForOppfølgingAvBarnFyltÅr(opprettOppgaveForEksternId, gjeldendeBarn)
                try {
                    taskRepository.save(
                        OpprettOppfølgingsoppgaveForBarnFyltÅrTask.opprettTask(
                            OpprettOppgavePayload(
                                gjeldendeBarn.behandlingId,
                                gjeldendeBarn.barnPersonIdent,
                                opprettOppgaveForEksternId.alder,
                                opprettOppgaveRequest
                            )
                        )
                    )
                } catch (e: DbActionExecutionException) {
                    if (e.cause is DuplicateKeyException) {
                        logger.info("Oppgave finnes allerede for barn fylt ${opprettOppgaveForEksternId.alder} " +
                                    "på behandling ${gjeldendeBarn.behandlingId}. Oppretter ikke task.")
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    private fun lagOppgaveRequestForOppfølgingAvBarnFyltÅr(
        opprettOppgaveForEksternId: OpprettOppgaveForBarn,
        barnTilOppgave: BarnTilOppgave
    ): OpprettOppgaveRequest {
        val enhetsnummer = personopplysningerIntegrasjonerClient.hentNavEnhetForPersonMedRelasjoner(
            opprettOppgaveForEksternId.fødselsnummerSøker
        ).first().enhetId
        return OpprettOppgaveRequest(
            ident = OppgaveIdentV2(
                ident = opprettOppgaveForEksternId.fødselsnummerSøker,
                gruppe = IdentGruppe.FOLKEREGISTERIDENT
            ),
            saksId = barnTilOppgave.eksternFagsakId.toString(),
            tema = Tema.ENF,
            oppgavetype = Oppgavetype.InnhentDokumentasjon,
            fristFerdigstillelse = oppgaveService.lagFristForOppgave(LocalDateTime.now()),
            beskrivelse = opprettOppgaveForEksternId.alder.oppgavebeskrivelse,
            enhetsnummer = enhetsnummer,
            behandlingstema = Behandlingstema.Overgangsstønad.value,
            tilordnetRessurs = null,
            behandlesAvApplikasjon = "familie-ef-sak",
            mappeId = oppgaveService.finnHendelseMappeId(enhetsnummer)
        )
    }

    private fun fødselsdato(barnTilUtplukkForOppgave: BarnTilUtplukkForOppgave): LocalDate? {
        return barnTilUtplukkForOppgave.fødselsnummerBarn?.let {
            Fødselsnummer(it).fødselsdato
        }
    }
}
