package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

        verify(exactly = 0) { oppgaverForOpprettelseRepository.deleteById(any()) }
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
}
