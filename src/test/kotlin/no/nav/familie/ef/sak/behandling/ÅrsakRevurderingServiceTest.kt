package no.nav.familie.ef.sak.behandling

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingService
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingsRepository
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.felles.Opplysningskilde
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import org.assertj.core.api.Assertions.assertThat
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
    private val tilordnetRessursService = mockk<TilordnetRessursService>()

    private val service = ÅrsakRevurderingService(behandlingService, årsakRevurderingsRepository, tilordnetRessursService)

    private val førstegångsbehandling = saksbehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING)
    private val revurdering = saksbehandling(type = BehandlingType.REVURDERING)
    private val revurderingMedKravMottatt =
        saksbehandling(type = BehandlingType.REVURDERING, kravMottatt = LocalDate.now())

    private val årsakRevurderingSlot = slot<ÅrsakRevurdering>()

    @BeforeEach
    internal fun setUp() {
        listOf(førstegångsbehandling, revurdering, revurderingMedKravMottatt).forEach {
            every { behandlingService.hentSaksbehandling(it.id) } returns it
        }
        every { behandlingService.oppdaterKravMottatt(any(), any()) } returns behandling()

        justRun { årsakRevurderingsRepository.deleteById(any()) }
        every { årsakRevurderingsRepository.insert(capture(årsakRevurderingSlot)) } answers { firstArg() }
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true
    }

    @Nested
    inner class ValiderHarGyldigRevurderingsinformasjon {
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

        /**
         * Hvis denne feiler må man vurdere om den nye [BehandlingÅrsak] skal valideres for at årsak revurdering finnes eller ikke
         * Hvis den ikke skal valideres, som for eks for [BehandlingÅrsak.MIGRERING] så skal den legges inn i
         * årsaker som filtres vekk i [ÅrsakRevurderingService]
         *
         * Uavhengig om den skal med eller ikke må den legges inn i [årsakerSomErTattStillingTil]
         */
        @Test
        internal fun `validerHarGyldigRevurderingsinformasjon har tatt stilling til alle behandlingsårsaker`() {
            val årsakerSomErTattStillingTil =
                setOf(
                    BehandlingÅrsak.KLAGE,
                    BehandlingÅrsak.NYE_OPPLYSNINGER,
                    BehandlingÅrsak.SANKSJON_1_MND,
                    BehandlingÅrsak.SØKNAD,
                    BehandlingÅrsak.MIGRERING,
                    BehandlingÅrsak.G_OMREGNING,
                    BehandlingÅrsak.IVERKSETTE_KA_VEDTAK,
                    BehandlingÅrsak.KORRIGERING_UTEN_BREV,
                    BehandlingÅrsak.PAPIRSØKNAD,
                    BehandlingÅrsak.SATSENDRING,
                    BehandlingÅrsak.MANUELT_OPPRETTET,
                )
            val alleÅrsaker = BehandlingÅrsak.values().toSet()
            assertThat(årsakerSomErTattStillingTil).containsExactlyInAnyOrderElementsOf(alleÅrsaker)
        }
    }

    @Test
    internal fun `oppdaterRevurderingsinformasjon skal slette og oppdatere data`() {
        val kravMottatt = LocalDate.of(2021, 1, 1)
        val årsakRevurdering =
            ÅrsakRevurderingDto(Opplysningskilde.MELDING_MODIA, Revurderingsårsak.ENDRING_INNTEKT, "asd")
        service.oppdaterRevurderingsinformasjon(revurdering, kravMottatt, årsakRevurdering)

        val lagretObjekt = årsakRevurderingSlot.captured
        assertThat(lagretObjekt.behandlingId).isEqualTo(revurdering.id)
        assertThat(lagretObjekt.årsak).isEqualTo(årsakRevurdering.årsak)
        assertThat(lagretObjekt.opplysningskilde).isEqualTo(årsakRevurdering.opplysningskilde)
        assertThat(lagretObjekt.beskrivelse).isEqualTo(årsakRevurdering.beskrivelse)

        verify { årsakRevurderingsRepository.deleteById(revurdering.id) }
        verify { årsakRevurderingsRepository.insert(any()) }
        verify { behandlingService.oppdaterKravMottatt(revurdering.id, kravMottatt) }
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
