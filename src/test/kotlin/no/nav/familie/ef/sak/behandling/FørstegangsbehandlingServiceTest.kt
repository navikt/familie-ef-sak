package no.nav.familie.ef.sak.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.dto.FørstegangsbehandlingDto
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

internal class FørstegangsbehandlingServiceTest {

    val featureToggleService = mockk<FeatureToggleService>()
    val førstegangsbehandlingService = FørstegangsbehandlingService(
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        featureToggleService
    )

    @BeforeEach
    internal fun setUp() {
        every { featureToggleService.isEnabled(any()) } returns true
    }

    @Test
    internal fun `skal feile hvis behandlingsårsak er ugyldig for førstegangsbehandlinger`() {
        val ugyldigeÅrsaker =
            BehandlingÅrsak.values().filter { it != BehandlingÅrsak.PAPIRSØKNAD && it != BehandlingÅrsak.NYE_OPPLYSNINGER }
        ugyldigeÅrsaker.forEach {
            assertThrows<Feil> {
                førstegangsbehandlingService.opprettFørstegangsbehandling(
                    UUID.randomUUID(),
                    FørstegangsbehandlingDto(
                        behandlingsårsak = it,
                        kravMottatt = LocalDate.now().minusDays(10),
                        barn = emptyList()
                    )
                )
            }
        }
    }
}
