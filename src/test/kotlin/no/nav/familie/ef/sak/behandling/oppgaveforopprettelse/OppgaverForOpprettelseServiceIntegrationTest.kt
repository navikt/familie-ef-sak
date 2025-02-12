package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.oppfølgingsoppgave.OppfølgingsoppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.SendTilBeslutterDto
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class OppgaverForOpprettelseServiceIntegrationTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var vedtakService: VedtakService

    @Autowired
    private lateinit var oppfølgingsoppgaveService: OppfølgingsoppgaveService

    val fagsak = fagsak()
    val behandling = behandling(fagsak = fagsak)
    val behandlingId = behandling.id

    val vedtakRequest = InnvilgelseOvergangsstønad("", "")

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        behandlingRepository.insert(behandling)
        vedtakService.lagreVedtak(vedtakRequest, behandling.id, fagsak.stønadstype)
    }

    @Test
    internal fun `opprett oppgaver for opprettelse`() {
        opprettTilkjentYtelse(1000)
        opprettInntektskontroll()

        assertThat(oppfølgingsoppgaveService.hentOppgaverForOpprettelseEllerNull(behandlingId)?.oppgavetyper).containsExactly(
            OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID,
        )
    }

    @Test
    internal fun `oppdater oppgaver med tom liste`() {
        opprettTilkjentYtelse(1000)
        opprettInntektskontroll()
        opprettTomListe()

        assertThat(oppfølgingsoppgaveService.hentOppgaverForOpprettelseEllerNull(behandlingId)?.oppgavetyper).isEmpty()
    }

    private fun opprettTomListe() {
        oppfølgingsoppgaveService.lagreOppgaverForOpprettelse(
            behandlingId,
            data =
                SendTilBeslutterDto(
                    emptyList(),
                ),
        )
    }

    private fun opprettInntektskontroll() {
        oppfølgingsoppgaveService.lagreOppgaverForOpprettelse(
            behandlingId,
            data =
                SendTilBeslutterDto(
                    oppgavetyperSomSkalOpprettes = listOf(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID),
                ),
        )
    }

    private fun opprettTilkjentYtelse(beløp: Int) {
        val andel =
            lagAndelTilkjentYtelse(
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
