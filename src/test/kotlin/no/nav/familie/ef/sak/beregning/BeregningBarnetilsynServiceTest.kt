package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeregningsgrunnlagBarnetilsynDto
import no.nav.familie.ef.sak.felles.dto.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.BigDecimal.TEN
import java.math.BigDecimal.ZERO
import java.time.LocalDate
import java.time.YearMonth

internal class BeregningBarnetilsynServiceTest {

    @Test
    fun `split en utgiftsperiode som varer fra januar til desember i 12 mnd`() {

        val januar = YearMonth.of(2000, 1)
        val desember = YearMonth.of(2000, 12)

        val utgiftsperiodeDto = UtgiftsperiodeDto(januar, desember, barn = listOf(), utgifter = TEN)
        val resultat = utgiftsperiodeDto.split()

        assertThat(resultat).hasSize(12)

    }

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

    @Test
    fun `merge to påfølgende måneder med hull ifm fradatoer, forvent NotImplementedError`() {
        val januar = YearMonth.of(2000, 1)
        val mars = YearMonth.of(2000, 3)
        val forventetFraDato = januar.atDay(1)
        val forventetTilDato = mars.atEndOfMonth()
        val beløpsperioder = listOf(lagBeløpsperiode(fraDato = forventetFraDato,
                                                     tilDato = januar.atEndOfMonth(),
                                                     beløp = BigDecimal(100)),
                                    lagBeløpsperiode(fraDato = mars.atDay(1),
                                                     tilDato = forventetTilDato,
                                                     beløp = BigDecimal(100)))
        val error = assertThrows<NotImplementedError> { beløpsperioder.merge() }
        assertThat(error.message).isEqualTo("Støtter ikke hull i perioder")
    }

    @Test
    fun `merge to påfølgende måneder med hull ifm fradatoer og forskjellige beløp, forvent NotImplementedError`() {
        val januar = YearMonth.of(2000, 1)
        val mars = YearMonth.of(2000, 3)
        val forventetFraDato = januar.atDay(1)
        val forventetTilDato = mars.atEndOfMonth()
        val beløpsperioder = listOf(lagBeløpsperiode(fraDato = forventetFraDato,
                                                     tilDato = januar.atEndOfMonth(),
                                                     beløp = BigDecimal(100)),
                                    lagBeløpsperiode(fraDato = mars.atDay(1),
                                                     tilDato = forventetTilDato,
                                                     beløp = BigDecimal(200)))
        val error = assertThrows<NotImplementedError> { beløpsperioder.merge() }
        assertThat(error.message).isEqualTo("Støtter ikke hull i perioder")
    }

    private fun lagBeløpsperiode(fraDato: LocalDate,
                                 tilDato: LocalDate,
                                 beløp: BigDecimal = BigDecimal(100)): BeløpsperiodeBarnetilsynDto {
        return BeløpsperiodeBarnetilsynDto(periode = Periode(fraDato, tilDato),
                                           beløp = beløp,
                                           beregningsgrunnlag = BeregningsgrunnlagBarnetilsynDto(utgiftsbeløp = ZERO,
                                                                                                 kontantstøttebeløp = ZERO,
                                                                                                 tilleggsstønadsbeløp = ZERO,
                                                                                                 antallBarn = 1))
    }
}