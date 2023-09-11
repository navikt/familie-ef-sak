package no.nav.familie.ef.sak.vilkår.dto

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GrunnlagsdataPeriodeHistorikkDtoTest {

    @Test
    internal fun `skal regne ut antall måneder`() {
        val grunnlagsdataPeriodeHistorikkDto = GrunnlagsdataPeriodeHistorikkDto(
            periodeType = "Test",
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            harPeriodeUtenUtbetaling = false,
        )

        val grunnlagsdataPeriodeHistorikkDto2 = GrunnlagsdataPeriodeHistorikkDto(
            periodeType = "Test",
            fom = LocalDate.of(2020, 1, 1),
            tom = LocalDate.of(2021, 12, 31),
            harPeriodeUtenUtbetaling = true,
        )

        Assertions.assertThat(grunnlagsdataPeriodeHistorikkDto.antMnd).isEqualTo(1)
        Assertions.assertThat(grunnlagsdataPeriodeHistorikkDto2.antMnd).isEqualTo(24)
    }
}
