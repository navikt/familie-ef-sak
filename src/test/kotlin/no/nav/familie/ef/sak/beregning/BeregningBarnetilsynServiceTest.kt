package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeregningsgrunnlagBarnetilsynDto
import no.nav.familie.ef.sak.felles.dto.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class BeregningBarnetilsynServiceTest {

    @Test
    fun `merge to påfølgende måneder med like beløp, forvent én periode`() {
        val januar = YearMonth.of(2000, 1)
        val februar = YearMonth.of(2000, 2)
        val forventetFraDato = januar.atDay(1)
        val forventetTilDato = februar.atEndOfMonth()
        val beløpsperioder = listOf(lagBeløpsperiode(forventetFraDato, januar.atEndOfMonth()),
                                    lagBeløpsperiode(februar.atDay(1), forventetTilDato))

        val resultat = beløpsperioder.merge()

        assertThat(resultat).hasSize(1)
        assertThat(resultat.first().periode.fradato).isEqualTo(forventetFraDato)
        assertThat(resultat.first().periode.tildato).isEqualTo(forventetTilDato)
    }

    @Test
    fun `merge to påfølgende måneder med forskjellige beløp, forvent to perioder`() {
        val januar = YearMonth.of(2000, 1)
        val februar = YearMonth.of(2000, 2)
        val forventetFraDato = januar.atDay(1)
        val forventetTilDato = februar.atEndOfMonth()
        val beløpsperioder = listOf(lagBeløpsperiode(fraDato = forventetFraDato,
                                                     tilDato = januar.atEndOfMonth(),
                                                     beløp = BigDecimal(100)),
                                    lagBeløpsperiode(fraDato = februar.atDay(1),
                                                     tilDato = forventetTilDato,
                                                     beløp = BigDecimal(200)))

        val resultat = beløpsperioder.merge()

        assertThat(resultat).hasSize(2)
        assertThat(resultat.first().periode.fradato).isEqualTo(forventetFraDato)
        assertThat(resultat.last().periode.tildato).isEqualTo(forventetTilDato)
    }

    private fun lagBeløpsperiode(fraDato: LocalDate,
                                 tilDato: LocalDate,
                                 beløp: BigDecimal = BigDecimal(100)): BeløpsperiodeBarnetilsynDto {
        return BeløpsperiodeBarnetilsynDto(periode = Periode(fraDato, tilDato),
                                           beløp = beløp,
                                           beregningsgrunnlag = BeregningsgrunnlagBarnetilsynDto(utgiftsbeløp = BigDecimal.ZERO,
                                                                                                 kontantstøttebeløp = BigDecimal.ZERO,
                                                                                                 tilleggsstønadsbeløp = BigDecimal.ZERO,
                                                                                                 antallBarn = 1))
    }
}