package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.dummy.TilkjentYtelseTestDTO
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.time.YearMonth
import java.util.*


@Disabled
class KonsistensavstemmingOpprettPostmanRequester {

    @Test
    internal fun `1 periode bak i tid`() {
        val personident = "0"
        lagTilkjentYtelse(personident,
                          listOf(lagAndel(personident = personident,
                                          beløp = 100,
                                          fom = YearMonth.of(2020, 1),
                                          tom = YearMonth.of(2020, 2))))
    }

    @Test
    internal fun `1 periode`() {
        val personident = "1"
        lagTilkjentYtelse(personident,
                          listOf(lagAndel(personident = personident,
                                          beløp = 100,
                                          fom = YearMonth.of(2021, 1),
                                          tom = YearMonth.of(2021, 2))))
    }

    @Test
    internal fun `1 periode, en ny periode etter`() {
        val personident = "2"
        val ty = lagTilkjentYtelse(personident,
                                   listOf(lagAndel(personident = personident,
                                                   beløp = 100,
                                                   fom = YearMonth.of(2021, 1),
                                                   tom = YearMonth.of(2021, 2))))

        lagTilkjentYtelse(personident,
                          listOf(ty.nyTilkjentYtelse.andelerTilkjentYtelse.first(),
                                 lagAndel(personident = personident,
                                          beløp = 100,
                                          fom = YearMonth.of(2021, 3),
                                          tom = YearMonth.of(2021, 4))))
    }

    @Test
    internal fun `2 perioder, opphør andre perioden`() {
        val personident = "3"
        val ty = lagTilkjentYtelse(personident,
                                   listOf(lagAndel(personident = personident,
                                                   beløp = 100,
                                                   fom = YearMonth.of(2021, 1),
                                                   tom = YearMonth.of(2021, 2)),
                                          lagAndel(personident = personident,
                                                   beløp = 100,
                                                   fom = YearMonth.of(2021, 3),
                                                   tom = YearMonth.of(2021, 5))))

        lagTilkjentYtelse(personident, listOf(ty.nyTilkjentYtelse.andelerTilkjentYtelse.first()))
    }

    @Test
    internal fun `2 perioder, en endring i andre og en ny periode etter`() {
        val personident = "4"
        val ty = lagTilkjentYtelse(personident,
                                   listOf(lagAndel(personident = personident,
                                                   beløp = 100,
                                                   fom = YearMonth.of(2021, 1),
                                                   tom = YearMonth.of(2021, 2)),
                                          lagAndel(personident = personident,
                                                   beløp = 100,
                                                   fom = YearMonth.of(2021, 3),
                                                   tom = YearMonth.of(2021, 5))))

        lagTilkjentYtelse(personident,
                          listOf(ty.nyTilkjentYtelse.andelerTilkjentYtelse.first(),
                                 lagAndel(personident = personident,
                                          beløp = 100,
                                          fom = YearMonth.of(2021, 4),
                                          tom = YearMonth.of(2021, 4)),
                                 lagAndel(personident = personident,
                                          beløp = 200,
                                          fom = YearMonth.of(2021, 5),
                                          tom = YearMonth.of(2021, 6))))
    }

    @Test
    internal fun `3 periode, 1 bak i tid och en frem i tid, endring på tredje perioden`() {
        val personident = "5"
        val ty = lagTilkjentYtelse(personident,
                                   listOf(lagAndel(personident = personident,
                                                   beløp = 100,
                                                   fom = YearMonth.of(2020, 1),
                                                   tom = YearMonth.of(2020, 2)),
                                          lagAndel(personident = personident,
                                                   beløp = 100,
                                                   fom = YearMonth.of(2020, 3),
                                                   tom = YearMonth.of(2021, 2)),
                                          lagAndel(
                                                  personident = personident,
                                                  beløp = 100,
                                                  fom = YearMonth.of(2021, 4),
                                                  tom = YearMonth.of(2022, 2),
                                          )))
        lagTilkjentYtelse(personident,
                          listOf(ty.nyTilkjentYtelse.andelerTilkjentYtelse[0],
                                 ty.nyTilkjentYtelse.andelerTilkjentYtelse[1],
                                 lagAndel(
                                         personident = personident,
                                         beløp = 100,
                                         fom = YearMonth.of(2021, 3),
                                         tom = YearMonth.of(2022, 2),
                                 )))
    }

    private fun lagTilkjentYtelse(personident: String,
                                  andelerTilkjentYtelse: List<AndelTilkjentYtelseDTO>): TilkjentYtelseTestDTO {
        val tilkjentYtelseTestDTO = TilkjentYtelseTestDTO(TilkjentYtelseDTO(behandlingId = UUID.randomUUID(),
                                                                            søker = personident,
                                                                            andelerTilkjentYtelse = andelerTilkjentYtelse),
                                                          stønadstype = Stønadstype.OVERGANGSSTØNAD)
        val id = personidenter.merge(personident, 1) { i, _ -> i + 1 }
        val testname = Thread.currentThread().getStackTrace()[2].getMethodName()
        val data = mutableMapOf("test" to testname,
                                "part" to id,
                                "data" to tilkjentYtelseTestDTO)
        File("postman/${personident}_${id}").writeBytes(om.writeValueAsBytes(data))
        return tilkjentYtelseTestDTO
    }

    private fun lagAndel(personident: String, beløp: Int, fom: YearMonth, tom: YearMonth) =
            AndelTilkjentYtelseDTO(beløp = beløp,
                                   stønadFom = fom.atDay(1),
                                   stønadTom = tom.atEndOfMonth(),
                                   personIdent = personident,
                                   kildeBehandlingId = null)

    companion object {

        val personidenter = mutableMapOf<String, Int>()
        val om = objectMapper.writerWithDefaultPrettyPrinter()
    }

}