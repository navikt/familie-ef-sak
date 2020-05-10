package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.dto.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.dto.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.dto.YtelseType
import java.time.LocalDate
import java.util.*

object DataGenerator {
    fun tilfeldigAndelTilkjentYtelse(
            tilkjentYtelseId: Long,
            beløp: Int=Random().nextInt(),
            stønadFom: LocalDate = LocalDate.now(),
            stønadTom: LocalDate = LocalDate.now(),
            type: YtelseType = YtelseType.OVERGANGSSTØNAD
    ) = AndelTilkjentYtelse(
            tilkjentYtelseId = tilkjentYtelseId,
            personIdentifikator = UUID.randomUUID().toString().substring(0, 20),
            beløp = beløp,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
            type = type)

    fun flereTilfeldigeAndelerTilkjentYtelse(tilkjentYtelseId: Long, antall: Int): List<AndelTilkjentYtelse> =
        (1 .. antall).map { tilfeldigAndelTilkjentYtelse(tilkjentYtelseId) }.toList()

    val defaultTilkjentYtelse = TilkjentYtelse(
            personIdentifikator = "12345678910",
            stønadFom = LocalDate.now(),
            stønadTom = LocalDate.now(),
            saksnummer = "SAK123",
            vedtaksdato = LocalDate.now()
    )
}