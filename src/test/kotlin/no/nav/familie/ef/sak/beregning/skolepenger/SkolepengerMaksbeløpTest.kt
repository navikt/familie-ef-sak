package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.beregning.skolepenger.SkolepengerMaksbeløp.maksbeløp
import no.nav.familie.ef.sak.felles.util.Skoleår
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype.HØGSKOLE_UNIVERSITET
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype.VIDEREGÅENDE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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

    private fun maksbeløpForÅr(studietype: SkolepengerStudietype, skoleår: Year): Int {
        return maksbeløp(studietype, Skoleår(skoleår))
    }
}
