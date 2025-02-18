package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingService
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.revurderingsinformasjon
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.kontrakter.ef.felles.Opplysningskilde
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ÅrsakRevurderingStegTest {
    private val årsakRevurderingService = mockk<ÅrsakRevurderingService>()
    private val tilordnetRessursService = mockk<TilordnetRessursService>()

    private val steg = ÅrsakRevurderingSteg(årsakRevurderingService, tilordnetRessursService)

    private val saksbehandling = saksbehandling()
    private val stønadstype = saksbehandling.stønadstype

    private val gyldigÅrsak = Revurderingsårsak.values().first { it.gjelderStønadstyper.contains(stønadstype) }
    private val ugyldigÅrsak = Revurderingsårsak.values().first { !it.gjelderStønadstyper.contains(stønadstype) }

    @BeforeEach
    internal fun setUp() {
        justRun { årsakRevurderingService.oppdaterRevurderingsinformasjon(any(), any(), any()) }
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true
    }

    private val gyldigRevurderingsinformasjon = revurderingsinformasjon()

    @Test
    internal fun `lagrer kravMottatt og årsakRevurdering når dataen er gyldig`() {
        val data = gyldigRevurderingsinformasjon
        val nesteSteg = steg.utførOgReturnerNesteSteg(saksbehandling, data)

        assertThat(nesteSteg).isEqualTo(saksbehandling.steg)

        verify {
            årsakRevurderingService.oppdaterRevurderingsinformasjon(
                saksbehandling,
                data.kravMottatt!!,
                data.årsakRevurdering!!,
            )
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
    inner class Validering {
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
            val dto =
                RevurderingsinformasjonDto(
                    LocalDate.now(),
                    ÅrsakRevurderingDto(Opplysningskilde.BESKJED_ANNEN_ENHET, ugyldigÅrsak, null),
                )
            assertThatThrownBy { utførOgReturnerNesteSteg(dto) }
                .hasMessage("Årsak er ikke gyldig for stønadstype")
        }

        @Test
        internal fun `må angi beskrivelse når årsak=ANNET`() {
            val årsak = Revurderingsårsak.ANNET
            val dto =
                RevurderingsinformasjonDto(
                    LocalDate.now(),
                    ÅrsakRevurderingDto(Opplysningskilde.BESKJED_ANNEN_ENHET, årsak, "   "),
                )
            assertThatThrownBy { utførOgReturnerNesteSteg(dto) }
                .hasMessage("Må ha med beskrivelse når årsak er annet")
        }
    }

    fun utførOgReturnerNesteSteg(dto: RevurderingsinformasjonDto) {
        steg.utførOgReturnerNesteSteg(saksbehandling, dto)
    }
}
