package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveClient
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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class BarnFyllerÅrOppfølgingsoppgaveService(
    private val gjeldendeBarnRepository: GjeldendeBarnRepository,
    private val oppgaveClient: OppgaveClient,
    private val oppgaveService: OppgaveService,
    private val oppgaveRepository: OppgaveRepository,
    private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun opprettOppgaverForAlleBarnSomHarFyltÅr(dryRun: Boolean = false) {
        val dagensDato = LocalDate.now()
        val alleBarnIGjeldendeBehandlinger =
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, dagensDato) +
                gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(StønadType.OVERGANGSSTØNAD, dagensDato)

        logger.info("Antall barn i gjeldende behandlinger: ${alleBarnIGjeldendeBehandlinger.size}")

        val skalOpprettes = filtrerBarnSomHarFyltÅr(alleBarnIGjeldendeBehandlinger)

        if (!dryRun) {
            opprettOppgaveForBarn(skalOpprettes)
        } else {
            logger.info("Ville opprettet oppgave for ${skalOpprettes.size} barn.")
            skalOpprettes.forEach {
                secureLogger.info(
                    "Ville opprettet oppgave for barn med fødselsnummer: " +
                        "${it.fødselsnummer} med alder ${it.alder}"
                )
            }
        }
    }

    private fun filtrerBarnSomHarFyltÅr(barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>): List<OpprettOppgaveForBarn> {
        val opprettedeOppgaver = oppgaveRepository.findByTypeAndAlderIsNotNull(Oppgavetype.InnhentDokumentasjon)
        val skalOpprettes = mutableListOf<OpprettOppgaveForBarn>()

        barnTilUtplukkForOppgave.forEach { barn ->
            val barnetsAlder = Alder.fromFødselsdato(fødselsdato(barn))
            if (barnetsAlder != null && barn.fødselsnummerBarn != null && opprettedeOppgaver.none { it.barnPersonIdent == barn.fødselsnummerBarn && it.alder == barnetsAlder }) {
                skalOpprettes.add(
                    OpprettOppgaveForBarn(
                        barn.fødselsnummerBarn,
                        barn.fødselsnummerSøker,
                        barnetsAlder,
                        barn.behandlingId
                    )
                )
            }
        }

        logger.info("barn til utplukk for oppgave: ${skalOpprettes.size}")
        return skalOpprettes
    }

    private fun opprettOppgaveForBarn(opprettOppgaverForBarn: List<OpprettOppgaveForBarn>) {
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
                val opprettetOppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
                oppgaveClient.leggOppgaveIMappe(opprettetOppgaveId)
                val oppgave = Oppgave(
                    gsakOppgaveId = opprettetOppgaveId,
                    behandlingId = gjeldendeBarn.behandlingId,
                    barnPersonIdent = gjeldendeBarn.barnPersonIdent,
                    type = Oppgavetype.InnhentDokumentasjon,
                    alder = opprettOppgaveForEksternId.alder
                )
                oppgaveRepository.insert(oppgave)
            }
        }
    }

    private fun lagOppgaveRequestForOppfølgingAvBarnFyltÅr(
        opprettOppgaveForEksternId: OpprettOppgaveForBarn,
        barnTilOppgave: BarnTilOppgave
    ) =
        OpprettOppgaveRequest(
            ident = OppgaveIdentV2(
                ident = opprettOppgaveForEksternId.fødselsnummerSøker,
                gruppe = IdentGruppe.FOLKEREGISTERIDENT
            ),
            saksId = barnTilOppgave.eksternFagsakId.toString(),
            tema = Tema.ENF,
            oppgavetype = Oppgavetype.InnhentDokumentasjon,
            fristFerdigstillelse = oppgaveService.lagFristForOppgave(LocalDateTime.now()),
            beskrivelse = opprettOppgaveForEksternId.alder.oppgavebeskrivelse,
            enhetsnummer = personopplysningerIntegrasjonerClient.hentNavEnhetForPersonMedRelasjoner(
                opprettOppgaveForEksternId.fødselsnummerSøker
            ).first().enhetId,
            behandlingstema = Behandlingstema.Overgangsstønad.value,
            tilordnetRessurs = null,
            behandlesAvApplikasjon = "familie-ef-sak"
        )

    private fun fødselsdato(barnTilUtplukkForOppgave: BarnTilUtplukkForOppgave): LocalDate? {
        return barnTilUtplukkForOppgave.fødselsnummerBarn?.let {
            Fødselsnummer(it).fødselsdato
        }
    }
}
