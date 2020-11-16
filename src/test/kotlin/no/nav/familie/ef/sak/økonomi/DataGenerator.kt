package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.YtelseType
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

object DataGenerator {

    private fun tilfeldigFødselsnummer() = Random().nextInt(Int.MAX_VALUE).toString()
    private fun tilfeldigSaksnummer() = "SAK" + Random().nextInt(Int.MAX_VALUE)

    private fun flereTilfeldigeAndelerTilkjentYtelse(antall: Int): List<AndelTilkjentYtelse> =
            (1..antall).map { tilfeldigAndelTilkjentYtelse() }.toList()

    private fun tilfeldigAndelTilkjentYtelse(beløp: Int = Random().nextInt(),
                                             stønadFom: LocalDate = LocalDate.now(),
                                             stønadTom: LocalDate = LocalDate.now(),
                                             personIdent: String = tilfeldigFødselsnummer(),
                                             type: YtelseType = YtelseType.OVERGANGSSTØNAD) =
            AndelTilkjentYtelse(beløp = beløp,
                                stønadFom = stønadFom,
                                stønadTom = stønadTom,
                                personIdent = personIdent,
                                type = type)

    fun tilfeldigTilkjentYtelse(andelerTilkjentYtelse: List<AndelTilkjentYtelse>, behandlingId: UUID) =
            TilkjentYtelse(personident = tilfeldigFødselsnummer(),
                           stønadFom = LocalDate.now(),
                           stønadTom = LocalDate.now(),
                           vedtaksdato = LocalDate.now(),
                           behandlingId = behandlingId,
                           saksbehandler = tilfeldigFødselsnummer(),
                           andelerTilkjentYtelse = andelerTilkjentYtelse)

    fun tilfeldigTilkjentYtelse(antallAndelerTilkjentYtelse: Int = 1, behandlingId: UUID) =
            TilkjentYtelse(personident = tilfeldigFødselsnummer(),
                           stønadFom = LocalDate.now(),
                           stønadTom = LocalDate.now(),
                           saksbehandler = tilfeldigFødselsnummer(),
                           vedtaksdato = LocalDate.now(),
                           behandlingId = behandlingId,
                           andelerTilkjentYtelse = flereTilfeldigeAndelerTilkjentYtelse(antallAndelerTilkjentYtelse))

    fun tilfeldigTilkjentYtelseDto(): TilkjentYtelseDTO {
        val søker = tilfeldigFødselsnummer()

        return TilkjentYtelseDTO(søker = søker,
                                 behandlingId = UUID.randomUUID(),
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

        return AndelTilkjentYtelse(personIdent = personIdent,
                                   beløp = beløp,
                                   stønadFom = dato(fom),
                                   stønadTom = dato(tom),
                                   type = ytelseType,
                                   periodeId = periodeIdOffset)
    }

    private fun dato(s: String): LocalDate = LocalDate.parse(s)
    fun årMnd(s: String): YearMonth = YearMonth.parse(s)

}