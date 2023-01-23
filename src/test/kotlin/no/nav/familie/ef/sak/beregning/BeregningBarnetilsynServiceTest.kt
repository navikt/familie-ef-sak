package no.nav.familie.ef.sak.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynService
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningsgrunnlagBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.mergeSammenhengendePerioder
import no.nav.familie.ef.sak.beregning.barnetilsyn.roundUp
import no.nav.familie.ef.sak.beregning.barnetilsyn.split
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.barnetilsynperiodeDto
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class BeregningBarnetilsynServiceTest {

    val featureToggleService = mockk<FeatureToggleService>()
    val service: BeregningBarnetilsynService = BeregningBarnetilsynService(featureToggleService)

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

    @BeforeEach
    fun setup() {
        every { featureToggleService.isEnabled(any()) } returns false
    }

    @Nested
    inner class BeregningBarnetilsynValidering {

        @Test
        internal fun `Skal kaste feil når vi sender inn kontantstøtteperiode-fradrag før kontantstøtte ble innført`() {
            val utgiftsperiode = listeMedEnUtgiftsperiode()
            val perioderMedTidligDato = listeMedEnPeriodeMedBeløp(fra = YearMonth.of(2020, Month.FEBRUARY))
            val kontantstøtteperiodeStarterForTidlig = assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = utgiftsperiode,
                    kontantstøttePerioder = perioderMedTidligDato,
                    tilleggsstønadsperioder = listOf()
                )
            }
            assertThat(kontantstøtteperiodeStarterForTidlig.message).isEqualTo("Fradrag for innvilget kontantstøtte trår i kraft: 2020-03")
        }

        @Test
        internal fun `Skal kaste feil når vi sender inn ufornuftige beløp `() {
            val utgiftsperiode = listeMedEnUtgiftsperiode()
            val utgiftsperiodeMedHøytBeløp = listeMedEnUtgiftsperiode(beløp = 50000)
            val utgiftsperiodeMedNegativtBeløp = listeMedEnUtgiftsperiode(beløp = -1)
            val periodeMedHøytBeløp = listeMedEnPeriodeMedBeløp(beløp = 50000)
            val periodeMedNegativtBeløp = listeMedEnPeriodeMedBeløp(beløp = -1)

            val negativUtgiftsfeil = assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = utgiftsperiodeMedNegativtBeløp,
                    kontantstøttePerioder = listOf(),
                    tilleggsstønadsperioder = listOf()
                )
            }
            assertThat(negativUtgiftsfeil.message).isEqualTo("Utgifter kan ikke være mindre enn 0")

            val forHøyUtgiftsfeil = assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = utgiftsperiodeMedHøytBeløp,
                    kontantstøttePerioder = listOf(),
                    tilleggsstønadsperioder = listOf()
                )
            }
            assertThat(forHøyUtgiftsfeil.message).isEqualTo("Utgifter på mer enn 40000 støttes ikke")

            assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = utgiftsperiode,
                    kontantstøttePerioder = listOf(),
                    tilleggsstønadsperioder = periodeMedHøytBeløp
                )
            }

            assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = utgiftsperiode,
                    kontantstøttePerioder = periodeMedHøytBeløp,
                    tilleggsstønadsperioder = listOf()
                )
            }

            assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = utgiftsperiode,
                    kontantstøttePerioder = listOf(),
                    tilleggsstønadsperioder = periodeMedNegativtBeløp
                )
            }

            assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = utgiftsperiode,
                    kontantstøttePerioder = periodeMedNegativtBeløp,
                    tilleggsstønadsperioder = listOf()
                )
            }
        }

        @Test
        fun `Skal kaste feil når vi sender inn urelevant kontantstøtteperiode`() {
            val januarTilApril = listeMedEnUtgiftsperiode(januar2022, april2022)
            val urelevant = listeMedEnPeriodeMedBeløp(juli2022, desember2022)
            assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = januarTilApril,
                    kontantstøttePerioder = urelevant,
                    tilleggsstønadsperioder = listOf()
                )
            }
        }

        @Test
        fun `Skal kaste feil når vi sender inn urelevant tilleggsstønadsperiode`() {
            val januarTilApril = listeMedEnUtgiftsperiode(januar2022, april2022)
            val urelevant = listeMedEnPeriodeMedBeløp(juli2022, desember2022)
            assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = januarTilApril,
                    kontantstøttePerioder = listOf(),
                    tilleggsstønadsperioder = urelevant
                )
            }
        }

        @Test
        fun `Skal kaste feil hvis utgiftsperioder er overlappende`() {
            val periode1 = barnetilsynperiodeDto(januar2022, april2022, listOf(UUID.randomUUID()), 10)
            val periode2 = barnetilsynperiodeDto(mars2022, juli2022, listOf(UUID.randomUUID()), 10)
            val perioder = listOf(periode1, periode2)

            val feil = assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = perioder,
                    kontantstøttePerioder = listOf(),
                    tilleggsstønadsperioder = listOf()
                )
            }
            assertThat(feil.message).contains("Utgiftsperioder")
        }

        @Test
        fun `Skal kaste brukerfeil hvis kontantstøtteperioder er overlappende`() {
            val overlappende = listeMedEnPeriodeMedBeløp(januar2022, april2022) + listeMedEnPeriodeMedBeløp(april2022, april2022)

            val feil = assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = listeMedEnUtgiftsperiode(januar2022, april2022),
                    kontantstøttePerioder = overlappende,
                    tilleggsstønadsperioder = listOf()
                )
            }
            assertThat(feil.message).contains("Kontantstøtteperioder [Månedsperiode(fom=2022-01, tom=2022-04), Månedsperiode(fom=2022-04, tom=2022-04)] overlapper")
        }

        @Test
        fun `Skal kaste brukerfeil hvis tilleggsstønadperioder er overlappende`() {
            val overlappendePerioder =
                listeMedEnPeriodeMedBeløp(april2022, april2022) + listeMedEnPeriodeMedBeløp(januar2022, april2022)
            val feil = assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = listeMedEnUtgiftsperiode(januar2022, april2022),
                    kontantstøttePerioder = listOf(),
                    tilleggsstønadsperioder = overlappendePerioder
                )
            }
            assertThat(feil.message).contains("Tilleggsstønadsperioder")
        }

        @Test
        internal fun `Skal kaste feil dersom aktivitetstype ikke er valgt og periodetype ikke er opphør eller sanksjon`() {
            val utgiftsperioder = listeMedEnUtgiftsperiode(aktivitetstype = AktivitetstypeBarnetilsyn.I_ARBEID, periodetype = PeriodetypeBarnetilsyn.ORDINÆR) +
                listeMedEnUtgiftsperiode(fra = mars2022, til = april2022, periodetype = PeriodetypeBarnetilsyn.ORDINÆR, aktivitetstype = null)
            val ugyldigUtgiftsperiode = assertThrows<ApiFeil> {
                service.beregnYtelseBarnetilsyn(
                    utgiftsperioder = utgiftsperioder,
                    kontantstøttePerioder = listOf(),
                    tilleggsstønadsperioder = listOf()
                )
            }
            assertThat(ugyldigUtgiftsperiode.message).isEqualTo("Utgiftsperioder $utgiftsperioder mangler en eller flere aktivitetstyper")
        }

        @Test
        internal fun `Migrering skal ikke kaste feil dersom aktivitetstype mangler`() {
            val utgiftsperioder = listeMedEnUtgiftsperiode(
                fra = mars2022,
                til = april2022,
                periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                aktivitetstype = null
            )
            val perioder = service.beregnYtelseBarnetilsyn(
                utgiftsperioder = utgiftsperioder,
                kontantstøttePerioder = listOf(),
                tilleggsstønadsperioder = listOf(),
                erMigrering = true
            )
            assertThat(perioder).hasSize(1)
        }
    }

    @Test
    fun `Skal lage tre perioder når tre forskjellige beløp i en 12mnd periode`() {
        val barnUUID = UUID.randomUUID()
        val periode1 = barnetilsynperiodeDto(januar2022, mars2022, listOf(barnUUID), 10)
        val periode2 = barnetilsynperiodeDto(april2022, juli2022, listOf(barnUUID), 1)
        val periode3 = barnetilsynperiodeDto(august2022, desember2022, listOf(barnUUID), 20)
        val perioder = listOf(periode1, periode2, periode3)

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(
            utgiftsperioder = perioder,
            kontantstøttePerioder = listOf(),
            tilleggsstønadsperioder = listOf()
        )
        assertThat(beregnYtelseBarnetilsyn).hasSize(3)
    }

    @Test
    fun `Skal lage seks perioder med tre forskjellige kontantstøtteperioder i en 12 mnd periode`() {
        val barnUUID = UUID.randomUUID()
        val periode1 = barnetilsynperiodeDto(januar2022, mars2022, listOf(barnUUID), 10)
        val periode2 = barnetilsynperiodeDto(april2022, juli2022, listOf(barnUUID), 1)
        val periode3 = barnetilsynperiodeDto(august2022, desember2022, listOf(barnUUID), 20)
        val perioder = listOf(periode1, periode2, periode3)

        val kontantStøtteperiodeJanuar = PeriodeMedBeløpDto(
            årMånedFra = januar2022,
            årMånedTil = januar2022,
            periode = Månedsperiode(januar2022, januar2022),
            beløp = 10
        )
        val kontantStøtteperiodeApril = PeriodeMedBeløpDto(
            årMånedFra = april2022,
            årMånedTil = april2022,
            periode = Månedsperiode(april2022, april2022),
            beløp = 10
        )
        val kontantStøtteperiodeAugust = PeriodeMedBeløpDto(
            årMånedFra = august2022,
            årMånedTil = august2022,
            periode = Månedsperiode(august2022, august2022),
            beløp = 10
        )

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(
            utgiftsperioder = perioder,
            kontantstøttePerioder = listOf(
                kontantStøtteperiodeJanuar,
                kontantStøtteperiodeApril,
                kontantStøtteperiodeAugust
            ),
            tilleggsstønadsperioder = listOf()
        )

        assertThat(beregnYtelseBarnetilsyn).hasSize(6)
    }

    @Test
    fun `Skal lage fire perioder med to utgiftsperioder og med en overlappende kontantstøtteperiode`() {
        val barnUUID = UUID.randomUUID()
        val periode1 = barnetilsynperiodeDto(januar2022, juli2022, listOf(barnUUID), 10)
        val periode2 = barnetilsynperiodeDto(august2022, desember2022, listOf(barnUUID), 1)
        val perioder = listOf(periode1, periode2)

        val kontantStøtteperiodeJanuar =
            PeriodeMedBeløpDto(årMånedFra = juli2022, årMånedTil = august2022, Månedsperiode(juli2022, august2022), beløp = 10)

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(
            utgiftsperioder = perioder,
            kontantstøttePerioder = listOf(kontantStøtteperiodeJanuar),
            tilleggsstønadsperioder = listOf()
        )

        assertThat(beregnYtelseBarnetilsyn).hasSize(4)
    }

    @Test
    fun `Skal lage seks perioder med to utgiftsperioder og en overlappende kontantstøtte og tilleggsstønadsperiodemed`() {
        val barnUUID = UUID.randomUUID()
        val periode1 = barnetilsynperiodeDto(januar2022, juli2022, listOf(barnUUID), 10)
        val periode2 = barnetilsynperiodeDto(august2022, desember2022, listOf(barnUUID), 1)
        val perioder = listOf(periode1, periode2)

        val kontantStøtteperiodeJanuar = PeriodeMedBeløpDto(
            årMånedFra = mai2022,
            årMånedTil = september2022,
            periode = Månedsperiode(mai2022, september2022),
            beløp = 10
        )
        val tilleggsstønadPeriodeDto =
            PeriodeMedBeløpDto(
                årMånedFra = juli2022,
                årMånedTil = november2022,
                periode = Månedsperiode(juli2022, november2022),
                beløp = 10
            )

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(
            utgiftsperioder = perioder,
            kontantstøttePerioder = listOf(kontantStøtteperiodeJanuar),
            tilleggsstønadsperioder = listOf(tilleggsstønadPeriodeDto)
        )
        assertThat(beregnYtelseBarnetilsyn).hasSize(6)
    }

    @Test
    fun `Skal lage 9 perioder når tre forskjellige kontantstøtteperioder og tilleggsstønadsperioder i en 12 mnd periode`() {
        val barnUUID = UUID.randomUUID()
        val periode1 = barnetilsynperiodeDto(januar2022, mars2022, listOf(barnUUID), 10)
        val periode2 = barnetilsynperiodeDto(april2022, juli2022, listOf(barnUUID), 1)
        val periode3 = barnetilsynperiodeDto(august2022, desember2022, listOf(barnUUID), 20)
        val perioder = listOf(periode1, periode2, periode3)

        val kontantStøtteperiodeJanuar = PeriodeMedBeløpDto(
            årMånedFra = januar2022,
            årMånedTil = januar2022,
            Månedsperiode(januar2022, januar2022),
            beløp = 10
        )
        val kontantStøtteperiodeApril =
            PeriodeMedBeløpDto(årMånedFra = april2022, årMånedTil = april2022, Månedsperiode(april2022, april2022), beløp = 10)
        val kontantStøtteperiodeAugust = PeriodeMedBeløpDto(
            årMånedFra = august2022,
            årMånedTil = august2022,
            Månedsperiode(august2022, august2022),
            beløp = 10
        )

        val tilleggsstønadsperiodeMars =
            PeriodeMedBeløpDto(årMånedFra = mars2022, årMånedTil = mars2022, Månedsperiode(mars2022, mars2022), 10)
        val tilleggsstønadsperiodeJuli =
            PeriodeMedBeløpDto(årMånedFra = juli2022, årMånedTil = juli2022, Månedsperiode(juli2022, juli2022), 10)
        val tilleggsstønadsperiodeDesember =
            PeriodeMedBeløpDto(
                årMånedFra = desember2022,
                årMånedTil = desember2022,
                Månedsperiode(desember2022, desember2022),
                10
            )

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(
            utgiftsperioder = perioder,
            kontantstøttePerioder = listOf(
                kontantStøtteperiodeApril,
                kontantStøtteperiodeJanuar,
                kontantStøtteperiodeAugust
            ),
            tilleggsstønadsperioder = listOf(
                tilleggsstønadsperiodeDesember,
                tilleggsstønadsperiodeJuli,
                tilleggsstønadsperiodeMars
            )
        )

        assertThat(beregnYtelseBarnetilsyn).hasSize(9)
    }

    @Test
    fun `Skal lage 2 perioder når vi går over to satser over to år når man overskyter maksbeløp`() {
        val forventetBeløp2021 = 4195
        val forventetBeløp2022 = 4250
        val periode = barnetilsynperiodeDto(januar2021, desember2022, listOf(UUID.randomUUID()), 39000)

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(
            utgiftsperioder = listOf(periode),
            kontantstøttePerioder = listOf(),
            tilleggsstønadsperioder = listOf()
        )
            .sortedBy { it.periode.fom }
        assertThat(beregnYtelseBarnetilsyn).hasSize(2)
        assertThat(beregnYtelseBarnetilsyn.first().beløp).isLessThan(beregnYtelseBarnetilsyn.last().beløp)
        assertThat(beregnYtelseBarnetilsyn.first().beløp).isEqualTo(forventetBeløp2021)
        assertThat(beregnYtelseBarnetilsyn.last().beløp).isEqualTo(forventetBeløp2022)
    }

    @Test
    fun `Skal lage 2 perioder når vi går over to satser over to år med 2 barn når man overskyter maksbeløp`() {
        val barn = List(2) { UUID.randomUUID() }
        val periode1 = barnetilsynperiodeDto(januar2021, desember2021, barn, 39000)
        val periode2 = barnetilsynperiodeDto(januar2022, desember2022, barn, 39000)
        val perioder = listOf(periode1, periode2)

        val beregnYtelseBarnetilsyn =
            service.beregnYtelseBarnetilsyn(
                utgiftsperioder = perioder,
                kontantstøttePerioder = listOf(),
                tilleggsstønadsperioder = listOf()
            )
        assertThat(beregnYtelseBarnetilsyn).hasSize(2)
    }

    @Test
    fun `Skal lage 1 perioder når vi går over to satser over to år med 2 barn når man ikke overskyter maksbeløp`() {
        val barn = List(2) { UUID.randomUUID() }
        val periode1 = barnetilsynperiodeDto(januar2021, desember2021, barn, 1)
        val periode2 = barnetilsynperiodeDto(januar2022, desember2022, barn, 1)
        val perioder = listOf(periode1, periode2)

        val beregnYtelseBarnetilsyn =
            service.beregnYtelseBarnetilsyn(
                utgiftsperioder = perioder,
                kontantstøttePerioder = listOf(),
                tilleggsstønadsperioder = listOf()
            )
        assertThat(beregnYtelseBarnetilsyn).hasSize(1)
    }

    @Test
    fun `Skal lage 2 perioder når vi går over to satser over to år med 1 til 2 barn når man ikke overskyter maksbeløp`() {
        val barnPeriode1 = listOf(UUID.randomUUID())
        val barnPeriode2 = barnPeriode1 + listOf(UUID.randomUUID())
        val periode1 = barnetilsynperiodeDto(januar2021, desember2021, barnPeriode1, 1)
        val periode2 = barnetilsynperiodeDto(januar2022, desember2022, barnPeriode2, 1)
        val perioder = listOf(periode1, periode2)

        val beregnYtelseBarnetilsyn =
            service.beregnYtelseBarnetilsyn(
                utgiftsperioder = perioder,
                kontantstøttePerioder = listOf(),
                tilleggsstønadsperioder = listOf()
            )
        assertThat(beregnYtelseBarnetilsyn).hasSize(2)
    }

    @Test
    fun `Skal lage 2 perioder når vi går over to satser over to år med 1 til 2 barn når man overskyter maksbeløp`() {
        val barnPeriode1 = listOf(UUID.randomUUID())
        val barnPeriode2 = barnPeriode1 + listOf(UUID.randomUUID())
        val periode1 = barnetilsynperiodeDto(januar2021, desember2021, barnPeriode1, 39000)
        val periode2 = barnetilsynperiodeDto(januar2022, desember2022, barnPeriode2, 1)
        val perioder = listOf(periode1, periode2)

        val beregnYtelseBarnetilsyn =
            service.beregnYtelseBarnetilsyn(
                utgiftsperioder = perioder,
                kontantstøttePerioder = listOf(),
                tilleggsstønadsperioder = listOf()
            )
        assertThat(beregnYtelseBarnetilsyn).hasSize(2)
    }

    @Test
    fun `split en utgiftsperiode som varer fra januar til desember i 12 mnd`() {
        val periode1 = barnetilsynperiodeDto(januar2022, desember2022, listOf(UUID.randomUUID()), 10)
        val resultat = periode1.split()
        assertThat(resultat).hasSize(12)
    }

    @Test
    fun `merge to påfølgende måneder med like beløp, forvent én periode`() {
        val forventetFraDato = januar2000.atDay(1)
        val forventetTilDato = februar2000.atEndOfMonth()
        val beløpsperioder = listOf(
            lagBeløpsperiode(forventetFraDato, januar2000.atEndOfMonth()),
            lagBeløpsperiode(februar2000.atDay(1), forventetTilDato)
        )

        val resultat = beløpsperioder.mergeSammenhengendePerioder()

        assertThat(resultat).hasSize(1)
        assertThat(resultat.first().periode.fomDato).isEqualTo(forventetFraDato)
        assertThat(resultat.first().periode.tomDato).isEqualTo(forventetTilDato)
    }

    @Test
    fun `merge to påfølgende måneder med forskjellige beløp, forvent to perioder`() {
        val forventetFraDato = januar2000.atDay(1)
        val forventetTilDato = februar2000.atEndOfMonth()
        val beløpsperioder = listOf(
            lagBeløpsperiode(
                fraDato = forventetFraDato,
                tilDato = januar2000.atEndOfMonth(),
                beløp = BigDecimal(100)
            ),
            lagBeløpsperiode(
                fraDato = februar2000.atDay(1),
                tilDato = forventetTilDato,
                beløp = BigDecimal(200)
            )
        )

        val resultat = beløpsperioder.mergeSammenhengendePerioder()

        assertThat(resultat).hasSize(2)
        assertThat(resultat.first().periode.fomDato).isEqualTo(forventetFraDato)
        assertThat(resultat.last().periode.tomDato).isEqualTo(forventetTilDato)
    }

    @Test
    fun `merge to påfølgende måneder med hull ifm fradatoer`() {
        val forventetFraDato = januar2000.atDay(1)
        val forventetTilDato = mars2000.atEndOfMonth()
        val beløpsperioder = listOf(
            lagBeløpsperiode(
                fraDato = forventetFraDato,
                tilDato = januar2000.atEndOfMonth(),
                beløp = BigDecimal(100)
            ),
            lagBeløpsperiode(
                fraDato = mars2000.atDay(1),
                tilDato = forventetTilDato,
                beløp = BigDecimal(100)
            )
        )
        assertThat(beløpsperioder.mergeSammenhengendePerioder()).hasSize(2)
    }

    @Test
    fun `merge to påfølgende måneder med hull ifm fradatoer og forskjellige beløp`() {
        val forventetFraDato = januar2000.atDay(1)
        val forventetTilDato = mars2000.atEndOfMonth()
        val beløpsperioder = listOf(
            lagBeløpsperiode(
                fraDato = forventetFraDato,
                tilDato = januar2000.atEndOfMonth(),
                beløp = BigDecimal(100)
            ),
            lagBeløpsperiode(
                fraDato = mars2000.atDay(1),
                tilDato = forventetTilDato,
                beløp = BigDecimal(200)
            )
        )

        assertThat(beløpsperioder.mergeSammenhengendePerioder()).hasSize(2)
    }

    @Test
    fun `Skal lage tre utgiftsperioder når etterfølgende perioder differ på periodetype`() {
        val barnUUID = listOf(UUID.randomUUID())
        val utgiftsperioder =
            listeMedEnUtgiftsperiode(fra = januar2022, til = mars2022, periodetype = PeriodetypeBarnetilsyn.ORDINÆR, aktivitetstype = AktivitetstypeBarnetilsyn.I_ARBEID, barn = barnUUID) +
                listeMedEnUtgiftsperiode(fra = april2022, til = juli2022, periodetype = PeriodetypeBarnetilsyn.OPPHØR, aktivitetstype = null, barn = barnUUID) +
                listeMedEnUtgiftsperiode(fra = august2022, til = desember2022, periodetype = PeriodetypeBarnetilsyn.ORDINÆR, aktivitetstype = AktivitetstypeBarnetilsyn.I_ARBEID, barn = barnUUID)

        val beregnYtelseBarnetilsyn = service.beregnYtelseBarnetilsyn(
            utgiftsperioder = utgiftsperioder,
            kontantstøttePerioder = listOf(),
            tilleggsstønadsperioder = listOf()
        )

        assertThat(beregnYtelseBarnetilsyn).hasSize(2)
    }

    private fun lagBeløpsperiode(
        fraDato: LocalDate,
        tilDato: LocalDate,
        beløp: BigDecimal = BigDecimal(100)
    ): BeløpsperiodeBarnetilsynDto {
        return BeløpsperiodeBarnetilsynDto(
            periode = Månedsperiode(fraDato, tilDato),
            beløp = beløp.roundUp().toInt(),
            beløpFørFratrekkOgSatsjustering = beløp.roundUp().toInt(),
            sats = 6284,
            beregningsgrunnlag = BeregningsgrunnlagBarnetilsynDto(
                utgifter = ZERO,
                kontantstøttebeløp = ZERO,
                tilleggsstønadsbeløp = ZERO,
                antallBarn = 1,
                barn = emptyList()
            ),
            periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
            aktivitetstype = AktivitetstypeBarnetilsyn.I_ARBEID
        )
    }

    private fun listeMedEnPeriodeMedBeløp(
        fra: YearMonth = januar2022,
        til: YearMonth = februar2022,
        beløp: Int = 10
    ): List<PeriodeMedBeløpDto> {
        return listOf(
            PeriodeMedBeløpDto(
                årMånedFra = fra,
                årMånedTil = til,
                periode = Månedsperiode(fra, til),
                beløp = beløp
            )
        )
    }

    private fun listeMedEnUtgiftsperiode(
        fra: YearMonth = januar2022,
        til: YearMonth = februar2022,
        beløp: Int = 10,
        periodetype: PeriodetypeBarnetilsyn = PeriodetypeBarnetilsyn.ORDINÆR,
        aktivitetstype: AktivitetstypeBarnetilsyn? = AktivitetstypeBarnetilsyn.I_ARBEID,
        barn: List<UUID>? = null
    ): List<UtgiftsperiodeDto> {
        return listOf(
            UtgiftsperiodeDto(
                årMånedFra = fra,
                årMånedTil = til,
                periode = Månedsperiode(fra, til),
                barn = barn ?: listOf(UUID.randomUUID()),
                utgifter = beløp,
                sanksjonsårsak = null,
                periodetype = periodetype,
                aktivitetstype = aktivitetstype
            )
        )
    }
}
