package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ForberedOppgaverForBarnService(private val gjeldendeBarnRepository: GjeldendeBarnRepository,
                                     private val behandlingRepository: BehandlingRepository,
                                     private val iverksettClient: IverksettClient) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun forberedOppgaverForAlleBarnSomFyllerAarNesteUke(sisteKjøring: LocalDate) {
        val referanseDato = referanseDato(sisteKjøring)
        val gjeldendeBarn =
                gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, referanseDato)
        val barnSomFyllerAar = barnSomFyllerAar(gjeldendeBarn, referanseDato)
        if (barnSomFyllerAar.isEmpty()) {
            return
        }
        val oppgaver = lagOppgaverForBarn(barnSomFyllerAar)
        if (oppgaver.isNotEmpty()) {
            logger.info("Fant ${oppgaver.size} oppgaver som skal opprettes ved forbereding av oppgaver for barn som fyller år")
            sendOppgaverTilIverksett(oppgaver)
        }
    }

    private fun lagOppgaverForBarn(barnSomFyllerAar: Map<UUID, Pair<BarnTilUtplukkForOppgave, String>>): List<OppgaveForBarn> {
        return behandlingRepository.finnEksterneIder(barnSomFyllerAar.map { it.key }.toSet()).map {
            val utplukketBarn = barnSomFyllerAar[it.behandlingId]
                                ?: error("Kunne ikke finne behandlingsId fra utplukk. Dette skal ikke skje.")
            val beskrivelse = utplukketBarn.second
            OppgaveForBarn(it.behandlingId,
                           it.eksternFagsakId,
                           utplukketBarn.first.fødselsnummerSøker!!,
                           Stønadstype.OVERGANGSSTØNAD.name,
                           beskrivelse)
        }
    }

    private fun barnSomFyllerAar(barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>,
                                 referanseDato: LocalDate): Map<UUID, Pair<BarnTilUtplukkForOppgave, String>> {
        val barnSomFyllerAar = mutableMapOf<UUID, Pair<BarnTilUtplukkForOppgave, String>>()
        barnTilUtplukkForOppgave.forEach { barn ->
            val fødselsdato = fødselsdato(barn)
            if (barnBlirEttÅr(referanseDato, fødselsdato)) {
                barnSomFyllerAar[barn.behandlingId] = Pair(barn, OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr())
            } else if (barnBlirSeksMnd(referanseDato, fødselsdato)) {
                barnSomFyllerAar[barn.behandlingId] = Pair(barn, OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd())
            }
        }
        return barnSomFyllerAar
    }

    private fun sendOppgaverTilIverksett(oppgaver: List<OppgaveForBarn>) {
        iverksettClient.sendOppgaverForBarn(OppgaverForBarnDto(oppgaver))
    }

    private fun fødselsdato(barnTilUtplukkForOppgave: BarnTilUtplukkForOppgave): LocalDate {
        return barnTilUtplukkForOppgave.fodselsnummerBarn?.let {
            Fødselsnummer(it).fødselsdato
        } ?: barnTilUtplukkForOppgave.termindatoBarn ?: error("Ingen datoer for barn funnet")
    }

    private fun barnBlirEttÅr(referanseDato: LocalDate, fødselsdato: LocalDate): Boolean {
        return barnErUnder(12L, referanseDato, fødselsdato)
               && LocalDate.now().plusWeeks(1) >= fødselsdato.plusYears(1)
    }

    private fun barnBlirSeksMnd(referanseDato: LocalDate, fødselsdato: LocalDate): Boolean {
        return barnErUnder(6L, referanseDato, fødselsdato)
               && LocalDate.now().plusWeeks(1) >= fødselsdato.plusMonths(6L)
    }

    private fun barnErUnder(antallMnd: Long, referanseDato: LocalDate, fødselsdato: LocalDate): Boolean {
        return referanseDato <= fødselsdato.plusMonths(antallMnd)
    }

    private fun referanseDato(sisteKjøring: LocalDate): LocalDate {
        val periodeGap = ChronoUnit.DAYS.between(sisteKjøring, LocalDate.now()) - 7
        if (periodeGap > 0) {
            return LocalDate.now().minusDays(periodeGap)
        }
        return LocalDate.now()
    }

}