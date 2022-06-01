package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class EksternBehandlingService(
    val tilkjentYtelseService: TilkjentYtelseService,
    val behandlingRepository: BehandlingRepository,
    val fagsakService: FagsakService
) {

    fun finnesBehandlingFor(personidenter: Set<String>, stønadstype: StønadType?): Boolean {
        return if (stønadstype != null) {
            return eksistererBehandlingSomIkkeErBlankett(stønadstype, personidenter)
        } else {
            StønadType.values().any { eksistererBehandlingSomIkkeErBlankett(it, personidenter) }
        }
    }

    fun harLøpendeStønad(personidenter: Set<String>): Boolean {
        val behandlingIDer = hentAlleBehandlingIDer(personidenter)
        val sisteStønadsdato = behandlingIDer
            .map(tilkjentYtelseService::hentForBehandling)
            .mapNotNull { it.andelerTilkjentYtelse.maxOfOrNull(AndelTilkjentYtelse::stønadTom) }
            .maxOfOrNull { it } ?: LocalDate.MIN
        return sisteStønadsdato >= LocalDate.now()
    }

    /**
     * Hvis siste behandling er teknisk opphør, skal vi returnere false,
     * hvis ikke så skal vi returnere true hvis det finnes en behandling
     */
    private fun eksistererBehandlingSomIkkeErBlankett(
        stønadstype: StønadType,
        personidenter: Set<String>
    ): Boolean {
        return behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(stønadstype, personidenter)?.let {
            it.type != BehandlingType.TEKNISK_OPPHØR
        } ?: false
    }

    private fun hentAlleBehandlingIDer(personidenter: Set<String>): Set<UUID> {
        return StønadType.values().mapNotNull { fagsakService.finnFagsak(personidenter, it) }
            .mapNotNull { behandlingRepository.finnSisteIverksatteBehandling(it.id) }
            .map { it.id }
            .toSet()
    }
}
