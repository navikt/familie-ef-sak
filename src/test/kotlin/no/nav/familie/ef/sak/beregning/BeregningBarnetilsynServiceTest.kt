package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynService
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningsgrunnlagBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.UtgiftsperiodeDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.mergeSammenhengendePerioder
import no.nav.familie.ef.sak.beregning.barnetilsyn.split
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


    val service: BeregningBarnetilsynService = BeregningBarnetilsynService()

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
        val utgiftsperiode3 = UtgiftsperiodeDto(aug, desember, barn = listOf(barnUUID), utgifter = TEN + TEN)

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode1,
                                                                                               utgiftsperiode2,
                                                                                               utgiftsperiode3),
                                                                      kontantstøttePerioder = listOf(),
                                                                      tilleggsstønadsperioder = listOf())

        assertThat(beregnYtelseBarnetilsyn).hasSize(3)
    }

    @Test
    fun `Skal lage seks perioder med tre forskjellige kontantstøtteperioder i en 12 mnd periode`() {
        val januar = YearMonth.of(2022, 1)
        val mars = YearMonth.of(2022, 3)
        val april = YearMonth.of(2022, 4)
        val juli = YearMonth.of(2022, 7)
        val august = YearMonth.of(2022, 8)
        val desember = YearMonth.of(2022, 12)
        val barnUUID = UUID.randomUUID()

        val utgiftsperiode1 = UtgiftsperiodeDto(januar, mars, barn = listOf(barnUUID), utgifter = TEN)
        val utgiftsperiode2 = UtgiftsperiodeDto(april, juli, barn = listOf(barnUUID), utgifter = ONE)
        val utgiftsperiode3 = UtgiftsperiodeDto(august, desember, barn = listOf(barnUUID), utgifter = TEN + TEN)

        val kontantStøtteperiodeJanuar = PeriodeMedBeløpDto(årMånedFra = januar, årMånedTil = januar, beløp = TEN)
        val kontantStøtteperiodeApril = PeriodeMedBeløpDto(årMånedFra = april, årMånedTil = april, beløp = TEN)
        val kontantStøtteperiodeAugust = PeriodeMedBeløpDto(årMånedFra = august, årMånedTil = august, beløp = TEN)

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode1,
                                                                                               utgiftsperiode2,
                                                                                               utgiftsperiode3),
                                                                      kontantstøttePerioder = listOf(kontantStøtteperiodeJanuar,
                                                                                                     kontantStøtteperiodeApril,
                                                                                                     kontantStøtteperiodeAugust),
                                                                      tilleggsstønadsperioder = listOf())

        assertThat(beregnYtelseBarnetilsyn).hasSize(6)
    }

    @Test
    fun `Skal lage fire perioder med to utgiftsperioder og med en overlappende kontantstøtteperiode`() {
        val januar = YearMonth.of(2022, 1)
        val juli = YearMonth.of(2022, 7)
        val august = YearMonth.of(2022, 8)
        val desember = YearMonth.of(2022, 12)
        val barnUUID = UUID.randomUUID()

        val utgiftsperiode1 = UtgiftsperiodeDto(januar, juli, barn = listOf(barnUUID), utgifter = TEN)
        val utgiftsperiode2 = UtgiftsperiodeDto(august, desember, barn = listOf(barnUUID), utgifter = ONE)


        val kontantStøtteperiodeJanuar = PeriodeMedBeløpDto(årMånedFra = juli, årMånedTil = august, beløp = TEN)


        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode1,
                                                                                               utgiftsperiode2),
                                                                      kontantstøttePerioder = listOf(kontantStøtteperiodeJanuar),
                                                                      tilleggsstønadsperioder = listOf())

        assertThat(beregnYtelseBarnetilsyn).hasSize(4)
    }

    @Test
    fun `Skal lage seks perioder med to utgiftsperioder og en overlappende kontantstøtte og tilleggsstønadsperiodemed`() {
        val januar = YearMonth.of(2022, 1)
        val mai = YearMonth.of(2022, 5)
        val juli = YearMonth.of(2022, 7)
        val august = YearMonth.of(2022, 8)
        val september = YearMonth.of(2022, 9)
        val november = YearMonth.of(2022, 11)
        val desember = YearMonth.of(2022, 12)
        val barnUUID = UUID.randomUUID()

        val utgiftsperiode1 = UtgiftsperiodeDto(januar, juli, barn = listOf(barnUUID), utgifter = TEN)
        val utgiftsperiode2 = UtgiftsperiodeDto(august, desember, barn = listOf(barnUUID), utgifter = ONE)

        val kontantStøtteperiodeJanuar = PeriodeMedBeløpDto(årMånedFra = mai, årMånedTil = september, beløp = TEN)
        val tilleggsstønadPeriodeDto =
                PeriodeMedBeløpDto(årMånedFra = juli, årMånedTil = november, beløp = TEN)

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode1,
                                                                                               utgiftsperiode2),
                                                                      kontantstøttePerioder = listOf(kontantStøtteperiodeJanuar),
                                                                      tilleggsstønadsperioder = listOf(tilleggsstønadPeriodeDto))
        assertThat(beregnYtelseBarnetilsyn).hasSize(6)
    }

    @Test
    fun `Skal lage 9 perioder når tre forskjellige kontantstøtteperioder og tilleggsstønadsperioder i en 12 mnd periode`() {
        val januar = YearMonth.of(2022, 1)
        val mars = YearMonth.of(2022, 3)
        val april = YearMonth.of(2022, 4)
        val juli = YearMonth.of(2022, 7)
        val august = YearMonth.of(2022, 8)
        val desember = YearMonth.of(2022, 12)
        val barnUUID = UUID.randomUUID()

        val utgiftsperiode1 = UtgiftsperiodeDto(januar, mars, barn = listOf(barnUUID), utgifter = TEN)
        val utgiftsperiode2 = UtgiftsperiodeDto(april, juli, barn = listOf(barnUUID), utgifter = ONE)
        val utgiftsperiode3 = UtgiftsperiodeDto(august, desember, barn = listOf(barnUUID), utgifter = TEN + TEN)

        val kontantStøtteperiodeJanuar = PeriodeMedBeløpDto(årMånedFra = januar, årMånedTil = januar, beløp = TEN)
        val kontantStøtteperiodeApril = PeriodeMedBeløpDto(årMånedFra = april, årMånedTil = april, beløp = TEN)
        val kontantStøtteperiodeAugust = PeriodeMedBeløpDto(årMånedFra = august, årMånedTil = august, beløp = TEN)

        val tilleggsstønadsperiodeMars =
                PeriodeMedBeløpDto(årMånedFra = mars, årMånedTil = mars, TEN)
        val tilleggsstønadsperiodeJuli =
                PeriodeMedBeløpDto(årMånedFra = juli, årMånedTil = juli, TEN)
        val tilleggsstønadsperiodeDesember =
                PeriodeMedBeløpDto(årMånedFra = desember, årMånedTil = desember, TEN)

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode1,
                                                                                               utgiftsperiode2,
                                                                                               utgiftsperiode3),
                                                                      kontantstøttePerioder = listOf(kontantStøtteperiodeApril,
                                                                                                     kontantStøtteperiodeJanuar,
                                                                                                     kontantStøtteperiodeAugust),
                                                                      tilleggsstønadsperioder = listOf(
                                                                              tilleggsstønadsperiodeDesember,
                                                                              tilleggsstønadsperiodeJuli,
                                                                              tilleggsstønadsperiodeMars))

        assertThat(beregnYtelseBarnetilsyn).hasSize(9)
    }

    @Test
    fun `Skal lage 2 perioder når vi går over to satser over to år når man overskyter maksbeløp`() {
        val januar = YearMonth.of(2021, 1)
        val desember = YearMonth.of(2022, 12)

        val forventetBeløp2021 = BigDecimal(4195)
        val forventetBeløp2022 = BigDecimal(4250)

        val utgiftsperiode =
                UtgiftsperiodeDto(januar, desember, barn = listOf(UUID.randomUUID()), utgifter = BigDecimal(1000000.0))
        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode),
                                                                      kontantstøttePerioder = listOf(),
                                                                      tilleggsstønadsperioder = listOf())
                .sortedBy { it.periode.fradato }
        assertThat(beregnYtelseBarnetilsyn).hasSize(2)
        assertThat(beregnYtelseBarnetilsyn.first().beløp).isLessThan(beregnYtelseBarnetilsyn.last().beløp)
        assertThat(beregnYtelseBarnetilsyn.first().beløp).isEqualTo(forventetBeløp2021)
        assertThat(beregnYtelseBarnetilsyn.last().beløp).isEqualTo(forventetBeløp2022)
    }

    @Test
    fun `Skal lage 2 perioder når vi går over to satser over to år med 2 barn når man overskyter maksbeløp`() {
        val januar21 = YearMonth.of(2021, 1)
        val desember21 = YearMonth.of(2021, 12)
        val januar22 = YearMonth.of(2022, 1)
        val desember22 = YearMonth.of(2022, 12)

        val utgiftsperiode21 = UtgiftsperiodeDto(januar21,
                                                 desember21,
                                                 barn = listOf(UUID.randomUUID(), UUID.randomUUID()),
                                                 utgifter = BigDecimal(1000000.0))
        val utgiftsperiode22 = UtgiftsperiodeDto(januar22,
                                                 desember22,
                                                 barn = listOf(UUID.randomUUID(), UUID.randomUUID()),
                                                 utgifter = BigDecimal(1000000.0))
        val beregnYtelseBarnetilsyn =
                service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode21, utgiftsperiode22),
                                                kontantstøttePerioder = listOf(),
                                                tilleggsstønadsperioder = listOf())
        assertThat(beregnYtelseBarnetilsyn).hasSize(2)
    }

    @Test
    fun `Skal lage 1 perioder når vi går over to satser over to år med 2 barn når man ikke overskyter maksbeløp`() {
        val januar21 = YearMonth.of(2021, 1)
        val desember21 = YearMonth.of(2021, 12)
        val januar22 = YearMonth.of(2022, 1)
        val desember22 = YearMonth.of(2022, 12)

        val utgiftsperiode21 = UtgiftsperiodeDto(januar21,
                                                 desember21,
                                                 barn = listOf(UUID.randomUUID(), UUID.randomUUID()),
                                                 utgifter = BigDecimal(1.0))
        val utgiftsperiode22 = UtgiftsperiodeDto(januar22,
                                                 desember22,
                                                 barn = listOf(UUID.randomUUID(), UUID.randomUUID()),
                                                 utgifter = BigDecimal(1.0))
        val beregnYtelseBarnetilsyn =
                service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode21, utgiftsperiode22),
                                                kontantstøttePerioder = listOf(),
                                                tilleggsstønadsperioder = listOf())
        assertThat(beregnYtelseBarnetilsyn).hasSize(1)
    }

    @Test
    fun `Skal lage 2 perioder når vi går over to satser over to år med 1 til 2 barn når man ikke overskyter maksbeløp`() {
        val januar21 = YearMonth.of(2021, 1)
        val desember21 = YearMonth.of(2021, 12)
        val januar22 = YearMonth.of(2022, 1)
        val desember22 = YearMonth.of(2022, 12)

        val utgiftsperiode21 =
                UtgiftsperiodeDto(januar21, desember21, barn = listOf(UUID.randomUUID()), utgifter = BigDecimal(1.0))
        val utgiftsperiode22 = UtgiftsperiodeDto(januar22,
                                                 desember22,
                                                 barn = listOf(UUID.randomUUID(), UUID.randomUUID()),
                                                 utgifter = BigDecimal(1.0))
        val beregnYtelseBarnetilsyn =
                service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode21, utgiftsperiode22),
                                                kontantstøttePerioder = listOf(),
                                                tilleggsstønadsperioder = listOf())
        assertThat(beregnYtelseBarnetilsyn).hasSize(2)
    }

    @Test
    fun `Skal lage 2 perioder når vi går over to satser over to år med 1 til 2 barn når man overskyter maksbeløp`() {
        val januar21 = YearMonth.of(2021, 1)
        val desember21 = YearMonth.of(2021, 12)
        val januar22 = YearMonth.of(2022, 1)
        val desember22 = YearMonth.of(2022, 12)

        val utgiftsperiode21 =
                UtgiftsperiodeDto(januar21, desember21, barn = listOf(UUID.randomUUID()), utgifter = BigDecimal(100000.0))
        val utgiftsperiode22 = UtgiftsperiodeDto(januar22,
                                                 desember22,
                                                 barn = listOf(UUID.randomUUID(), UUID.randomUUID()),
                                                 utgifter = BigDecimal(100000.0))
        val beregnYtelseBarnetilsyn =
                service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode21, utgiftsperiode22),
                                                kontantstøttePerioder = listOf(),
                                                tilleggsstønadsperioder = listOf())
        assertThat(beregnYtelseBarnetilsyn).hasSize(2)
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

        val resultat = beløpsperioder.mergeSammenhengendePerioder()

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

        val resultat = beløpsperioder.mergeSammenhengendePerioder()

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
        assertThat(beløpsperioder.mergeSammenhengendePerioder()).hasSize(2)

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

        assertThat(beløpsperioder.mergeSammenhengendePerioder()).hasSize(2)
    }

    private fun lagBeløpsperiode(fraDato: LocalDate,
                                 tilDato: LocalDate,
                                 beløp: BigDecimal = BigDecimal(100)): BeløpsperiodeBarnetilsynDto {
        return BeløpsperiodeBarnetilsynDto(periode = Periode(fraDato, tilDato),
                                           beløp = beløp,
                                           beregningsgrunnlag = BeregningsgrunnlagBarnetilsynDto(utgifter = ZERO,
                                                                                                 kontantstøttebeløp = ZERO,
                                                                                                 tilleggsstønadsbeløp = ZERO,
                                                                                                 antallBarn = 1))
    }

}