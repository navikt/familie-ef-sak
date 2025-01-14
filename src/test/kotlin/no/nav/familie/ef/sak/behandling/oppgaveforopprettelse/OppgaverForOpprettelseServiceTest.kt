package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.iverksattFørstegangsbehandling
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.iverksattRevurdering
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.AvslagÅrsak
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class OppgaverForOpprettelseServiceTest {
    private val oppgaverForOpprettelseRepository = mockk<OppgaverForOpprettelseRepository>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val behandlingService = mockk<BehandlingService>()
    private val vedtakService = mockk<VedtakService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private var oppgaverForOpprettelseService =
        spyk(OppgaverForOpprettelseService(oppgaverForOpprettelseRepository, behandlingService, tilkjentYtelseService, vedtakService, featureToggleService))

    private val behandling = behandling(fagsak = fagsak())
    private val behandlingId = behandling.id
    private val oppgaverForOpprettelse = OppgaverForOpprettelse(behandlingId, emptyList())
    private val saksbehandling = lagSaksbehandling(stønadType = StønadType.OVERGANGSSTØNAD, behandling = behandling)
    private val vedtak = mockk<Vedtak>()

    @BeforeEach
    fun init() {
        every { oppgaverForOpprettelseRepository.deleteById(any()) } just runs
        every { oppgaverForOpprettelseRepository.insert(any()) } returns oppgaverForOpprettelse
        every { oppgaverForOpprettelseRepository.update(any()) } returns oppgaverForOpprettelse
        every { vedtak.resultatType } returns ResultatType.INNVILGE
        every { vedtakService.hentVedtak(any()) } returns vedtak
        every { featureToggleService.isEnabled(Toggle.FRONTEND_VIS_MARKERE_GODKJENNE_OPPGAVE_MODAL) } returns true
    }

    @Test
    fun `slett innslag når det ikke kan opprettes noen oppgaver og det finnes innslag fra før`() {
        every { oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(any()) } returns emptyList()
        every { oppgaverForOpprettelseRepository.existsById(any()) } returns true
        every { behandlingService.hentSaksbehandling(behandlingId) } returns saksbehandling

        oppgaverForOpprettelseService.opprettEllerErstatt(behandlingId, listOf())

        verify { oppgaverForOpprettelseRepository.deleteById(behandlingId) }
        verify(exactly = 0) { oppgaverForOpprettelseRepository.insert(any()) }
        verify(exactly = 0) { oppgaverForOpprettelseRepository.update(any()) }
    }

    @Test
    fun `ikke gjør noe når det ikke kan opprettes oppgaver og det ikke finnes innslag fra før`() {
        every { oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(any()) } returns emptyList()
        every { oppgaverForOpprettelseRepository.existsById(any()) } returns false

        oppgaverForOpprettelseService.opprettEllerErstatt(behandlingId, listOf())

        verify { oppgaverForOpprettelseRepository.deleteById(any()) }
        verify(exactly = 0) { oppgaverForOpprettelseRepository.insert(any()) }
        verify(exactly = 0) { oppgaverForOpprettelseRepository.update(any()) }
    }

    @Test
    fun `oppdater innslag når det finnes innslag, og når man kan oppdatere oppgaver `() {
        every { oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(any()) } returns
            listOf(
                OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID,
            )
        every { oppgaverForOpprettelseRepository.existsById(any()) } returns true

        oppgaverForOpprettelseService.opprettEllerErstatt(behandlingId, listOf())

        verify(exactly = 0) { oppgaverForOpprettelseRepository.deleteById(any()) }
        verify(exactly = 0) { oppgaverForOpprettelseRepository.insert(any()) }
        verify { oppgaverForOpprettelseRepository.update(any()) }
    }

    @Test
    fun `lag innslag når det ikke finnes innslag, og når man kan oppdatere oppgaver`() {
        every { oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(any()) } returns
            listOf(
                OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID,
            )
        every { oppgaverForOpprettelseRepository.existsById(any()) } returns false

        oppgaverForOpprettelseService.opprettEllerErstatt(behandlingId, listOf())

        verify(exactly = 0) { oppgaverForOpprettelseRepository.deleteById(any()) }
        verify { oppgaverForOpprettelseRepository.insert(any()) }
        verify(exactly = 0) { oppgaverForOpprettelseRepository.update(any()) }
    }

    @Test
    fun `skal kunne opprette oppgave hvis behandling er førstegangsbehandling`() {
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling

        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(iverksattFørstegangsbehandling.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isTrue
    }

    @Test
    fun `skal kunne opprette oppgave hvis behandling er en revurdering`() {
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid
        every { behandlingService.hentSaksbehandling(iverksattRevurdering.id) } returns saksbehandling
        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(iverksattRevurdering.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isTrue
    }

    @Test
    fun `skal ikke kunne opprette oppgave hvis behandling er førstegangsbehandling, men andeler under 1 år frem i tid`() {
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelseUnder1årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(iverksattFørstegangsbehandling.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isFalse
    }

    @Test
    fun `skal ikke kunne opprette fremleggsoppgave hvis stønadstype ikke er overgangsstønad`() {
        val saksbehandling = lagSaksbehandling(stønadType = StønadType.BARNETILSYN, behandling = behandling)
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelseUnder1årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(iverksattFørstegangsbehandling.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isFalse
    }

    @Test
    fun `ikke oppgaveopprettelse for avslått overgangsstønad med tilkjente ytelser under 1 år frem i tid`() {
        val saksbehandling = lagSaksbehandling(behandling = behandling)
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelseUnder1årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { vedtak.avslåÅrsak } returns AvslagÅrsak.MINDRE_INNTEKTSENDRINGER
        every { vedtak.resultatType } returns ResultatType.AVSLÅ
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling

        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(iverksattFørstegangsbehandling.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isFalse
    }

    @Test
    fun `oppgaveopprettelse for avslått overgangsstønad med tilkjente ytelser over 1 år frem i tid`() {
        val saksbehandling = lagSaksbehandling(behandling = behandling)
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { vedtak.avslåÅrsak } returns AvslagÅrsak.MINDRE_INNTEKTSENDRINGER
        every { vedtak.resultatType } returns ResultatType.AVSLÅ
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling

        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(iverksattFørstegangsbehandling.id)
        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isTrue()
    }

    @Test
    fun `oppgaveopprettelse for avslått overgangsstønad med ytelser frem i tid og avslagsårsak inntektsendringer`() {
        val saksbehandling = lagSaksbehandling(behandling = behandling)
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { vedtak.avslåÅrsak } returns AvslagÅrsak.MINDRE_INNTEKTSENDRINGER
        every { vedtak.resultatType } returns ResultatType.AVSLÅ
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling

        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(iverksattFørstegangsbehandling.id)
        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isTrue()
    }

    @Test
    fun `ikke oppgaveopprettelse for avslått overgangsstønad med avslagsårsak ulik inntektsendring`() {
        val saksbehandling = lagSaksbehandling(behandling = behandling)
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { vedtak.avslåÅrsak } returns AvslagÅrsak.KORTVARIG_AVBRUDD_JOBB
        every { vedtak.resultatType } returns ResultatType.AVSLÅ
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling

        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(iverksattFørstegangsbehandling.id)
        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isFalse()
    }

    @Test
    fun `siste iverksatte behandling hentes for avslag`() {
        val saksbehandling = lagSaksbehandling(behandling = behandling)
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { vedtak.avslåÅrsak } returns AvslagÅrsak.MINDRE_INNTEKTSENDRINGER
        every { vedtak.resultatType } returns ResultatType.AVSLÅ
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling

        oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(iverksattFørstegangsbehandling.id)
        verify { behandlingService.finnSisteIverksatteBehandling(any()) }
    }

    private val tilkjentYtelse2årFremITid =
        lagTilkjentYtelse(
            andelerTilkjentYtelse =
                listOf(
                    lagAndelTilkjentYtelse(
                        fraOgMed = LocalDate.now(),
                        kildeBehandlingId = UUID.randomUUID(),
                        beløp = 10_000,
                        tilOgMed = LocalDate.now().plusYears(2),
                    ),
                ),
        )

    private val tilkjentYtelseUnder1årFremITid =
        lagTilkjentYtelse(
            andelerTilkjentYtelse =
                listOf(
                    lagAndelTilkjentYtelse(
                        fraOgMed = LocalDate.now(),
                        kildeBehandlingId = UUID.randomUUID(),
                        beløp = 10_000,
                        tilOgMed = LocalDate.now().plusMonths(11),
                    ),
                ),
        )

    private fun lagSaksbehandling(
        stønadType: StønadType = StønadType.OVERGANGSSTØNAD,
        behandling: Behandling,
    ): Saksbehandling {
        val fagsak = fagsak(stønadstype = stønadType)
        return saksbehandling(fagsak, behandling)
    }
}
