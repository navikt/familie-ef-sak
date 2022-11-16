package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.RevurderingService
import no.nav.familie.ef.sak.behandling.dto.RevurderingBarnDto
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.ekstern.bisys.lagAndelHistorikkDto
import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class RevurderingServiceTest {

    val barnRepository = mockk<BarnRepository>()
    val vedtakHistorikkService = mockk<VedtakHistorikkService>()
    val revurderingService: RevurderingService = RevurderingService(
        søknadService = mockk(),
        behandlingService = mockk(),
        oppgaveService = mockk(),
        vurderingService = mockk(),
        grunnlagsdataService = mockk(),
        taskRepository = mockk(),
        barnService = mockk(),
        fagsakService = mockk(),
        vedtakService = mockk(),
        vedtakHistorikkService = vedtakHistorikkService,
        barnRepository = barnRepository
    )

    val fagsak = fagsak()
    val forrigeBehandling = behandling(fagsak)
    val revurdering = behandling(fagsak= fagsak, forrigeBehandlingId = forrigeBehandling.id)


    val historiskBehandlingsbarn = behandlingBarn(
        id = UUID.randomUUID(),
        behandlingId = forrigeBehandling.id,
        søknadBarnId = UUID.randomUUID(),
        personIdent = "01010112345",
        navn = "Ola",
        fødselTermindato = LocalDate.now()
    )

    val barn = behandlingBarn(
        id = UUID.randomUUID(),
        behandlingId = revurdering.id,
        søknadBarnId = UUID.randomUUID(),
        personIdent = "01010112345",
        navn = "Ola",
        fødselTermindato = LocalDate.now()
    )

    val element = lagAndelHistorikkDto(fraOgMed = LocalDate.now().minusMonths(2), tilOgMed = LocalDate.now(), behandlingBarn = listOf(historiskBehandlingsbarn), beløp = 0, endring = null)
    @Test
    fun `Skal kopiere vedtak innhold til ny behandling hvis satsendring `() {



        every { barnRepository.findByBehandlingId(revurdering.id) } returns listOf(barn)
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(element)
        every { barnRepository.findAllById(listOf(historiskBehandlingsbarn.id)) } returns listOf( historiskBehandlingsbarn)

        revurderingService.kopierVedtakHvisSatsendring(BehandlingÅrsak.SATSENDRING, fagsak = fagsak, revurdering = revurdering)
    }
}
