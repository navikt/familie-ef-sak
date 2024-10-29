package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import java.time.LocalDate
import java.util.Random
import java.util.UUID

object DataGenerator {
    private fun tilfeldigFødselsnummer() = Random().nextInt(Int.MAX_VALUE).toString()

    private fun flereTilfeldigeAndelerTilkjentYtelse(
        antall: Int,
        behandlingId: UUID,
    ): List<AndelTilkjentYtelse> = (1..antall).map { tilfeldigAndelTilkjentYtelse(behandlingId = behandlingId) }.toList()

    private fun tilfeldigAndelTilkjentYtelse(
        beløp: Int = Random().nextInt(20_000) + 1,
        stønadFom: LocalDate = LocalDate.now(),
        stønadTom: LocalDate = LocalDate.now(),
        behandlingId: UUID,
        personIdent: String = tilfeldigFødselsnummer(),
    ) = AndelTilkjentYtelse(
        beløp = beløp,
        stønadFom = stønadFom,
        stønadTom = stønadTom,
        kildeBehandlingId = behandlingId,
        inntekt = 0,
        inntektsreduksjon = 0,
        samordningsfradrag = 0,
        personIdent = personIdent,
    )

    fun tilfeldigTilkjentYtelse(
        behandling: Behandling = behandling(fagsak()),
        antallAndelerTilkjentYtelse: Int = 1,
    ): TilkjentYtelse {
        val andelerTilkjentYtelse = flereTilfeldigeAndelerTilkjentYtelse(antallAndelerTilkjentYtelse, behandling.id)
        return TilkjentYtelse(
            personident = tilfeldigFødselsnummer(),
            behandlingId = behandling.id,
            startdato = andelerTilkjentYtelse.minOf { it.stønadFom },
            andelerTilkjentYtelse = andelerTilkjentYtelse,
        )
    }
}
