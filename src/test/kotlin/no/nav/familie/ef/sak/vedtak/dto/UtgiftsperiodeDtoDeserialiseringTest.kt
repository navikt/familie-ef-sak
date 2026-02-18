package no.nav.familie.ef.sak.vedtak.dto

import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.jsonMapper
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class UtgiftsperiodeDtoDeserialiseringTest {
    @Test
    fun `skal kunne deserialisere UtgiftsperiodeDto uten utgifter-felt`() {
        val json =
            """
            {
              "årMånedFra": "2021-01",
              "årMånedTil": "2021-12",
              "barn": [],
              "sanksjonsårsak": null,
              "periodetype": "OPPHØR",
              "aktivitetstype": null
            }
            """.trimIndent()

        val dto = jsonMapper.readValue<UtgiftsperiodeDto>(json)

        assertThat(dto.utgifter).isEqualTo(0)
        assertThat(dto.periode.fom).isEqualTo(YearMonth.of(2021, 1))
        assertThat(dto.periode.tom).isEqualTo(YearMonth.of(2021, 12))
        assertThat(dto.periodetype).isEqualTo(PeriodetypeBarnetilsyn.OPPHØR)
    }

    @Test
    fun `skal kunne deserialisere UtgiftsperiodeDto med utgifter-felt`() {
        val json =
            """
            {
              "årMånedFra": "2021-01",
              "årMånedTil": "2021-12",
              "barn": ["4ab497b2-a19c-4415-bf00-556ff8e9ce86"],
              "utgifter": 2000,
              "sanksjonsårsak": null,
              "periodetype": "ORDINÆR",
              "aktivitetstype": "I_ARBEID"
            }
            """.trimIndent()

        val dto = jsonMapper.readValue<UtgiftsperiodeDto>(json)

        assertThat(dto.utgifter).isEqualTo(2000)
        assertThat(dto.periodetype).isEqualTo(PeriodetypeBarnetilsyn.ORDINÆR)
    }
}
