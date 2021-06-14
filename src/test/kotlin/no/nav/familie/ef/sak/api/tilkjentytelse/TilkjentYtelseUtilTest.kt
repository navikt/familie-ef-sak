package no.nav.familie.ef.sak.api.tilkjentytelse

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class TilkjentYtelseUtilTest {

    private val personIdent1 = "1"
    private val personIdent2 = "2"
    private val behandling1 = UUID.fromString("c40145db-0484-4ac5-83d4-0b7e53b2c7f7")
    private val behandling2 = UUID.fromString("de4f2646-774c-41ea-b138-ffbe2c113e6d")
    private val behandling3 = UUID.randomUUID()

    private val andel1 = lagAndel(100, LocalDate.of(2021, 1, 1))
    private val andel2 = lagAndel(200, LocalDate.of(2022, 1, 1))
    private val andel3 = lagAndel(300, LocalDate.of(2023, 1, 1))
    private val tilkjentYtelse = lagTilkjentYtelse(listOf(andel1, andel2, andel3))

    @Test
    internal fun `mergeAndeler - forrige og ny er eksakt like`() {
        assertThat(mergeAndeler(tilkjentYtelse, tilkjentYtelse).andelerTilkjentYtelse)
                .isEqualTo(tilkjentYtelse.andelerTilkjentYtelse)
    }

    @Test
    internal fun `mergeAndeler - forrige og ny er like med med ulike kildeBehandlingId og personIdent`() {
        val forrigeTilkjentYtelse = lagNyTilkjentYtelse(behandling2, personIdent2, andel1, andel2, andel3)
        assertThat(mergeAndeler(tilkjentYtelse, forrigeTilkjentYtelse).andelerTilkjentYtelse)
                .isEqualTo(tilkjentYtelse.andelerTilkjentYtelse)
    }

    @Test
    internal fun `mergeAndeler - har fjernet siste periode`() {
        val nyTilkjentYtelse = lagNyTilkjentYtelse(behandling2, personIdent2, andel1, andel2)
        assertThat(mergeAndeler(nyTilkjentYtelse, tilkjentYtelse).andelerTilkjentYtelse)
                .isEqualTo(tilkjentYtelse.copy(andelerTilkjentYtelse = listOf(andel1, andel2)).andelerTilkjentYtelse)
    }

    @Test
    internal fun `mergeAndeler - har fjernet første periode`() {
        val nyTilkjentYtelse = lagNyTilkjentYtelse(behandling2, personIdent2, andel2, andel3)
        assertThat(mergeAndeler(nyTilkjentYtelse, tilkjentYtelse).andelerTilkjentYtelse)
                .isEqualTo(tilkjentYtelse.copy(andelerTilkjentYtelse = listOf(andel2, andel3)).andelerTilkjentYtelse)
    }

    @Test
    internal fun `mergeAndeler - har endret andre perioden`() {
        val nyAndel1 = andel1.med(behandling2, personIdent2)
        val nyAndel2 = andel2.copy(beløp = 2).med(behandling2, personIdent2)
        val nyAndel3 = andel3.med(behandling2, personIdent2)
        val nyTilkjentYtelse = lagNyTilkjentYtelse(behandling2, personIdent2, nyAndel1, nyAndel2, nyAndel3)

        val forventedeNyeAndeler = listOf(andel1, nyAndel2, nyAndel3)
        assertThat(mergeAndeler(nyTilkjentYtelse, tilkjentYtelse).andelerTilkjentYtelse)
                .isEqualTo(tilkjentYtelse.copy(andelerTilkjentYtelse = forventedeNyeAndeler).andelerTilkjentYtelse)
    }

    private fun lagNyTilkjentYtelse(behandlingId: UUID,
                                    personIdent: String,
                                    vararg andeler: AndelTilkjentYtelse): TilkjentYtelse {
        return lagTilkjentYtelse(andelerTilkjentYtelse = andeler.map { it.med(behandlingId, personIdent) }.toList(),
                                 id = UUID.randomUUID(),
                                 behandlingId = behandlingId,
                                 personident = personIdent)
    }

    private fun AndelTilkjentYtelse.med(kildeBehandlingId: UUID,
                                        personIdent: String) = this.copy(kildeBehandlingId = kildeBehandlingId,
                                                                         personIdent = personIdent)

    private fun lagAndel(beløp: Int, fom: LocalDate, personIdent: String = personIdent1, kildeBehandlingId: UUID = behandling1) =
            lagAndelTilkjentYtelse(beløp = beløp,
                                   fraOgMed = fom,
                                   tilOgMed = YearMonth.of(fom.year, fom.month).atEndOfMonth(),
                                   personIdent = personIdent,
                                   kildeBehandlingId = kildeBehandlingId,
                                   inntekt = 0,
                                   samordningsfradrag = 0,
                                   inntektsreduksjon = 0)
}