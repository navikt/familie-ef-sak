package no.nav.familie.ef.sak.mapper

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.StatsborgerskapMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Statsborgerskap
import no.nav.familie.ef.sak.service.KodeverkService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class StatsborgerskapMapperTest {

    private val kodeverkService: KodeverkService = mockk()
    private val statsborgerskapMapper = StatsborgerskapMapper(kodeverkService)

    @Test
    internal fun `skal mappe NOR ISO3 landkode til Norge`() {
        every { kodeverkService.hentLand("NOR", any()) } returns "Norge"
        val mappedStatsborgerskap =
                statsborgerskapMapper.map(listOf(Statsborgerskap("NOR", LocalDate.MIN, LocalDate.MAX))).first()
        assertThat(mappedStatsborgerskap.land).isEqualTo("Norge")
        assertThat(mappedStatsborgerskap.gyldigFraOgMedDato).isEqualTo(LocalDate.MIN)
        assertThat(mappedStatsborgerskap.gyldigTilOgMedDato).isEqualTo(LocalDate.MAX)
    }

    @Test
    internal fun `skal returnere landkoden fra pdl hvis vi ikke finner mapping fra kodeverk`() {
        every { kodeverkService.hentLand(any(), any()) } returns null
        val mappedStatsborgerskap = statsborgerskapMapper.map(listOf(Statsborgerskap("NOR", null, null))).first()
        assertThat(mappedStatsborgerskap.land).isEqualTo("NOR")
    }


}