package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeregningsgrunnlagBarnetilsynDto
import no.nav.familie.ef.sak.felles.dto.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.TEN
import java.math.BigDecimal.ZERO
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class BeregningBarnetilsynServiceTest {


       val service : BeregningBarnetilsynService = BeregningBarnetilsynService()

    @Test
    fun `Skal lage tre perioder når tre forskjellige beløp i en 12,md periode`() {
        val januar = YearMonth.of(2022, 1)
        val mars = YearMonth.of(2022, 3)
        val april = YearMonth.of(2022, 4)
        val juli = YearMonth.of(2022, 7)
        val aug = YearMonth.of(2022, 8)
        val desember = YearMonth.of(2022, 12)
       val barnUUID = UUID.randomUUID()
        val utgiftsperiode1 = UtgiftsperiodeDto(januar, mars, barn = listOf(barnUUID), utgifter = TEN)
        val utgiftsperiode2 = UtgiftsperiodeDto(april, juli, barn = listOf(barnUUID), utgifter = ONE)
        val utgiftsperiode3 = UtgiftsperiodeDto(aug, desember, barn = listOf(barnUUID), utgifter = TEN+TEN)

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode1,
                                                                                               utgiftsperiode2,
                                                                                               utgiftsperiode3),
                                                                      kontantstøttePerioder = listOf(),
                                                                      tilleggsstønadsperioder = listOf())

        assertThat(beregnYtelseBarnetilsyn).hasSize(3)
    }

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
        assertThat(beløpsperioder.merge()).hasSize(2)

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

        assertThat(beløpsperioder.merge()).hasSize(2)
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