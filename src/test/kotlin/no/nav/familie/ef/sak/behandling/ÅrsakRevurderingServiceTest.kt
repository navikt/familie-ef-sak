package no.nav.familie.ef.sak.behandling

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.saksbehandling
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.UUID

internal class ÅrsakRevurderingServiceTest {

    private val behandlingService = mockk<BehandlingService>()

    private val årsakRevurderingsRepository = mockk<ÅrsakRevurderingsRepository>()

    private val service = ÅrsakRevurderingService(behandlingService, årsakRevurderingsRepository)

    private val førstegångsbehandling = saksbehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING)
    private val revurdering = saksbehandling(type = BehandlingType.REVURDERING)
    private val revurderingMedKravMottatt =
        saksbehandling(type = BehandlingType.REVURDERING, kravMottatt = LocalDate.now())

    @BeforeEach
    internal fun setUp() {
        listOf(førstegångsbehandling, revurdering, revurderingMedKravMottatt).forEach {
            every { behandlingService.hentSaksbehandling(it.id) } returns it
        }
        every { behandlingService.oppdaterKravMottatt(any(), any()) } returns behandling()
        justRun { årsakRevurderingsRepository.deleteById(any()) }
    }

    @Nested
    inner class validerHarGyldigRevurderingsinformasjon {

        @Test
        internal fun `skal ikke validere hvis det er førstegångsbehandling`() {
            service.validerHarGyldigRevurderingsinformasjon(førstegångsbehandling)

            verify(exactly = 0) { årsakRevurderingsRepository.findByIdOrNull(førstegångsbehandling.id) }
        }

        @Test
        internal fun `skal kaste feil hvis mottattDato mangler`() {
            every { årsakRevurderingsRepository.findByIdOrNull(revurdering.id) } returns null
            assertThatThrownBy {
                service.validerHarGyldigRevurderingsinformasjon(revurdering)
            }.hasMessageContaining("Behandlingen mangler årsak til revurdering.")
        }

        @Test
        internal fun `skal kaste feil hvis årsak revurdering mangler`() {
            every { årsakRevurderingsRepository.findByIdOrNull(revurderingMedKravMottatt.id) } returns null
            assertThatThrownBy {
                service.validerHarGyldigRevurderingsinformasjon(revurderingMedKravMottatt)
            }.hasMessageContaining("Behandlingen mangler årsak til revurdering.")
        }
    }

    @Test
    internal fun `slett skal fjerne mottattdato og årsak revurdering`() {
        val behandlingId = UUID.randomUUID()
        service.slettRevurderingsinformasjon(behandlingId)

        verify(exactly = 1) {
            årsakRevurderingsRepository.deleteById(behandlingId)
            behandlingService.oppdaterKravMottatt(behandlingId, null)
        }
    }
}