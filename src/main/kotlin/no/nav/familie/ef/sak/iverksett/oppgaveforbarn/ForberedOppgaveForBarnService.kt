package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class ForberedOppgaveForBarnService(private val behandlingRepository: BehandlingRepository,
                                    private val iverksettClient: IverksettClient) {

    fun forberedOppgaverForAlleBarnSomFyllerAar(innenAntallUker: Long, sisteKjøring: LocalDate) {
        val referanseDato = referanseDato(innenAntallUker, sisteKjøring)
        val gjeldendeBarn =
                behandlingRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, referanseDato)
        val oppgaver = mutableListOf<OppgaveForBarn>()
        gjeldendeBarn.forEach { barn ->
            val fødselsdato = fødselsdato(barn)
            if (barnBlirEttÅr(innenAntallUker, referanseDato, fødselsdato)) {
                oppgaver.add(oppgaveForBarn(barn, OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr()))
            } else if (barnBlirSeksMnd(innenAntallUker, referanseDato, fødselsdato)) {
                oppgaver.add(oppgaveForBarn(barn, OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd()))
            }
        }
        if (oppgaver.isNotEmpty()) {
            sendOppgaverTilIverksett(oppgaver)
        }
    }

    private fun sendOppgaverTilIverksett(oppgaver: List<OppgaveForBarn>) {
        iverksettClient.sendOppgaverForBarn(OppgaverForBarnDto(oppgaver))
    }

    private fun oppgaveForBarn(gjeldendeBarn: GjeldendeBarn, beskrivelse: String): OppgaveForBarn {
        return OppgaveForBarn(gjeldendeBarn.id,
                              beskrivelse,
                              gjeldendeBarn.fødselsnummerSøker,
                              gjeldendeBarn.fodselsnummerBarn,
                              gjeldendeBarn.termindatoBarn)
    }

    private fun fødselsdato(gjeldendeBarn: GjeldendeBarn): LocalDate {
        return gjeldendeBarn.fodselsnummerBarn?.let {
            Fødselsnummer(it).fødselsdato
        } ?: gjeldendeBarn.termindatoBarn ?: error("Ingen datoer for barn funnet")
    }

    private fun barnBlirEttÅr(innenAntallUker: Long, referanseDato: LocalDate, fødselsdato: LocalDate): Boolean {
        return barnErUnder(12L, referanseDato, fødselsdato)
               && LocalDate.now().plusWeeks(innenAntallUker) >= fødselsdato.plusYears(1)
    }

    private fun barnBlirSeksMnd(innenAntallUker: Long, referanseDato: LocalDate, fødselsdato: LocalDate): Boolean {
        return barnErUnder(6L, referanseDato, fødselsdato)
               && LocalDate.now().plusWeeks(innenAntallUker) >= fødselsdato.plusMonths(6L)
    }

    private fun barnErUnder(antallMnd: Long, referanseDato: LocalDate, fødselsdato: LocalDate): Boolean {
        return referanseDato <= fødselsdato.plusMonths(antallMnd)
    }

    private fun referanseDato(innenAntallUker: Long, sisteKjøring: LocalDate): LocalDate {
        val periodeGap = ChronoUnit.DAYS.between(sisteKjøring, LocalDate.now()) - innenAntallUker * 7
        if (periodeGap > 0) {
            return LocalDate.now().minusDays(periodeGap)
        }
        return LocalDate.now()
    }

}