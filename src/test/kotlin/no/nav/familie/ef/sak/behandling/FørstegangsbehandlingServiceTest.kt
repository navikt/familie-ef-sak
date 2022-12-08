package no.nav.familie.ef.sak.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.dto.FørstegangsbehandlingDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

internal class FørstegangsbehandlingServiceTest {

    val fagsakService = mockk<FagsakService>()
    val featureToggleService = mockk<FeatureToggleService>()
    val førstegangsbehandlingService = FørstegangsbehandlingService(
        BehandlingService(mockk(), mockk(), mockk(), mockk(), mockk()),
        fagsakService,
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
        every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak()
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

    @Test
    internal fun `skal feile hvis krav mottatt er frem i tid`() {
        assertThrows<ApiFeil> {
            førstegangsbehandlingService.opprettFørstegangsbehandling(
                UUID.randomUUID(),
                FørstegangsbehandlingDto(
                    behandlingsårsak = BehandlingÅrsak.PAPIRSØKNAD,
                    kravMottatt = LocalDate.now().plusDays(2),
                    barn = emptyList()
                )
            )
        }
    }
}
