package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Opplysningskilde
import no.nav.familie.ef.sak.behandling.domain.Revurderingsårsak
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.behandling.ÅrsakRevurderingsRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.revurderingsinformasjon
import no.nav.familie.ef.sak.repository.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ÅrsakRevurderingStegTest {

    private val årsakRevurderingsRepository = mockk<ÅrsakRevurderingsRepository>()

    private val behandlingService = mockk<BehandlingService>()

    private val steg = ÅrsakRevurderingSteg(årsakRevurderingsRepository, behandlingService)

    private val saksbehandling = saksbehandling()
    private val stønadstype = saksbehandling.stønadstype

    private val gyldigÅrsak = Revurderingsårsak.values().first { it.gjelderStønadstyper.contains(stønadstype) }
    private val ugyldigÅrsak = Revurderingsårsak.values().first { !it.gjelderStønadstyper.contains(stønadstype) }

    private val årsakRevurderingSlot = slot<ÅrsakRevurdering>()

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.oppdaterKravMottatt(saksbehandling.id, any()) } returns behandling()

        justRun { årsakRevurderingsRepository.deleteById(saksbehandling.id) }
        every { årsakRevurderingsRepository.insert(capture(årsakRevurderingSlot)) } answers { firstArg() }
    }

    private val gyldigRevurderingsinformasjon = revurderingsinformasjon()

    @Test
    internal fun `lagrer kravMottatt og årsakRevurdering når dataen er gyldig`() {
        val data = gyldigRevurderingsinformasjon
        val nesteSteg = steg.utførOgReturnerNesteSteg(saksbehandling, data)

        assertThat(nesteSteg).isEqualTo(saksbehandling.steg)
        val lagretObjekt = årsakRevurderingSlot.captured
        assertThat(lagretObjekt.årsak).isEqualTo(data.årsakRevurdering?.årsak)
        assertThat(lagretObjekt.opplysningskilde).isEqualTo(data.årsakRevurdering?.opplysningskilde)
        assertThat(lagretObjekt.beskrivelse).isEqualTo(data.årsakRevurdering?.beskrivelse)

        verify {
            behandlingService.oppdaterKravMottatt(saksbehandling.id, data.kravMottatt!!)
        }
    }

    @Test
    internal fun `skal returnere saksbehandlingens nåværende steg for å ikke endre steg`() {
        val behandling = saksbehandling.copy(steg = StegType.BEREGNE_YTELSE)
        val data = gyldigRevurderingsinformasjon

        val nesteSteg = steg.utførOgReturnerNesteSteg(behandling, data)

        assertThat(nesteSteg).isEqualTo(StegType.BEREGNE_YTELSE)
    }

    @Nested
    inner class validering {

        @Test
        internal fun `feiler hvis kravMottatt mangler`() {
            assertThatThrownBy { utførOgReturnerNesteSteg(RevurderingsinformasjonDto()) }
                .hasMessage("Mangler kravMottatt")
        }

        @Test
        internal fun `feiler hvis årsakRevurdering mangler`() {
            assertThatThrownBy { utførOgReturnerNesteSteg(RevurderingsinformasjonDto(LocalDate.now())) }
                .hasMessage("Mangler årsakRevurdering")
        }

        @Test
        internal fun `feiler hvis man man sender inn en årsak som ikke er gyldig for gitt stønadstype`() {
            val dto = RevurderingsinformasjonDto(
                LocalDate.now(),
                ÅrsakRevurderingDto(Opplysningskilde.BESKJED_ANNEN_ENHET, ugyldigÅrsak, null),
            )
            assertThatThrownBy { utførOgReturnerNesteSteg(dto) }
                .hasMessage("Årsak er ikke gyldig for stønadstype")
        }

        @Test
        internal fun `må angi beskrivelse når årsak=ANNET`() {
            val årsak = Revurderingsårsak.ANNET
            val dto = RevurderingsinformasjonDto(
                LocalDate.now(),
                ÅrsakRevurderingDto(Opplysningskilde.BESKJED_ANNEN_ENHET, årsak, "   ")
            )
            assertThatThrownBy { utførOgReturnerNesteSteg(dto) }
                .hasMessage("Må ha med beskrivelse når årsak er annet")
        }

        @Test
        internal fun `skal ikke sende med beskrivelse når årsak er annet enn ANNET`() {
            val dto = RevurderingsinformasjonDto(
                LocalDate.now(),
                ÅrsakRevurderingDto(Opplysningskilde.BESKJED_ANNEN_ENHET, gyldigÅrsak, "asd")
            )
            assertThatThrownBy { utførOgReturnerNesteSteg(dto) }
                .hasMessage("Kan ikke ha med beskrivelse når årsak er noe annet en annet")
        }
    }

    fun utførOgReturnerNesteSteg(dto: RevurderingsinformasjonDto) {
        steg.utførOgReturnerNesteSteg(saksbehandling, dto)
    }
}