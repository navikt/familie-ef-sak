package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate
import java.util.*

data class TilkjentYtelseDTO(val søker: String,
                             val vedtaksdato: LocalDate = LocalDate.now(),
        // Skal man opprette andeler som frontend godkjenner eller skal frontend sende inn andeler.
        // Hvis frontend sender inn andeler kanskje id kan være optional og att backend oppretter id
                             val id: UUID = UUID.randomUUID(),
                             val behandlingId: UUID,
                             val opphørFom: LocalDate? = null,
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
                                  val kildeBehandlingId: UUID?,
                                  val personIdent: String)

