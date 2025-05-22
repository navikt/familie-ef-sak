package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service

import io.mockk.MockKException
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandling.revurdering.RevurderingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.journalføring.dto.BarnSomSkalFødes
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

internal class RevurderingServiceTest {
    val fagsakService = mockk<FagsakService>()
    private val revurderingService: RevurderingService =
        RevurderingService(
            søknadService = mockk(),
            behandlingService = mockk(),
            vurderingService = mockk(),
            grunnlagsdataService = mockk(),
            taskService = mockk(),
            barnService = mockk(),
            fagsakService = fagsakService,
            årsakRevurderingSteg = mockk(),
            stegService = mockk(),
            årsakRevurderingService = mockk(),
            kopierVedtakService = mockk(),
            vedtakService = mockk(),
            nyeBarnService = mockk(),
            tilordnetRessursService = mockk(),
        )

    @Test
    internal fun `revurdering - skal kaste feil dersom satsendring på overgangsstønad`() {
        val overgangsstønadFagsak = fagsak(stønadstype = StønadType.OVERGANGSSTØNAD)
        val revurderingDto = RevurderingDto(overgangsstønadFagsak.id, behandlingsårsak = BehandlingÅrsak.SATSENDRING, LocalDate.now(), VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE)
        every { fagsakService.fagsakMedOppdatertPersonIdent(overgangsstønadFagsak.id) } returns overgangsstønadFagsak

        val feil = assertThrows<Feil> { revurderingService.opprettRevurderingManuelt(revurderingDto) }
        assertThat(feil.message).isEqualTo("Kan ikke opprette revurdering med årsak satsendring for OVERGANGSSTØNAD")
    }

    @EnumSource(
        value = BehandlingÅrsak::class,
        names = ["NYE_OPPLYSNINGER", "PAPIRSØKNAD"],
        mode = EnumSource.Mode.INCLUDE,
    )
    @ParameterizedTest
    internal fun `skal kunne opprette revurdering med terminbarn gitt behandlingsårsaker`(behandlingÅrsak: BehandlingÅrsak) {
        val overgangsstønadFagsak = fagsak(stønadstype = StønadType.OVERGANGSSTØNAD)
        val revurderingDto = RevurderingDto(overgangsstønadFagsak.id, behandlingsårsak = behandlingÅrsak, LocalDate.now(), VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE, listOf(terminbarn))
        every { fagsakService.fagsakMedOppdatertPersonIdent(overgangsstønadFagsak.id) } returns overgangsstønadFagsak

        // For å unngå å mocke ut veldig mange servicer sjekker bare at vi kommer forbi valideringen
        val feil = assertThrows<MockKException> { revurderingService.opprettRevurderingManuelt(revurderingDto) }
        assertThat(feil.message).contains("no answer found for BehandlingService")
        assertThat(feil.message).contains("opprettBehandling(REVURDERING,")
        assertThat(feil.message).contains("among the configured answers")
    }

    @EnumSource(
        value = BehandlingÅrsak::class,
        names = ["NYE_OPPLYSNINGER", "PAPIRSØKNAD", "SATSENDRING"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    @ParameterizedTest
    internal fun `skal ikke kunne opprette revurdering med terminbarn gitt behandlingsårsaker`(behandlingÅrsak: BehandlingÅrsak) {
        val overgangsstønadFagsak = fagsak(stønadstype = StønadType.OVERGANGSSTØNAD)
        val revurderingDto = RevurderingDto(overgangsstønadFagsak.id, behandlingÅrsak, LocalDate.now(), VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE, listOf(terminbarn))
        every { fagsakService.fagsakMedOppdatertPersonIdent(overgangsstønadFagsak.id) } returns overgangsstønadFagsak

        val feil = assertThrows<Feil> { revurderingService.opprettRevurderingManuelt(revurderingDto) }
        assertThat(feil.message).isEqualTo("Terminbarn på revurdering kan kun legges inn for papirsøknader og nye opplysninger")
    }

    private val terminbarn = BarnSomSkalFødes(LocalDate.now().plusDays(10))
}
