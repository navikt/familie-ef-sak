package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.YtelseType
import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

object DataGenerator {
    fun tilfeldigFødselsnummer() = Random().nextInt(Int.MAX_VALUE).toString()
    private fun tilfeldigSaksnummer() = "SAK" + Random().nextInt(Int.MAX_VALUE)

    private fun flereTilfeldigeAndelerTilkjentYtelse(antall: Int): List<AndelTilkjentYtelse> =
            (1..antall).map { tilfeldigAndelTilkjentYtelse() }.toList()

    fun tilfeldigAndelTilkjentYtelse(beløp: Int = Random().nextInt(),
                                     stønadFom: LocalDate = LocalDate.now(),
                                     stønadTom: LocalDate = LocalDate.now(),
                                     personIdent: String = tilfeldigFødselsnummer(),
                                     type: YtelseType = YtelseType.OVERGANGSSTØNAD) =
            AndelTilkjentYtelse(beløp = beløp,
                                stønadFom = stønadFom,
                                stønadTom = stønadTom,
                                personIdent = personIdent,
                                type = type)

    fun tilfeldigTilkjentYtelse(andelerTilkjentYtelse: List<AndelTilkjentYtelse>) =
            TilkjentYtelse(personident = tilfeldigFødselsnummer(),
                           stønadFom = LocalDate.now(),
                           stønadTom = LocalDate.now(),
                           saksnummer = tilfeldigSaksnummer(),
                           vedtaksdato = LocalDate.now(),
                           behandlingId = Math.random().toLong(),
                           saksbehandler = tilfeldigFødselsnummer(),
                           andelerTilkjentYtelse = andelerTilkjentYtelse)

    fun tilfeldigTilkjentYtelse(antallAndelerTilkjentYteelse: Int = 1) =
            TilkjentYtelse(personident = tilfeldigFødselsnummer(),
                           stønadFom = LocalDate.now(),
                           stønadTom = LocalDate.now(),
                           saksnummer = tilfeldigSaksnummer(),
                           saksbehandler = tilfeldigFødselsnummer(),
                           vedtaksdato = LocalDate.now(),
                           behandlingId = Math.random().toLong(),
                           andelerTilkjentYtelse = flereTilfeldigeAndelerTilkjentYtelse(antallAndelerTilkjentYteelse))

    fun tilfeldigTilkjentYtelseDto(): TilkjentYtelseDTO {
        val søker = tilfeldigFødselsnummer()

        return TilkjentYtelseDTO(søker = søker,
                                 saksnummer = tilfeldigSaksnummer(),
                                 behandlingId = Random().nextLong(),
                                 andelerTilkjentYtelse =
                                 listOf(AndelTilkjentYtelseDTO(beløp = Random().nextInt(100_000),
                                                               stønadFom = LocalDate.now(),
                                                               stønadTom = LocalDate.now(),
                                                               personIdent = tilfeldigFødselsnummer(),
                                                               type = YtelseType.OVERGANGSSTØNAD)))
    }

    fun lagAndelTilkjentYtelse(fom: String,
                               tom: String,
                               personIdent: String,
                               ytelseType: YtelseType = YtelseType.OVERGANGSSTØNAD,
                               beløp: Int = Random().nextInt(),
                               periodeIdOffset: Long? = null): AndelTilkjentYtelse {

        return AndelTilkjentYtelse(
                personIdent = personIdent,
                beløp = beløp,
                stønadFom = dato(fom),
                stønadTom = dato(tom),
                type = ytelseType,
                periodeId = periodeIdOffset
        )
    }

    fun dato(s: String) = LocalDate.parse(s)
    fun årMnd(s: String) = YearMonth.parse(s)

}