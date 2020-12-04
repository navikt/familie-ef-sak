package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.*
import java.time.LocalDate
import java.util.*

object DataGenerator {

    private fun tilfeldigFødselsnummer() = Random().nextInt(Int.MAX_VALUE).toString()

    private fun flereTilfeldigeAndelerTilkjentYtelse(antall: Int, behandlingId: UUID): List<AndelTilkjentYtelse> =
            (1..antall).map { tilfeldigAndelTilkjentYtelse(behandlingId = behandlingId) }.toList()

    private fun tilfeldigAndelTilkjentYtelse(beløp: Int = Random().nextInt(),
                                             stønadFom: LocalDate = LocalDate.now(),
                                             stønadTom: LocalDate = LocalDate.now(),
                                             behandlingId: UUID,
                                             personIdent: String = tilfeldigFødselsnummer()) =
            AndelTilkjentYtelse(beløp = beløp,
                                stønadFom = stønadFom,
                                stønadTom = stønadTom,
                                opprinnelsesbehandlingId = behandlingId,
                                personIdent = personIdent)

    fun tilfeldigTilkjentYtelse(behandling: Behandling = behandling(fagsak()), antallAndelerTilkjentYtelse: Int = 1) =
            TilkjentYtelse(personident = tilfeldigFødselsnummer(),
                           stønadFom = LocalDate.now(),
                           stønadTom = LocalDate.now(),
                           vedtaksdato = LocalDate.now(),
                           behandlingId = behandling.id,
                           andelerTilkjentYtelse = flereTilfeldigeAndelerTilkjentYtelse(antallAndelerTilkjentYtelse, behandling.id))
}