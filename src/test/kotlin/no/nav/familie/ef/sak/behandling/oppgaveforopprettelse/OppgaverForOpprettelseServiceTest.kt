package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.henlagtFørstegangsbehandling
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.iverksattFørstegangsbehandling
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.revurdering
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class OppgaverForOpprettelseServiceTest {

    private val oppgaverForOpprettelseRepository = mockk<OppgaverForOpprettelseRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()

    private var oppgaverForOpprettelseService =
        spyk(OppgaverForOpprettelseService(oppgaverForOpprettelseRepository, behandlingService, tilkjentYtelseService))

    private val behandlingId = UUID.randomUUID()
    private val oppgaverForOpprettelse = OppgaverForOpprettelse(behandlingId, emptyList())

    @BeforeEach
    fun init() {
        every { oppgaverForOpprettelseRepository.deleteById(any()) } just runs
        every { oppgaverForOpprettelseRepository.insert(any()) } returns oppgaverForOpprettelse
        every { oppgaverForOpprettelseRepository.update(any()) } returns oppgaverForOpprettelse
    }

    @Test
    fun `slett innslag når det ikke kan opprettes noen oppgaver og det finnes innslag fra før`() {
        every { oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(any()) } returns emptyList()
        every { oppgaverForOpprettelseRepository.existsById(any()) } returns true

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
        every { oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(any()) } returns listOf(
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
        every { oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(any()) } returns listOf(
            OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID,
        )
        every { oppgaverForOpprettelseRepository.existsById(any()) } returns false

        oppgaverForOpprettelseService.opprettEllerErstatt(behandlingId, listOf())

        verify(exactly = 0) { oppgaverForOpprettelseRepository.deleteById(any()) }
        verify { oppgaverForOpprettelseRepository.insert(any()) }
        verify(exactly = 0) { oppgaverForOpprettelseRepository.update(any()) }
    }

    @Test
    fun `skal kunne opprette oppgave hvis forrige behandlingen er en HENLAGT førstegangsbehandling`() {
        val behandling = revurdering.copy(forrigeBehandlingId = henlagtFørstegangsbehandling.id)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { behandlingService.hentBehandling(henlagtFørstegangsbehandling.id) } returns henlagtFørstegangsbehandling
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid

        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(behandling.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isTrue
    }

    @Test
    fun `skal kunne opprette oppgave hvis forrige behandling er en AVSLÅTT førstegangsbehandling`() {
        val behandling = revurdering.copy(forrigeBehandlingId = henlagtFørstegangsbehandling.id)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { behandlingService.hentBehandling(henlagtFørstegangsbehandling.id) } returns henlagtFørstegangsbehandling.copy(
            resultat = BehandlingResultat.AVSLÅTT,
        )

        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid

        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(behandling.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isTrue
    }

    @Test
    fun `skal ikke kunne opprette oppgave hvis forrige behandling er en INNVILGET førstegangsbehandling`() {
        val behandling = revurdering.copy(forrigeBehandlingId = henlagtFørstegangsbehandling.id)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { behandlingService.hentBehandling(henlagtFørstegangsbehandling.id) } returns iverksattFørstegangsbehandling
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid

        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(behandling.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isFalse
    }

    @Test
    fun `skal kunne opprette oppgave hvis behandling er førstegangsbehandling`() {
        every { behandlingService.hentBehandling(iverksattFørstegangsbehandling.id) } returns iverksattFørstegangsbehandling
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid

        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(iverksattFørstegangsbehandling.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isTrue
    }

    @Test
    fun `skal ikke kunne opprette oppgave hvis behandling er førstegangsbehandling, men andeler under 1 år frem i tid`() {
        every { behandlingService.hentBehandling(iverksattFørstegangsbehandling.id) } returns iverksattFørstegangsbehandling
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelseUnder1årFremITid

        val oppgaver = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(iverksattFørstegangsbehandling.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isFalse
    }

    private val tilkjentYtelse2årFremITid = lagTilkjentYtelse(
        andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                fraOgMed = LocalDate.now(),
                kildeBehandlingId = UUID.randomUUID(),
                beløp = 10_000,
                tilOgMed = LocalDate.now().plusYears(2),
            ),
        ),
    )

    private val tilkjentYtelseUnder1årFremITid = lagTilkjentYtelse(
        andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelse(
                fraOgMed = LocalDate.now(),
                kildeBehandlingId = UUID.randomUUID(),
                beløp = 10_000,
                tilOgMed = LocalDate.now().plusMonths(11),
            ),
        ),
    )
}
