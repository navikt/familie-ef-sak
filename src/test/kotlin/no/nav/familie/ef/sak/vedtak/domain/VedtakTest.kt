package no.nav.familie.ef.sak.vedtak.domain

import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class VedtakTest {

    private val januar = YearMonth.of(2022, 1)
    private val febaruar = YearMonth.of(2022, 2)
    private val januarNesteÅr = YearMonth.of(2023, 1)

    private val periode = Månedsperiode(januar)

    @Nested
    inner class Overgangsstønad {

        @Test
        internal fun `gyldig sanksjon`() {
            val periode = Vedtaksperiode(
                periode,
                AktivitetType.IKKE_AKTIVITETSPLIKT,
                VedtaksperiodeType.SANKSJON,
                Sanksjonsårsak.SAGT_OPP_STILLING,
            )
            assertThat(periode).isNotNull
        }

        @Test
        internal fun `må ha sanksjonsårsak hvis sanksjon`() {
            assertThatThrownBy {
                Vedtaksperiode(
                    periode,
                    AktivitetType.IKKE_AKTIVITETSPLIKT,
                    VedtaksperiodeType.SANKSJON,
                    null,
                )
            }.hasMessageContaining("Ugyldig kombinasjon av sanksjon")
        }

        @Test
        internal fun `kan ikke ha aktivitet hvis sanksjon`() {
            assertThatThrownBy {
                Vedtaksperiode(
                    periode,
                    AktivitetType.FORSØRGER_I_ARBEID,
                    VedtaksperiodeType.SANKSJON,
                    Sanksjonsårsak.SAGT_OPP_STILLING,
                )
            }.hasMessageContaining("Ugyldig kombinasjon av sanksjon")
        }

        @Test
        internal fun `må være type sanksjon hvis sanksjonsårsak finnes`() {
            assertThatThrownBy {
                Vedtaksperiode(
                    periode,
                    AktivitetType.FORSØRGER_I_ARBEID,
                    VedtaksperiodeType.PERIODE_FØR_FØDSEL,
                    Sanksjonsårsak.SAGT_OPP_STILLING,
                )
            }.hasMessageContaining("Ugyldig kombinasjon av sanksjon")
        }

        @Test
        internal fun `periode må ikke være over 1 mnd`() {
            assertThatThrownBy {
                Vedtaksperiode(
                    Månedsperiode(januar, febaruar),
                    AktivitetType.IKKE_AKTIVITETSPLIKT,
                    VedtaksperiodeType.SANKSJON,
                    Sanksjonsårsak.SAGT_OPP_STILLING,
                )
            }.hasMessageContaining("Sanksjon må være en måned")

            assertThatThrownBy {
                Vedtaksperiode(
                    Månedsperiode(januar, januarNesteÅr),
                    AktivitetType.IKKE_AKTIVITETSPLIKT,
                    VedtaksperiodeType.SANKSJON,
                    Sanksjonsårsak.SAGT_OPP_STILLING,
                )
            }.hasMessageContaining("Sanksjon må være en måned")
        }
    }

    @Nested
    inner class Barnetilsyn {

        @Test
        internal fun `ordinær periode kan ikke inneholde sanksjonsårsak`() {
            assertThatThrownBy {
                Barnetilsynperiode(
                    periode,
                    1,
                    emptyList(),
                    Sanksjonsårsak.SAGT_OPP_STILLING,
                    PeriodetypeBarnetilsyn.OPPHØR,
                )
            }.hasMessageContaining("Ugyldig kombinasjon av sanksjon periodeType=OPPHØR sanksjonsårsak=SAGT_OPP_STILLING")
        }

        @Test
        internal fun `opphørsperiode kan ikke inneholde sanksjonsårsak`() {
            assertThatThrownBy {
                Barnetilsynperiode(
                    periode,
                    1,
                    emptyList(),
                    Sanksjonsårsak.SAGT_OPP_STILLING,
                    PeriodetypeBarnetilsyn.OPPHØR,
                )
            }.hasMessageContaining("Ugyldig kombinasjon av sanksjon periodeType=OPPHØR sanksjonsårsak=SAGT_OPP_STILLING")
        }

        @Test
        internal fun `gyldig sanksjon`() {
            val periode = Barnetilsynperiode(
                periode,
                1,
                emptyList(),
                Sanksjonsårsak.SAGT_OPP_STILLING,
                PeriodetypeBarnetilsyn.SANKSJON_1_MND,
            )
            assertThat(periode).isNotNull
        }

        @Test
        internal fun `sanksjon kan ikke ha periode over 1 måned`() {
            assertThatThrownBy {
                Barnetilsynperiode(
                    Månedsperiode(januar, januarNesteÅr),
                    1,
                    emptyList(),
                    Sanksjonsårsak.SAGT_OPP_STILLING,
                    PeriodetypeBarnetilsyn.SANKSJON_1_MND,
                )
            }.hasMessageContaining("Sanksjon må være en måned, fra=2022-01-01 til=2023-01-31")
        }

        @Test
        internal fun `sanksjon må sette sanksjonsårsak`() {
            assertThatThrownBy {
                Barnetilsynperiode(
                    periode,
                    1,
                    emptyList(),
                    null,
                    PeriodetypeBarnetilsyn.SANKSJON_1_MND,
                )
            }.hasMessageContaining("Ugyldig kombinasjon av sanksjon periodeType=SANKSJON_1_MND sanksjonsårsak=null")
        }
    }
}
