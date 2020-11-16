package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

object DataGenerator {

    private fun tilfeldigFødselsnummer() = Random().nextInt(Int.MAX_VALUE).toString()

    private fun flereTilfeldigeAndelerTilkjentYtelse(antall: Int): List<AndelTilkjentYtelse> =
            (1..antall).map { tilfeldigAndelTilkjentYtelse() }.toList()

    private fun tilfeldigAndelTilkjentYtelse(beløp: Int = Random().nextInt(),
                                             stønadFom: LocalDate = LocalDate.now(),
                                             stønadTom: LocalDate = LocalDate.now(),
                                             personIdent: String = tilfeldigFødselsnummer()) =
            AndelTilkjentYtelse(beløp = beløp,
                                stønadFom = stønadFom,
                                stønadTom = stønadTom,
                                personIdent = personIdent)

    fun tilfeldigTilkjentYtelse(antallAndelerTilkjentYtelse: Int = 1, behandlingId: UUID) =
            TilkjentYtelse(personident = tilfeldigFødselsnummer(),
                           stønadFom = LocalDate.now(),
                           stønadTom = LocalDate.now(),
                           saksbehandler = tilfeldigFødselsnummer(),
                           vedtaksdato = LocalDate.now(),
                           behandlingId = behandlingId,
                           andelerTilkjentYtelse = flereTilfeldigeAndelerTilkjentYtelse(antallAndelerTilkjentYtelse))

    fun lagAndelTilkjentYtelse(fom: String,
                               tom: String,
                               personIdent: String,
                               beløp: Int = Random().nextInt(),
                               periodeIdOffset: Long? = null): AndelTilkjentYtelse {

        return AndelTilkjentYtelse(personIdent = personIdent,
                                   beløp = beløp,
                                   stønadFom = dato(fom),
                                   stønadTom = dato(tom),
                                   periodeId = periodeIdOffset)
    }

    private fun dato(s: String): LocalDate = LocalDate.parse(s)
    fun årMnd(s: String): YearMonth = YearMonth.parse(s)

}