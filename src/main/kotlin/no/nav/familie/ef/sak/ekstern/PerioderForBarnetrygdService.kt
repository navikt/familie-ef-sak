package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.erFullOvergangsstønad
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.springframework.stereotype.Service

/**
 * Service som henter perioder fra EF sin base og infotrygd og slår disse sammen
 * Skal kun returnere de som har full overgangsstønad
 * Responsen brukes for å vurdere om personen skal få utvidet barnetrygd
 */
@Service
class PerioderForBarnetrygdService(private val pdlClient: PdlClient,
                                   private val fagsakService: FagsakService,
                                   private val behandlingService: BehandlingService,
                                   private val tilkjentYtelseService: TilkjentYtelseService) {

    fun hentPerioder(request: PersonIdent): PerioderOvergangsstønadResponse {
        val personIdenter = pdlClient.hentPersonidenter(request.ident, true).identer()
        val perioder = fagsakService.finnFagsak(personIdenter, Stønadstype.OVERGANGSSTØNAD)
                               ?.let { behandlingService.finnSisteIverksatteBehandling(it.id) }
                               ?.let { hentPerioderFraReplika() + hentPerioderFraEf(it) }
                       ?: emptyList()

        return PerioderOvergangsstønadResponse(perioder)
    }

    private fun hentPerioderFraReplika(): List<PeriodeOvergangsstønad> {
        return emptyList() // TODO("Not yet implemented")
    }

    private fun hentPerioderFraEf(it: Behandling) =
            tilkjentYtelseService.hentForBehandling(it.id).andelerTilkjentYtelse
                    .filter(AndelTilkjentYtelse::erFullOvergangsstønad)
                    .map(AndelTilkjentYtelse::tilEksternPeriodeOvergangsstønad)
}