package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.behandling.BehandlingService
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
    val behandlingService: BehandlingService,
    val fagsakService: FagsakService
) {

    fun harLøpendeStønad(personidenter: Set<String>): Boolean {
        val behandlingIDer = hentAlleBehandlingIDer(personidenter)
        val sisteStønadsdato = behandlingIDer
            .map(tilkjentYtelseService::hentForBehandling)
            .mapNotNull { it.andelerTilkjentYtelse.maxOfOrNull(AndelTilkjentYtelse::stønadTom) }
            .maxOfOrNull { it } ?: LocalDate.MIN
        return sisteStønadsdato >= LocalDate.now()
    }

    private fun hentAlleBehandlingIDer(personidenter: Set<String>): Set<UUID> {
        return StønadType.values().mapNotNull { fagsakService.finnFagsak(personidenter, it) }
            .mapNotNull { behandlingService.finnSisteIverksatteBehandling(it.id) }
            .map { it.id }
            .toSet()
    }
}
