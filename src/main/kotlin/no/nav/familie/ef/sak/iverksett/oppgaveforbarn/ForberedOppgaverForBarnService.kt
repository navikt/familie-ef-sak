package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForBarn
import no.nav.familie.kontrakter.ef.iverksett.OppgaverForBarnDto
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class ForberedOppgaverForBarnService(
    private val gjeldendeBarnRepository: GjeldendeBarnRepository,
    private val behandlingRepository: BehandlingRepository,
    private val iverksettClient: IverksettClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun forberedOppgaverForAlleBarnSomFyllerAarNesteUke(referansedato: LocalDate, dryRun: Boolean = false) {
        val gjeldendeBarn =
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, referansedato) +
                gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(StønadType.OVERGANGSSTØNAD, referansedato)
        logger.info(
            "Fant totalt ${gjeldendeBarn.size} barn, " +
                "av hvilke ${gjeldendeBarn.count { it.fraMigrering }} er fra migrerte behandlinger"
        )
        val barnSomFyllerAar = barnSomFyllerAar(gjeldendeBarn, referansedato)
        if (barnSomFyllerAar.isEmpty()) {
            return
        }
        val oppgaver = lagOppgaverForBarn(barnSomFyllerAar)
        logger.info("Fant ${oppgaver.size} oppgaver som skal opprettes ved forbereding av oppgaver for barn som fyller år")
        if (oppgaver.isNotEmpty()) {
            if (dryRun) {
                oppgaver.forEach { logger.info("Dryrun - oppretter oppgave for ${it.behandlingId}") }
            } else {
                sendOppgaverTilIverksett(oppgaver)
            }
        }
    }

    private fun lagOppgaverForBarn(barnSomFyllerAar: Map<UUID, Pair<BarnTilUtplukkForOppgave, String>>): List<OppgaveForBarn> {
        return behandlingRepository.finnEksterneIder(barnSomFyllerAar.map { it.key }.toSet()).map {
            val utplukketBarn = barnSomFyllerAar[it.behandlingId]
                ?: error("Kunne ikke finne behandlingsId fra utplukk. Dette skal ikke skje.")
            val beskrivelse = utplukketBarn.second
            OppgaveForBarn(
                it.behandlingId,
                it.eksternFagsakId,
                utplukketBarn.first.fødselsnummerSøker,
                StønadType.OVERGANGSSTØNAD,
                beskrivelse
            )
        }
    }

    private fun barnSomFyllerAar(
        barnTilUtplukkForOppgave: List<BarnTilUtplukkForOppgave>,
        referansedato: LocalDate
    ): Map<UUID, Pair<BarnTilUtplukkForOppgave, String>> {
        val barnSomFyllerAar = mutableMapOf<UUID, Pair<BarnTilUtplukkForOppgave, String>>()
        barnTilUtplukkForOppgave.forEach { barn ->
            val fødselsdato = fødselsdato(barn)
            if (barnBlirEttÅr(fødselsdato, referansedato)) {
                barnSomFyllerAar[barn.behandlingId] = Pair(barn, OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr())
            } else if (barnBlirSeksMnd(fødselsdato, referansedato)) {
                barnSomFyllerAar[barn.behandlingId] = Pair(barn, OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd())
            }
        }
        return barnSomFyllerAar
    }

    private fun sendOppgaverTilIverksett(oppgaver: List<OppgaveForBarn>) {
        iverksettClient.sendOppgaverForBarn(OppgaverForBarnDto(oppgaver))
    }

    private fun fødselsdato(barnTilUtplukkForOppgave: BarnTilUtplukkForOppgave): LocalDate {
        return barnTilUtplukkForOppgave.fødselsnummerBarn?.let {
            Fødselsnummer(it).fødselsdato
        } ?: barnTilUtplukkForOppgave.termindatoBarn ?: error("Ingen datoer for barn funnet")
    }

    private fun barnBlirEttÅr(
        fødselsdato: LocalDate,
        referansedato: LocalDate
    ): Boolean {
        return fødselsdato.plusYears(1) in referansedato..referansedato.plusDays(6)
    }

    private fun barnBlirSeksMnd(
        fødselsdato: LocalDate,
        referansedato: LocalDate
    ): Boolean {
        return fødselsdato.plusDays(182) in referansedato..referansedato.plusDays(6)
    }
}
