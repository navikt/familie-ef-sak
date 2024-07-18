package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.ekstern.bisys.lagAndelHistorikkDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.SamordningsfradragType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.tilVedtak
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertIs

class VedtakHistorikkServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val andelsHistorikkService = mockk<AndelsHistorikkService>()
    private val barnService = mockk<BarnService>()
    private val behandlingService = mockk<BehandlingService>()
    private val vedtakService = mockk<VedtakService>()

    private val vedtakHistorikkService = VedtakHistorikkService(fagsakService, andelsHistorikkService, barnService, behandlingService, vedtakService)

    @BeforeEach
    internal fun setUp() {
        every { andelsHistorikkService.hentHistorikk(any(), any()) } returns
            listOf(lagAndelHistorikkDto(tilOgMed = LocalDate.of(2024, 12, 1), aktivitet = AktivitetType.BARN_UNDER_ETT_ÅR, periodeType = VedtaksperiodeType.HOVEDPERIODE))
    }

    @Test
    internal fun `førstegangsbehandling, OS - skal sette null som samordningsfradragstype`() {
        val fagsakOS = fagsak(stønadstype = StønadType.OVERGANGSSTØNAD)
        val førstegangsbehandling = behandling(fagsakOS, type = BehandlingType.FØRSTEGANGSBEHANDLING, forrigeBehandlingId = null)

        every { fagsakService.hentFagsakForBehandling(førstegangsbehandling.id) } returns fagsakOS
        every { behandlingService.hentBehandling(førstegangsbehandling.id) } returns førstegangsbehandling

        val vedtak = vedtakHistorikkService.hentVedtakFraDato(førstegangsbehandling.id, YearMonth.now())
        assertThat(vedtak.resultatType).isEqualTo(ResultatType.INNVILGE)
        assertThat(vedtak._type).isEqualTo("InnvilgelseOvergangsstønad")
        assertIs<InnvilgelseOvergangsstønad>(vedtak)

        val innvilgelse = vedtak.tilVedtak(førstegangsbehandling.id, StønadType.OVERGANGSSTØNAD)
        assertThat(innvilgelse.samordningsfradragType).isNull()
    }

    @Test
    internal fun `revurdering, OS - skal hente med samordningsfradragstype fra den forrige behandlingen`() {
        val fagsakOS = fagsak(stønadstype = StønadType.OVERGANGSSTØNAD)
        val førstegangsbehandling = behandling(fagsakOS, type = BehandlingType.FØRSTEGANGSBEHANDLING, forrigeBehandlingId = null)
        val revurdering = behandling(fagsakOS, type = BehandlingType.REVURDERING, forrigeBehandlingId = førstegangsbehandling.id)
        val førstegangsvedtak = vedtak(behandlingId = førstegangsbehandling.id, samordningsfradragType = SamordningsfradragType.UFØRETRYGD)

        every { fagsakService.hentFagsakForBehandling(revurdering.id) } returns fagsakOS
        every { behandlingService.hentBehandling(revurdering.id) } returns revurdering
        every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns førstegangsvedtak

        val vedtak = vedtakHistorikkService.hentVedtakFraDato(revurdering.id, YearMonth.now())
        assertThat(vedtak.resultatType).isEqualTo(ResultatType.INNVILGE)
        assertThat(vedtak._type).isEqualTo("InnvilgelseOvergangsstønad")
        assertIs<InnvilgelseOvergangsstønad>(vedtak)

        val innvilgelse = vedtak.tilVedtak(revurdering.id, StønadType.OVERGANGSSTØNAD)
        assertThat(innvilgelse.samordningsfradragType).isEqualTo(SamordningsfradragType.UFØRETRYGD)
    }

    @Test
    internal fun `revurdering, OS - skal hente null som samordningsfradragstype fra den forrige behandlingen`() {
        val fagsakOS = fagsak(stønadstype = StønadType.OVERGANGSSTØNAD)
        val førstegangsbehandling = behandling(fagsakOS, type = BehandlingType.FØRSTEGANGSBEHANDLING, forrigeBehandlingId = null)
        val revurdering = behandling(fagsakOS, type = BehandlingType.REVURDERING, forrigeBehandlingId = førstegangsbehandling.id)
        val førstegangsvedtak = vedtak(behandlingId = førstegangsbehandling.id, samordningsfradragType = null)

        every { fagsakService.hentFagsakForBehandling(revurdering.id) } returns fagsakOS
        every { behandlingService.hentBehandling(revurdering.id) } returns revurdering
        every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns førstegangsvedtak

        val vedtak = vedtakHistorikkService.hentVedtakFraDato(revurdering.id, YearMonth.now())
        assertThat(vedtak.resultatType).isEqualTo(ResultatType.INNVILGE)
        assertThat(vedtak._type).isEqualTo("InnvilgelseOvergangsstønad")
        assertIs<InnvilgelseOvergangsstønad>(vedtak)

        val innvilgelse = vedtak.tilVedtak(revurdering.id, StønadType.OVERGANGSSTØNAD)
        assertThat(innvilgelse.samordningsfradragType).isEqualTo(null)
    }
}
