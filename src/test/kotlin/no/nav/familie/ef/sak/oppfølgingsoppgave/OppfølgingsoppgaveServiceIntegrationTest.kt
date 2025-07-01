package no.nav.familie.ef.sak.no.nav.familie.ef.sak.oppfølgingsoppgave

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.oppfølgingsoppgave.OppfølgingsoppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.SendTilBeslutterDto
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class OppfølgingsoppgaveServiceIntegrationTest : OppslagSpringRunnerTest() {
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
    val saksbehandling = saksbehandling(fagsak = fagsak)
    val behandlingId = behandling.id

    val vedtakRequest = InnvilgelseOvergangsstønad("", "")

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        behandlingRepository.insert(behandling)
        vedtakService.lagreVedtak(vedtakRequest, behandling.id, fagsak.stønadstype)
    }

//    @Test
//    internal fun `opprett oppgaver for opprettelse`() {
//        opprettTilkjentYtelse(1000)
//        opprettInntektskontroll()
//
//        assertThat(oppfølgingsoppgaveService.hentOppgaverForOpprettelseEllerNull(behandlingId)?.oppgavetyper).containsExactly(
//            OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID,
//        )
//    }

//    @Test
//    internal fun `oppdater oppgaver med tom liste`() {
//        opprettTilkjentYtelse(1000)
//        opprettInntektskontroll()
//        opprettTomListe()
//
//        assertThat(oppfølgingsoppgaveService.hentOppgaverForOpprettelseEllerNull(behandlingId)?.oppgavetyper).isEmpty()
//    }

    private fun opprettTomListe() {
        oppfølgingsoppgaveService.lagreOppgaverForOpprettelse(
            saksbehandling,
            data =
                SendTilBeslutterDto(
                    emptyList(),
                ),
        )
    }

//    private fun opprettInntektskontroll() {
//        oppfølgingsoppgaveService.lagreOppgaverForOpprettelse(
//            saksbehandling,
//            data =
//                SendTilBeslutterDto(
//                    oppgavetyperSomSkalOpprettes = listOf(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID),
//                ),
//        )
//    }

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
