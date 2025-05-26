package no.nav.familie.ef.sak.behandling

import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.dto.FørstegangsbehandlingDto
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

internal class FørstegangsbehandlingServiceTest {
    val førstegangsbehandlingService =
        FørstegangsbehandlingService(
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
        )

    @Test
    internal fun `skal feile hvis behandlingsårsak er ugyldig for førstegangsbehandlinger`() {
        val ugyldigeÅrsaker =
            BehandlingÅrsak.values().filter { it != BehandlingÅrsak.PAPIRSØKNAD && it != BehandlingÅrsak.NYE_OPPLYSNINGER && it != BehandlingÅrsak.MANUELT_OPPRETTET }
        ugyldigeÅrsaker.forEach {
            assertThrows<Feil> {
                førstegangsbehandlingService.opprettFørstegangsbehandling(
                    UUID.randomUUID(),
                    FørstegangsbehandlingDto(
                        behandlingsårsak = it,
                        kravMottatt = LocalDate.now().minusDays(10),
                        barn = emptyList(),
                    ),
                )
            }
        }
    }
}
