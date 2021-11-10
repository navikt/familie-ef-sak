package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class EksternBehandlingService(val tilkjentYtelseService: TilkjentYtelseService, val behandlingRepository: BehandlingRepository) {


    fun finnesBehandlingFor(personidenter: Set<String>, stønadstype: Stønadstype?): Boolean {
        return if (stønadstype != null) {
            return eksistererBehandlingSomIkkeErBlankett(stønadstype, personidenter)
        } else {
            Stønadstype.values().any { eksistererBehandlingSomIkkeErBlankett(it, personidenter) }
        }
    }

    fun erBehandlingerUtdaterteFor(personidenter: Set<String>): Boolean {
        val behandlingIDer = hentAlleBehandlingIDer(personidenter)
        val tilkjenteYtelser = mutableSetOf<TilkjentYtelse>()
        behandlingIDer.forEach { tilkjenteYtelser.add(tilkjentYtelseService.hentForBehandling(it)) }
        val senesteTomDatoAvAndeler: LocalDate = tilkjenteYtelser.maxOf { it.andelerTilkjentYtelse.maxOf { it.stønadTom } }
        return senesteTomDatoAvAndeler < LocalDate.now().minusYears(1)
    }

    /**
     * Hvis siste behandling er teknisk opphør, skal vi returnere false,
     * hvis ikke så skal vi returnere true hvis det finnes en behandling
     */
    private fun eksistererBehandlingSomIkkeErBlankett(stønadstype: Stønadstype, personidenter: Set<String>): Boolean {
        return behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(stønadstype, personidenter)?.let {
            it.type != BehandlingType.TEKNISK_OPPHØR
        } ?: false
    }

    private fun hentAlleBehandlingIDer(personidenter: Set<String>): Set<UUID> {
        val behandlinger = mutableSetOf<UUID>()
        Stønadstype.values().forEach {
            behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(it, personidenter)?.let { behandlinger.add(it.id) }
        }
        return behandlinger
    }

}