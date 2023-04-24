package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class OppgaveForOpprettelseServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var oppgaverForOpprettelseService: OppgaverForOpprettelseService

    val fagsak = fagsak()
    val behandling = behandling(fagsak)
    val behandlingId = behandling.id

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    internal fun `opprett oppgaver for opprettelse`() {
        opprettTilkjentYtelse(1000)
        opprettInntektskontroll()

        assertThat(oppgaverForOpprettelseService.hentOppgaverForOpprettelseEllerNull(behandlingId)?.oppgavetyper)
            .containsExactly(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)
    }

    @Test
    internal fun `oppdater oppgaver med tom liste`() {
        opprettTilkjentYtelse(1000)
        opprettInntektskontroll()
        opprettTomListe()

        assertThat(oppgaverForOpprettelseService.hentOppgaverForOpprettelseEllerNull(behandlingId)?.oppgavetyper)
            .isEmpty()
    }

    @Test
    internal fun `skal ikke kunne opprette kontrollere inntekt hvis den ikke kan opprettes`() {
        opprettTilkjentYtelse(0)
        assertThatThrownBy { opprettInntektskontroll() }
            .hasMessageContaining("prøver å opprette")
    }

    private fun opprettTomListe() {
        oppgaverForOpprettelseService.opprettEllerErstatt(behandlingId, listOf())
    }

    private fun opprettInntektskontroll() {
        oppgaverForOpprettelseService.opprettEllerErstatt(
            behandlingId,
            listOf(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID),
        )
    }

    private fun opprettTilkjentYtelse(beløp: Int) {
        val andel = lagAndelTilkjentYtelse(
            beløp = beløp,
            fraOgMed = LocalDate.now(),
            tilOgMed = LocalDate.now().plusYears(2),
            kildeBehandlingId = behandlingId,
        )
        tilkjentYtelseRepository.insert(
            lagTilkjentYtelse(
                behandlingId = behandlingId,
                andelerTilkjentYtelse = listOf(andel),
            ),
        )
    }
}
