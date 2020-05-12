package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.YtelseType
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

    fun tilfeldigTilkjentYtelse(
            personIdentifikator: String = Random().nextInt(Int.MAX_VALUE).toString(),
            stønadFom: LocalDate = LocalDate.now(),
            stønadTom: LocalDate = LocalDate.now(),
            saksnummer: String = "SAK"+Random().nextInt(Int.MAX_VALUE),
            vedtaksdato: LocalDate = LocalDate.now()

    ) = TilkjentYtelse(
            personIdentifikator = personIdentifikator,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
            saksnummer = saksnummer,
            vedtaksdato = vedtaksdato
    )
}