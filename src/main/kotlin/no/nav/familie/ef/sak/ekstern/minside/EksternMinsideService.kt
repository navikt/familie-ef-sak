package no.nav.familie.ef.sak.ekstern.minside

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service

@Service
class EksternMinsideService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val personService: PersonService,
) {
    fun hentStønadsperioderForBruker(personIdent: String): MineStønaderDto {
        val personIdenter = personService.hentPersonIdenter(personIdent).identer()
        val tilkjentYtelseOvergangsstønad = tilkjentYtelseForStønad(personIdenter, StønadType.OVERGANGSSTØNAD)
        val tilkjentYtelseBarnetilsyn = tilkjentYtelseForStønad(personIdenter, StønadType.BARNETILSYN)
        val tilkjentYtelseSkolepenger = tilkjentYtelseForStønad(personIdenter, StønadType.SKOLEPENGER)

        return MineStønaderDto(
            overgangsstønad = tilStønadsperiode(tilkjentYtelseOvergangsstønad),
            barnetilsyn = tilStønadsperiode(tilkjentYtelseBarnetilsyn),
            skolepenger = tilStønadsperiode(tilkjentYtelseSkolepenger),
        )
    }

    private fun tilStønadsperiode(tilkjentYtelse: TilkjentYtelse?): List<StønadsperiodeDto> {
        if (tilkjentYtelse == null) {
            return emptyList()
        }
        return tilkjentYtelse.andelerTilkjentYtelse.map { andel ->
            StønadsperiodeDto(
                fraDato = andel.stønadFom,
                tilDato = andel.stønadTom,
                beløp = andel.beløp,
                inntektsgrunnlag = andel.inntekt,
                samordningsfradrag = andel.samordningsfradrag,
            )
        }
    }

    private fun tilkjentYtelseForStønad(
        personIdenter: Set<String>,
        stønadstype: StønadType,
    ): TilkjentYtelse? =
        fagsakService
            .finnFagsak(personIdenter, stønadstype)
            ?.let { fagsak -> behandlingService.finnSisteIverksatteBehandling(fagsak.id) }
            ?.let { behandling -> tilkjentYtelseService.hentForBehandling(behandling.id) }
}
