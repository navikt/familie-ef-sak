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
class ForberedOppgaveForBarnService(private val gjeldendeBarnRepository: GjeldendeBarnRepository,
                                    private val behandlingRepository: BehandlingRepository,
                                    private val iverksettClient: IverksettClient) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun forberedOppgaverForAlleBarnSomFyllerAarNesteUke(sisteKjøring: LocalDate) {
        val referanseDato = referanseDato(sisteKjøring)
        val gjeldendeBarn =
                gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, referanseDato)
        val barnSomFyllerAar = barnSomFyllerAar(gjeldendeBarn, referanseDato)
        val oppgaver = lagOppgaverForBarn(barnSomFyllerAar)
        if (oppgaver.isNotEmpty()) {
            logger.info("Fant ${oppgaver.size} oppgaver som skal opprettes ved forbereding av oppgaver for barn som fyller år")
            sendOppgaverTilIverksett(oppgaver)
        }
    }

    private fun lagOppgaverForBarn(barnSomFyllerAar: Map<UUID, Pair<GjeldendeBarn, String>>): List<OppgaveForBarn> {
        return behandlingRepository.finnEksterneIder(barnSomFyllerAar.map { it.key }.toSet()).map {
            OppgaveForBarn(it.behandlingId,
                           it.eksternFagsakId,
                           barnSomFyllerAar[it.behandlingId]!!.first.fødselsnummerSøker
                           ?: error("Fant ikke noe fødselsnummer for søker"),
                           Stønadstype.OVERGANGSSTØNAD.name,
                           barnSomFyllerAar[it.behandlingId]!!.second)
        }
    }

    private fun barnSomFyllerAar(gjeldendeBarn: List<GjeldendeBarn>,
                                 referanseDato: LocalDate): Map<UUID, Pair<GjeldendeBarn, String>> {
        val barnSomFyllerAar = mutableMapOf<UUID, Pair<GjeldendeBarn, String>>()
        gjeldendeBarn.forEach { barn ->
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

    private fun fødselsdato(gjeldendeBarn: GjeldendeBarn): LocalDate {
        return gjeldendeBarn.fodselsnummerBarn?.let {
            Fødselsnummer(it).fødselsdato
        } ?: gjeldendeBarn.termindatoBarn ?: error("Ingen datoer for barn funnet")
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