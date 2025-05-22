package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.beregning.skolepenger.SkolepengerMaksbeløp.maksbeløp
import no.nav.familie.ef.sak.felles.util.Skoleår
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype.HØGSKOLE_UNIVERSITET
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype.VIDEREGÅENDE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Year
import java.time.YearMonth

internal class SkolepengerMaksbeløpTest {
    @Nested
    inner class MaksBeløpForUniversitet {
        @Test
        internal fun `skal returnere maksbeløp for universitet - 2019`() {
            assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(2019))).isEqualTo(65_326)
        }

        @Test
        internal fun `skal returnere maksbeløp for universitet - 2020`() {
            assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(2020))).isEqualTo(66_604)
        }

        @Test
        internal fun `skal returnere maksbeløp for universitet - 2021`() {
            assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(2021))).isEqualTo(68_136)
        }

        @Test
        internal fun `skal returnere maksbeløp for universitet - 2022`() {
            assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(2022))).isEqualTo(69_500)
        }

        @Test
        internal fun `skal returnere maksbeløp for universitet - 2023`() {
            assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(2023))).isEqualTo(74_366)
        }

        @Test
        internal fun `skal returnere maksbeløp for universitet - 2024`() {
            assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(2024))).isEqualTo(77_192)
        }

        @Test
        internal fun `skal returnere maksbeløp for universitet - 2025`() {
            assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(2025))).isEqualTo(79_432)
        }
    }

    @Nested
    inner class MaksBeløpForVideregående {
        @Test
        internal fun `skal returnere maksbeløp for videregående - 2019`() {
            assertThat(maksbeløpForÅr(VIDEREGÅENDE, Year.of(2019))).isEqualTo(27_276)
        }

        @Test
        internal fun `skal returnere maksbeløp for videregående - 2020`() {
            assertThat(maksbeløpForÅr(VIDEREGÅENDE, Year.of(2020))).isEqualTo(27_794)
        }

        @Test
        internal fun `skal returnere maksbeløp for videregående - 2021`() {
            assertThat(maksbeløpForÅr(VIDEREGÅENDE, Year.of(2021))).isEqualTo(28_433)
        }

        @Test
        internal fun `skal returnere maksbeløp for videregående - 2022`() {
            assertThat(maksbeløpForÅr(VIDEREGÅENDE, Year.of(2022))).isEqualTo(29_002)
        }

        @Test
        internal fun `skal returnere maksbeløp for videregående - 2023`() {
            assertThat(maksbeløpForÅr(VIDEREGÅENDE, Year.of(2023))).isEqualTo(31_033)
        }

        @Test
        internal fun `skal returnere maksbeløp for videregående - 2024`() {
            assertThat(maksbeløpForÅr(VIDEREGÅENDE, Year.of(2024))).isEqualTo(32_211)
        }

        @Test
        internal fun `skal returnere maksbeløp for videregående - 2025`() {
            assertThat(maksbeløpForÅr(VIDEREGÅENDE, Year.of(2025))).isEqualTo(33_145)
        }
    }

    @Test
    internal fun `det skal finnes makssats for hvert år - makssats må oppdateres innen juni`() {
        val sisteRegistrerteÅr = SkolepengerMaksbeløp.hentSisteÅrRegistrertMaksbeløpHøyskole()
        val åretsÅrEllerFjoråret =
            if (sisteRegistrerteÅr == Year.now() || YearMonth.now().month.value >= 6) {
                Year.now()
            } else {
                Year.now().minusYears(1)
            }

        IntRange(2019, åretsÅrEllerFjoråret.value).forEach {
            maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(it))
            maksbeløpForÅr(VIDEREGÅENDE, Year.of(it))
        }
    }

    @Test
    internal fun `maksbeløp for skoleår som ikke er definert kaster exception`() {
        assertThatThrownBy { assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(2018))) }
            .isInstanceOf(ApiFeil::class.java)
            .withFailMessage("Finner ikke maksbeløp for studietype=HØGSKOLE_UNIVERSITET skoleår=18/19")
        assertThatThrownBy { assertThat(maksbeløpForÅr(VIDEREGÅENDE, Year.of(2018))) }
            .isInstanceOf(ApiFeil::class.java)
            .withFailMessage("Finner ikke maksbeløp for studietype=VIDEREGÅENDE skoleår=18/19")
    }

    @Test
    internal fun `Siste registrerte maksbeløp-år skal være samme for høyskole og videregående`() {
        val årHøyskole = SkolepengerMaksbeløp.hentSisteÅrRegistrertMaksbeløpHøyskole()
        val årVideregående = SkolepengerMaksbeløp.hentSisteÅrRegistrertMaksbeløpVideregående()
        assertThat(årHøyskole).isEqualTo(årVideregående)
    }

    @Test
    internal fun `Må holde maksbeløp skolepenger oppdatert innen juni`() {
        val sisteRegistrerteÅr = SkolepengerMaksbeløp.hentSisteÅrRegistrertMaksbeløpHøyskole()
        if (YearMonth.now().month.value < 6) {
            val ifjor = Year.now().minusYears(1)
            assertThat(sisteRegistrerteÅr).isGreaterThanOrEqualTo(ifjor)
        } else {
            assertThat(sisteRegistrerteÅr).isEqualTo(Year.now()).withFailMessage("Makssats for skolepenger burde være vedtatt for år ${Year.now()} - legg denne inn i SkolepengerMaksbeløp.kt og i ef-sak-frontend (skoleår.ts)")
        }
    }

    /**
     * Disse testene må oppdateres med år når man legger inn nytt maksbeløp for neste år
     * * [årOm2År] må oppdateres
     * * Beløp må oppdateres
     */
    @Nested
    inner class MaksBeløpEtÅrFremITiden {
        private val årOm2År = Year.now().plusYears(2)

        @Test
        internal fun `Skolepenger skal returnere maksbeløp for siste høyskoleår når man ber om året etter`() {
            val år = SkolepengerMaksbeløp.hentSisteÅrRegistrertMaksbeløpHøyskole()
            val åretEtterSiste = år.plusYears(1)
            assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, år)).isEqualTo(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, åretEtterSiste))
        }

        @Test
        internal fun `Skolepenger skal returnere maksbeløp for siste videregåendeår når man ber om året etter`() {
            val år = SkolepengerMaksbeløp.hentSisteÅrRegistrertMaksbeløpVideregående()
            val åretEtterSiste = år.plusYears(1)
            assertThat(maksbeløpForÅr(VIDEREGÅENDE, år)).isEqualTo(maksbeløpForÅr(VIDEREGÅENDE, åretEtterSiste))
        }

        @Test
        internal fun `maksbeløp for skoleår 2 år frem i tiden kaster exception`() {
            assertThatThrownBy { assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, årOm2År)) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Finner ikke maksbeløp for studietype=HØGSKOLE_UNIVERSITET")
            assertThatThrownBy { assertThat(maksbeløpForÅr(VIDEREGÅENDE, årOm2År)) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Finner ikke maksbeløp for studietype=VIDEREGÅENDE")
        }
    }

    private fun maksbeløpForÅr(
        studietype: SkolepengerStudietype,
        skoleår: Year,
    ): Int = maksbeløp(studietype, Skoleår(skoleår))
}
