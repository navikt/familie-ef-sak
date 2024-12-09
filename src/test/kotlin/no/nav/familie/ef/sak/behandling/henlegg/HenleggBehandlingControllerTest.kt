package no.nav.familie.ef.sak.behandling.henlegg

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class HenleggBehandlingControllerTest {
    val taskService = mockk<TaskService>(relaxed = true)

    private val henleggBehandlingController: HenleggBehandlingController =
        HenleggBehandlingController(
            behandlingService = mockk(),
            fagsakService = mockk(relaxed = true),
            henleggService = mockk(relaxed = true),
            tilgangService = mockk(relaxed = true),
            featureToggleService = mockk(),
            taskService = taskService,
        )

    @BeforeEach fun setUp() {
        mockBrukerContext("", azp_name = "prod-gcp:teamfamilie:familie-ef-personhendelse")
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Test internal fun `Skal kaste feil hvis feilregistrert og send brev er true`() {
        val exception =
            assertThrows<Feil> {
                henleggBehandlingController.henleggBehandling(UUID.randomUUID(), HenlagtDto(HenlagtÅrsak.FEILREGISTRERT, true))
            }
        assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis type er ulik trukket tilbake")
    }

    @Test internal fun `Skal lage send brev task hvis send brev er true og henlagårsak er trukket`() {
        henleggBehandlingController.henleggBehandling(UUID.randomUUID(), HenlagtDto(HenlagtÅrsak.TRUKKET_TILBAKE, true))
        verify(exactly = 1) { taskService.save(any()) }
    }
}
