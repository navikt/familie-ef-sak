package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import no.nav.familie.kontrakter.felles.Behandlingstema
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
import java.util.Locale

@Service
class BarnFyllerÅrOppfølgingsoppgaveService(
    private val gjeldendeBarnRepository: GjeldendeBarnRepository,
    private val oppgaveClient: OppgaveClient,
    private val oppgaveService: OppgaveService,
    private val oppgaveRepository: OppgaveRepository,
    private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun opprettOppgaverForAlleBarnSomHarFyltÅr() {
        val dagensDato = LocalDate.now()
        val alleBarnIGjeldendeBehandlinger =
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, dagensDato) +
                gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(StønadType.OVERGANGSSTØNAD, dagensDato)

        logger.info("Antall barn i gjeldende behandlinger: ${alleBarnIGjeldendeBehandlinger.size}")
        logger.info("Antall barn i gjeldende behandlinger uten fnr:" +
                    alleBarnIGjeldendeBehandlinger.count { it.fødselsnummerBarn == null })

        val skalOpprettes = filtrerBarnSomHarFyltÅr(alleBarnIGjeldendeBehandlinger)
        opprettOppgaveForBarn(skalOpprettes)
    }

    private fun filtrerBarnSomHarFyltÅr(barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>): List<OpprettOppgaveForBarn> {
        val opprettedeOppgaver = listOf<OpprettetOppfølgingsoppgave>()
        val skalOpprettes = mutableListOf<OpprettOppgaveForBarn>()

        barnTilUtplukkForOppgave.forEach { barn ->
            val barnetsAlder = Alder.fromFødselsdato(fødselsdato(barn))
            if (barnetsAlder != null && barn.fødselsnummerBarn != null && opprettedeOppgaver.none { it.barnPersonIdent == barn.fødselsnummerBarn && it.alder == barnetsAlder }) {
                skalOpprettes.add(OpprettOppgaveForBarn(barn.fødselsnummerBarn, barn.fødselsnummerSøker, barnetsAlder))
            }
        }

        logger.info("barn til utplukk for oppgave: ${skalOpprettes.size}")
        return skalOpprettes
    }

    private fun opprettOppgaveForBarn(opprettOppgaverForBarn: List<OpprettOppgaveForBarn>) {

        val eksternIds = gjeldendeBarnRepository.finnEksternIderForBarn(opprettOppgaverForBarn.mapNotNull { it.fødselsnummer }.toSet())
        eksternIds.forEach { barnEksternId ->
            val opprettOppgaveForEksternId = opprettOppgaverForBarn.firstOrNull { it.fødselsnummer == barnEksternId.barnPersonIdent }
            val finnesOppgave = oppgaveRepository.findByBehandlingIdAndBarnPersonIdentAndAlder(barnEksternId.behandlingId, barnEksternId.barnPersonIdent, opprettOppgaveForEksternId?.alder) != null
            if (!finnesOppgave && opprettOppgaveForEksternId != null) {
                val opprettOppgaveRequest = lagOppgaveRequestForOppfølgingAvBarnFyltÅr(opprettOppgaveForEksternId, barnEksternId)
                val opprettetOppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
                oppgaveClient.leggOppgaveIMappe(opprettetOppgaveId)
                val oppgave = Oppgave(
                    gsakOppgaveId = opprettetOppgaveId,
                    behandlingId = barnEksternId.behandlingId,
                    barnPersonIdent = barnEksternId.barnPersonIdent,
                    type = Oppgavetype.InnhentDokumentasjon,
                    alder = opprettOppgaveForEksternId.alder
                )
                oppgaveRepository.insert(oppgave)
            }
        }
    }

    private fun lagOppgaveRequestForOppfølgingAvBarnFyltÅr(
        opprettOppgaveForEksternId: OpprettOppgaveForBarn,
        barnEksternId: BarnEksternIder
    ) =
        OpprettOppgaveRequest(
            ident = OppgaveIdentV2(
                ident = opprettOppgaveForEksternId.fødselsnummer,
                gruppe = IdentGruppe.FOLKEREGISTERIDENT
            ),
            saksId = barnEksternId.eksternFagsakId.toString(),
            tema = Tema.ENF,
            oppgavetype = Oppgavetype.InnhentDokumentasjon,
            fristFerdigstillelse = oppgaveService.lagFristForOppgave(LocalDateTime.now()),
            beskrivelse = opprettOppgaveForEksternId.alder.oppgavebeskrivelse,
            enhetsnummer = personopplysningerIntegrasjonerClient.hentNavEnhetForPersonMedRelasjoner(
                opprettOppgaveForEksternId.fødselsnummerSøker
            ).first().enhetId,
            behandlingstema = opprettBehandlingstema(StønadType.OVERGANGSSTØNAD).value,
            tilordnetRessurs = null,
            behandlesAvApplikasjon = "familie-ef-sak"
        )

    fun opprettBehandlingstema(stønadstype: StønadType): Behandlingstema {
        return Behandlingstema
            .fromValue(
                stønadstype.name.lowercase(Locale.getDefault())
                    .replaceFirstChar { it.uppercase() }
            )
    }

    private fun fødselsdato(barnTilUtplukkForOppgave: BarnTilUtplukkForOppgave): LocalDate? {
        return barnTilUtplukkForOppgave.fødselsnummerBarn?.let {
            Fødselsnummer(it).fødselsdato
        }
    }
}
