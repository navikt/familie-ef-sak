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
                Sanksjonsårsak.SAGT_OPP_STILLING
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
                    null
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
                    Sanksjonsårsak.SAGT_OPP_STILLING
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
                    Sanksjonsårsak.SAGT_OPP_STILLING
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
                    Sanksjonsårsak.SAGT_OPP_STILLING
                )
            }.hasMessageContaining("Sanksjon må være en måned")

            assertThatThrownBy {
                Vedtaksperiode(
                    Månedsperiode(januar, januarNesteÅr),
                    AktivitetType.IKKE_AKTIVITETSPLIKT,
                    VedtaksperiodeType.SANKSJON,
                    Sanksjonsårsak.SAGT_OPP_STILLING
                )
            }.hasMessageContaining("Sanksjon må være en måned")
        }
    }

    @Nested
    inner class Barnetilsyn {

        @Test
        internal fun `gyldig sanksjon`() {
            val periode = Barnetilsynperiode(
                periode,
                1,
                emptyList(),
                true,
                Sanksjonsårsak.SAGT_OPP_STILLING
            )
            assertThat(periode).isNotNull
        }

        @Test
        internal fun `skal sette erMidlertidigOpphør til true hvis sanksjon`() {
            assertThatThrownBy {
                Barnetilsynperiode(
                    periode,
                    1,
                    emptyList(),
                    null,
                    Sanksjonsårsak.SAGT_OPP_STILLING
                )
            }.hasMessageContaining("MidlerTidigOpphør må settes hvis sanksjon")
            assertThatThrownBy {
                Barnetilsynperiode(
                    periode,
                    1,
                    emptyList(),
                    false,
                    Sanksjonsårsak.SAGT_OPP_STILLING
                )
            }.hasMessageContaining("MidlerTidigOpphør må settes hvis sanksjon")
        }
    }
}