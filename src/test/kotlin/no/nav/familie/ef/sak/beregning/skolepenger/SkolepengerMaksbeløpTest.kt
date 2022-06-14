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

internal class SkolepengerMaksbeløpTest {

    @Test
    internal fun `maksbeløp for universitet`() {
        assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(2021))).isEqualTo(68_136)
    }

    @Test
    internal fun `maksbeløp for videregående`() {
        assertThat(maksbeløpForÅr(VIDEREGÅENDE, Year.of(2021))).isEqualTo(28_433)
    }

    @Test
    internal fun `finnes beløp for hvert år`() {
        IntRange(2020, 2022).forEach {
            maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(it))
            maksbeløpForÅr(VIDEREGÅENDE, Year.of(it))
        }
    }

    @Test
    internal fun `maksbeløp for skoleår som ikke er definiert kaster exception`() {
        assertThatThrownBy { assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(2019))) }
            .isInstanceOf(ApiFeil::class.java)
            .withFailMessage("Finner ikke maksbeløp for studietype=HØGSKOLE_UNIVERSITET skoleår=19/20")
        assertThatThrownBy { assertThat(maksbeløpForÅr(VIDEREGÅENDE, Year.of(2019))) }
            .isInstanceOf(ApiFeil::class.java)
            .withFailMessage("Finner ikke maksbeløp for studietype=VIDEREGÅENDE skoleår=19/20")
    }

    /**
     * Disse testene må oppdateres med år når man legger inn nytt maksbeløp for neste år
     * * [ÅR_OM_2_ÅR] må oppdateres
     * * Beløp må oppdateres
     */
    @Nested
    inner class MaksBeløpEtÅrFremITiden {

        private val ÅR_OM_2_ÅR = Year.of(2024)

        @Test
        internal fun `maksbeløp for universitet et år frem i tiden skal returnere forrige år sitt verdi`() {
            assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, Year.of(2023))).isEqualTo(69_500)
        }

        @Test
        internal fun `maksbeløp for videregående et år frem i tiden skal returnere forrige år sitt verdi`() {
            assertThat(maksbeløpForÅr(VIDEREGÅENDE, Year.of(2023))).isEqualTo(29_002)
        }

        @Test
        internal fun `maksbeløp for skoleår 2 år frem i tiden kaster exception`() {
            assertThatThrownBy { assertThat(maksbeløpForÅr(HØGSKOLE_UNIVERSITET, ÅR_OM_2_ÅR)) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Finner ikke maksbeløp for studietype=HØGSKOLE_UNIVERSITET")
            assertThatThrownBy { assertThat(maksbeløpForÅr(VIDEREGÅENDE, ÅR_OM_2_ÅR)) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Finner ikke maksbeløp for studietype=VIDEREGÅENDE")
        }
    }

    private fun maksbeløpForÅr(studietype: SkolepengerStudietype, skoleår: Year): Int {
        return maksbeløp(studietype, Skoleår(skoleår))
    }
}
