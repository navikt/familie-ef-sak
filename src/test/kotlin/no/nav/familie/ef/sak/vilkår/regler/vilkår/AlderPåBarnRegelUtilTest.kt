package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.regler.vilkår.AlderPåBarnRegelUtil.harFullførtFjerdetrinn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AlderPåBarnRegelUtilTest {

    fun datoer(år: Int) = listOf(
        LocalDate.of(år, 1, 1),
        LocalDate.of(år, 5, 1),
        LocalDate.of(år, 10, 1),
    )

    val fødselsdatoer = datoer(2013) // liste over ulike fødselsdatoer 2013

    @Test
    fun `barn født i 2013 har ikke avsluttet 4e trinn før 2023, uavhengig føselsdato det året`() {
        IntRange(2013, 2022).forEach { år ->
            val kjøredatoer = datoer(år)
            fødselsdatoer.forEach { fødselsdato ->
                kjøredatoer.forEach { kjøredato ->
                    assertThat(harFullførtFjerdetrinn(fødselsdato, kjøredato)).isFalse
                }
            }
        }
    }

    @Test
    fun `barn født i 2013 har ikke avsluttet 4e trinn før juni 2023`() {
        fødselsdatoer.forEach { fødselsdato ->
            assertThat(harFullførtFjerdetrinn(fødselsdato, LocalDate.of(2023, 1, 1))).isFalse
            assertThat(harFullførtFjerdetrinn(fødselsdato, LocalDate.of(2023, 3, 1))).isFalse
            assertThat(harFullførtFjerdetrinn(fødselsdato, LocalDate.of(2023, 5, 1))).isFalse
        }
    }

    @Test
    fun `barn født i 2013 har avsluttet 4e trinn etter juni 2023`() {
        fødselsdatoer.forEach { fødselsdato ->
            assertThat(harFullførtFjerdetrinn(fødselsdato, LocalDate.of(2023, 6, 1))).isTrue
            assertThat(harFullførtFjerdetrinn(fødselsdato, LocalDate.of(2023, 8, 1))).isTrue
            assertThat(harFullførtFjerdetrinn(fødselsdato, LocalDate.of(2023, 10, 1))).isTrue
            assertThat(harFullførtFjerdetrinn(fødselsdato, LocalDate.of(2024, 1, 1))).isTrue
            assertThat(harFullførtFjerdetrinn(fødselsdato, LocalDate.of(2024, 5, 1))).isTrue
        }
    }
}
