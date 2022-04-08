package no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.TEN
import java.math.BigDecimal.ZERO
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class BeregningBarnetilsynServiceTest {

    val service: BeregningBarnetilsynService = BeregningBarnetilsynService()

    val januar2022 = YearMonth.of(2022, 1)
    val februar2022 = YearMonth.of(2022, 2)
    val mars2022 = YearMonth.of(2022, 3)
    val april2022 = YearMonth.of(2022, 4)
    val mai2022 = YearMonth.of(2022, 5)
    val juli2022 = YearMonth.of(2022, 7)
    val august2022 = YearMonth.of(2022, 8)
    val september2022 = YearMonth.of(2022, 9)
    val november2022 = YearMonth.of(2022, 11)
    val desember2022 = YearMonth.of(2022, 12)

    val januar2021 = YearMonth.of(2021, 1)
    val desember2021 = YearMonth.of(2021, 12)

    val januar2000 = YearMonth.of(2000, 1)
    val februar2000 = YearMonth.of(2000, 2)
    val mars2000 = YearMonth.of(2000, 3)


    @Test
    fun `Skal kaste feil når vi sender inn urelevant kontantstøtteperiode`() {
        val januarTilApril = listeMedEnUtgiftsperiode(januar2022, april2022)
        val urelevant = enPeriodeMedBeløp(juli2022, desember2022)
        assertThrows<ApiFeil> {
            service.beregnYtelseBarnetilsyn(utgiftsperioder = januarTilApril,
                                            kontantstøttePerioder = urelevant,
                                            tilleggsstønadsperioder = listOf())
        }
    }

    @Test
    fun `Skal kaste feil når vi sender inn urelevant tilleggsstønadsperiode`() {
        val januarTilApril = listeMedEnUtgiftsperiode(januar2022, april2022)
        val urelevant = enPeriodeMedBeløp(juli2022, desember2022)
        assertThrows<ApiFeil> {
            service.beregnYtelseBarnetilsyn(utgiftsperioder = januarTilApril,
                                            kontantstøttePerioder = listOf(),
                                            tilleggsstønadsperioder = urelevant)
        }
    }

    @Test
    fun `Skal kaste feil hvis utgiftsperioder er overlappende`() {
        val utgiftsperiode1 = UtgiftsperiodeDto(januar2022, april2022, barn = listOf(UUID.randomUUID()), utgifter = TEN)
        val utgiftsperiode2 = UtgiftsperiodeDto(mars2022, juli2022, barn = listOf(UUID.randomUUID()), utgifter = TEN)
        val feil = assertThrows<ApiFeil> {
            service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode1, utgiftsperiode2),
                                            kontantstøttePerioder = listOf(),
                                            tilleggsstønadsperioder = listOf())
        }
        assertThat(feil.message).contains("Utgiftsperioder")
    }

    @Test
    fun `Skal kaste brukerfeil hvis kontantstøtteperioder er overlappende`() {
        val overlappende = enPeriodeMedBeløp(januar2022, april2022) + enPeriodeMedBeløp(april2022, april2022)

        val feil = assertThrows<ApiFeil> {
            service.beregnYtelseBarnetilsyn(utgiftsperioder = listeMedEnUtgiftsperiode(januar2022, april2022),
                                            kontantstøttePerioder = overlappende,
                                            tilleggsstønadsperioder = listOf())
        }
        assertThat(feil.message).contains("Kontantstøtteperioder [Periode(fradato=2022-01-01, tildato=2022-04-30), Periode(fradato=2022-04-01, tildato=2022-04-30)] overlapper")
    }


    @Test
    fun `Skal kaste brukerfeil hvis tilleggsstønadperioder er overlappende`() {
        val overlappendePerioder = enPeriodeMedBeløp(april2022, april2022) + enPeriodeMedBeløp(januar2022, april2022)
        val feil = assertThrows<ApiFeil> {
            service.beregnYtelseBarnetilsyn(utgiftsperioder = listeMedEnUtgiftsperiode(januar2022, april2022),
                                            kontantstøttePerioder = listOf(),
                                            tilleggsstønadsperioder = overlappendePerioder)
        }
        assertThat(feil.message).contains("Tilleggsstønadsperioder")
    }


    @Test
    fun `Skal lage tre perioder når tre forskjellige beløp i en 12,md periode`() {
        val barnUUID = UUID.randomUUID()
        val utgiftsperiode1 = UtgiftsperiodeDto(januar2022, mars2022, barn = listOf(barnUUID), utgifter = TEN)
        val utgiftsperiode2 = UtgiftsperiodeDto(april2022, juli2022, barn = listOf(barnUUID), utgifter = ONE)
        val utgiftsperiode3 = UtgiftsperiodeDto(august2022, desember2022, barn = listOf(barnUUID), utgifter = TEN + TEN)

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode1,
                                                                                               utgiftsperiode2,
                                                                                               utgiftsperiode3),
                                                                      kontantstøttePerioder = listOf(),
                                                                      tilleggsstønadsperioder = listOf())

        assertThat(beregnYtelseBarnetilsyn).hasSize(3)
    }

    @Test
    fun `Skal lage seks perioder med tre forskjellige kontantstøtteperioder i en 12 mnd periode`() {
        val desember = YearMonth.of(2022, 12)
        val barnUUID = UUID.randomUUID()

        val utgiftsperiode1 = UtgiftsperiodeDto(januar2022, mars2022, barn = listOf(barnUUID), utgifter = TEN)
        val utgiftsperiode2 = UtgiftsperiodeDto(april2022, juli2022, barn = listOf(barnUUID), utgifter = ONE)
        val utgiftsperiode3 = UtgiftsperiodeDto(august2022, desember, barn = listOf(barnUUID), utgifter = TEN + TEN)

        val kontantStøtteperiodeJanuar = PeriodeMedBeløpDto(årMånedFra = januar2022, årMånedTil = januar2022, beløp = 10)
        val kontantStøtteperiodeApril = PeriodeMedBeløpDto(årMånedFra = april2022, årMånedTil = april2022, beløp = 10)
        val kontantStøtteperiodeAugust = PeriodeMedBeløpDto(årMånedFra = august2022, årMånedTil = august2022, beløp = 10)

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

        val barnUUID = UUID.randomUUID()

        val utgiftsperiode1 = UtgiftsperiodeDto(januar2022, juli2022, barn = listOf(barnUUID), utgifter = TEN)
        val utgiftsperiode2 = UtgiftsperiodeDto(august2022, desember2022, barn = listOf(barnUUID), utgifter = ONE)


        val kontantStøtteperiodeJanuar = PeriodeMedBeløpDto(årMånedFra = juli2022, årMånedTil = august2022, beløp = 10)


        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode1,
                                                                                               utgiftsperiode2),
                                                                      kontantstøttePerioder = listOf(kontantStøtteperiodeJanuar),
                                                                      tilleggsstønadsperioder = listOf())

        assertThat(beregnYtelseBarnetilsyn).hasSize(4)
    }

    @Test
    fun `Skal lage seks perioder med to utgiftsperioder og en overlappende kontantstøtte og tilleggsstønadsperiodemed`() {

        val barnUUID = UUID.randomUUID()

        val utgiftsperiode1 = UtgiftsperiodeDto(januar2022, juli2022, barn = listOf(barnUUID), utgifter = TEN)
        val utgiftsperiode2 = UtgiftsperiodeDto(august2022, desember2022, barn = listOf(barnUUID), utgifter = ONE)

        val kontantStøtteperiodeJanuar = PeriodeMedBeløpDto(årMånedFra = mai2022, årMånedTil = september2022, beløp = 10)
        val tilleggsstønadPeriodeDto =
                PeriodeMedBeløpDto(årMånedFra = juli2022, årMånedTil = november2022, beløp = 10)

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(utgiftsperioder = listOf(utgiftsperiode1,
                                                                                               utgiftsperiode2),
                                                                      kontantstøttePerioder = listOf(kontantStøtteperiodeJanuar),
                                                                      tilleggsstønadsperioder = listOf(tilleggsstønadPeriodeDto))
        assertThat(beregnYtelseBarnetilsyn).hasSize(6)
    }

    @Test
    fun `Skal lage 9 perioder når tre forskjellige kontantstøtteperioder og tilleggsstønadsperioder i en 12 mnd periode`() {
        val barnUUID = UUID.randomUUID()

        val utgiftsperiode1 = UtgiftsperiodeDto(januar2022, mars2022, barn = listOf(barnUUID), utgifter = TEN)
        val utgiftsperiode2 = UtgiftsperiodeDto(april2022, juli2022, barn = listOf(barnUUID), utgifter = ONE)
        val utgiftsperiode3 = UtgiftsperiodeDto(august2022, desember2022, barn = listOf(barnUUID), utgifter = TEN + TEN)

        val kontantStøtteperiodeJanuar = PeriodeMedBeløpDto(årMånedFra = januar2022, årMånedTil = januar2022, beløp = 10)
        val kontantStøtteperiodeApril = PeriodeMedBeløpDto(årMånedFra = april2022, årMånedTil = april2022, beløp = 10)
        val kontantStøtteperiodeAugust = PeriodeMedBeløpDto(årMånedFra = august2022, årMånedTil = august2022, beløp = 10)

        val tilleggsstønadsperiodeMars =
                PeriodeMedBeløpDto(årMånedFra = mars2022, årMånedTil = mars2022, 10)
        val tilleggsstønadsperiodeJuli =
                PeriodeMedBeløpDto(årMånedFra = juli2022, årMånedTil = juli2022, 10)
        val tilleggsstønadsperiodeDesember =
                PeriodeMedBeløpDto(årMånedFra = desember2022, årMånedTil = desember2022, 10)

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
        val forventetBeløp2021 = 4195
        val forventetBeløp2022 = 4250

        val utgiftsperiode =
                UtgiftsperiodeDto(januar2021, desember2022, barn = listOf(UUID.randomUUID()), utgifter = BigDecimal(1000000.0))
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
        val utgiftsperiode21 = UtgiftsperiodeDto(januar2021,
                                                 desember2021,
                                                 barn = listOf(UUID.randomUUID(), UUID.randomUUID()),
                                                 utgifter = BigDecimal(1000000.0))
        val utgiftsperiode22 = UtgiftsperiodeDto(januar2022,
                                                 desember2022,
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
        val utgiftsperiode21 = UtgiftsperiodeDto(januar2021,
                                                 desember2021,
                                                 barn = listOf(UUID.randomUUID(), UUID.randomUUID()),
                                                 utgifter = BigDecimal(1.0))
        val utgiftsperiode22 = UtgiftsperiodeDto(januar2022,
                                                 desember2022,
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
        val utgiftsperiode21 =
                UtgiftsperiodeDto(januar2021, desember2021, barn = listOf(UUID.randomUUID()), utgifter = BigDecimal(1.0))
        val utgiftsperiode22 = UtgiftsperiodeDto(januar2022,
                                                 desember2022,
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
        val utgiftsperiode21 =
                UtgiftsperiodeDto(januar2021, desember2021, barn = listOf(UUID.randomUUID()), utgifter = BigDecimal(100000.0))
        val utgiftsperiode22 = UtgiftsperiodeDto(januar2022,
                                                 desember2022,
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
        val utgiftsperiodeDto = UtgiftsperiodeDto(januar2022, desember2022, barn = listOf(), utgifter = TEN)
        val resultat = utgiftsperiodeDto.split()
        assertThat(resultat).hasSize(12)

    }

    @Test
    fun `merge to påfølgende måneder med like beløp, forvent én periode`() {

        val forventetFraDato = januar2000.atDay(1)
        val forventetTilDato = februar2000.atEndOfMonth()
        val beløpsperioder = listOf(lagBeløpsperiode(forventetFraDato, januar2000.atEndOfMonth()),
                                    lagBeløpsperiode(februar2000.atDay(1), forventetTilDato))

        val resultat = beløpsperioder.mergeSammenhengendePerioder()

        assertThat(resultat).hasSize(1)
        assertThat(resultat.first().periode.fradato).isEqualTo(forventetFraDato)
        assertThat(resultat.first().periode.tildato).isEqualTo(forventetTilDato)
    }

    @Test
    fun `merge to påfølgende måneder med forskjellige beløp, forvent to perioder`() {

        val forventetFraDato = januar2000.atDay(1)
        val forventetTilDato = februar2000.atEndOfMonth()
        val beløpsperioder = listOf(lagBeløpsperiode(fraDato = forventetFraDato,
                                                     tilDato = januar2000.atEndOfMonth(),
                                                     beløp = BigDecimal(100)),
                                    lagBeløpsperiode(fraDato = februar2000.atDay(1),
                                                     tilDato = forventetTilDato,
                                                     beløp = BigDecimal(200)))

        val resultat = beløpsperioder.mergeSammenhengendePerioder()

        assertThat(resultat).hasSize(2)
        assertThat(resultat.first().periode.fradato).isEqualTo(forventetFraDato)
        assertThat(resultat.last().periode.tildato).isEqualTo(forventetTilDato)
    }

    @Test
    fun `merge to påfølgende måneder med hull ifm fradatoer, forvent NotImplementedError`() {

        val forventetFraDato = januar2000.atDay(1)
        val forventetTilDato = mars2000.atEndOfMonth()
        val beløpsperioder = listOf(lagBeløpsperiode(fraDato = forventetFraDato,
                                                     tilDato = januar2000.atEndOfMonth(),
                                                     beløp = BigDecimal(100)),
                                    lagBeløpsperiode(fraDato = mars2000.atDay(1),
                                                     tilDato = forventetTilDato,
                                                     beløp = BigDecimal(100)))
        assertThat(beløpsperioder.mergeSammenhengendePerioder()).hasSize(2)

    }

    @Test
    fun `merge to påfølgende måneder med hull ifm fradatoer og forskjellige beløp, forvent NotImplementedError`() {

        val forventetFraDato = januar2000.atDay(1)
        val forventetTilDato = mars2000.atEndOfMonth()
        val beløpsperioder = listOf(lagBeløpsperiode(fraDato = forventetFraDato,
                                                     tilDato = januar2000.atEndOfMonth(),
                                                     beløp = BigDecimal(100)),
                                    lagBeløpsperiode(fraDato = mars2000.atDay(1),
                                                     tilDato = forventetTilDato,
                                                     beløp = BigDecimal(200)))

        assertThat(beløpsperioder.mergeSammenhengendePerioder()).hasSize(2)
    }

    private fun lagBeløpsperiode(fraDato: LocalDate,
                                 tilDato: LocalDate,
                                 beløp: BigDecimal = BigDecimal(100)): BeløpsperiodeBarnetilsynDto {
        return BeløpsperiodeBarnetilsynDto(periode = Periode(fraDato, tilDato),
                                           beløp = beløp.roundUp().toInt(),
                                           beregningsgrunnlag = BeregningsgrunnlagBarnetilsynDto(utgifter = ZERO,
                                                                                                 kontantstøttebeløp = ZERO,
                                                                                                 tilleggsstønadsbeløp = ZERO,
                                                                                                 antallBarn = 1))
    }

    private fun enPeriodeMedBeløp(fra: YearMonth = januar2022, til: YearMonth = februar2022) = listOf(PeriodeMedBeløpDto(
            årMånedFra = fra,
            årMånedTil = til,
            beløp = 10))

    private fun listeMedEnUtgiftsperiode(fra: YearMonth = januar2022, til: YearMonth = februar2022) = listOf(UtgiftsperiodeDto(fra,
                                                                                                                               til,
                                                                                                                               listOf(UUID.randomUUID()),
                                                                                                                               TEN))

}