package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.RevurderingService
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class RevurderingServiceTest {

    val fagsakService = mockk<FagsakService>()
    val revurderingService: RevurderingService = RevurderingService(
        søknadService = mockk(),
        behandlingService = mockk(),
        vurderingService = mockk(),
        grunnlagsdataService = mockk(),
        taskService = mockk(),
        barnService = mockk(),
        fagsakService = fagsakService,
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
}
