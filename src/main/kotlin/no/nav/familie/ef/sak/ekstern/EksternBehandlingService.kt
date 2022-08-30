package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
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
    val fagsakService: FagsakService,
    val personService: PersonService,
    val infotrygdService: InfotrygdService
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

    fun kanOppretteFørstegangsbehandling(ident: String, type: StønadType): Boolean {
        val allePersonIdenter = personService.hentPersonIdenter(ident).identer.map { it.ident }.toSet()
        val fagsak = fagsakService.finnFagsak(allePersonIdenter, type)

        return harIngenReelleBehandlinger(fagsak) && harIngenInnslagIInfotrygd(ident, type)
    }

    private fun harIngenInnslagIInfotrygd(
        ident: String,
        type: StønadType
    ) = !infotrygdService.eksisterer(ident, setOf(type))

    private fun harIngenReelleBehandlinger(fagsak: Fagsak?): Boolean {
        return fagsak?.let {
            behandlingService.hentBehandlinger(fagsak.id).none { it.resultat != BehandlingResultat.HENLAGT }
        } ?: true
    }
}
