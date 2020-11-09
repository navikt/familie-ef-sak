package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.YtelseType
import java.time.LocalDate
import java.util.*

data class TilkjentYtelseDTO(val søker: String,
                             val saksnummer: String,
                             val vedtaksdato: LocalDate = LocalDate.now(),
                             val id: UUID = UUID.randomUUID(),
                             val behandlingId: UUID,
                             val andelerTilkjentYtelse: List<AndelTilkjentYtelseDTO>) {

    fun valider() {
        if (andelerTilkjentYtelse.isEmpty()) {
            throw IllegalStateException("Request trenger minst én andel tilkjent ytelse")
        }

        andelerTilkjentYtelse.forEach {
            if (it.beløp < 0)
                throw IllegalStateException("Beløp på andel tilkjent ytelse er negativt")
            if (it.stønadTom < it.stønadFom)
                throw IllegalStateException("Stønad til-og-med-dato (${it.stønadTom}) " +
                                            "kan ikke være tidligere enn stønad fra-og-med-dato (${it.stønadFom})")
        }
    }
}

data class AndelTilkjentYtelseDTO(val beløp: Int,
                                  val stønadFom: LocalDate,
                                  val stønadTom: LocalDate,
                                  val personIdent: String,
                                  val type: YtelseType)

data class TilkjentYtelseTestDTO(val nyTilkjentYtelse: TilkjentYtelse, val forrigeTilkjentYtelse: TilkjentYtelse?)

