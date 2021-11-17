package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
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

    fun harStønadSiste12Måneder(personidenter: Set<String>): Boolean {
        val fagsakIDer = hentAlleFagsakIder(personidenter)
        val sisteStønadsdato = fagsakIDer
                                       .mapNotNull { behandlingRepository.finnSisteIverksatteBehandling(it)?.id }
                                       .map(tilkjentYtelseService::hentForBehandling)
                                       .mapNotNull { it.andelerTilkjentYtelse.maxOfOrNull(AndelTilkjentYtelse::stønadTom) }
                                       .maxOfOrNull { it } ?: LocalDate.MIN
        return sisteStønadsdato > LocalDate.now().minusYears(1)
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

    private fun hentAlleFagsakIder(personidenter: Set<String>): Set<UUID> {
        val fagsakIDer = mutableSetOf<UUID>()
        Stønadstype.values().forEach {
            behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(it, personidenter)?.let { fagsakIDer.add(it.fagsakId) }
        }
        return fagsakIDer
    }

}