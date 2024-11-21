package no.nav.familie.ef.sak.selvstendig

import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.testutil.kjørSomLeader
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class NæringsinntektKontrollServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var næringsinntektKontrollService: NæringsinntektKontrollService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var oppgaveClient: OppgaveClient

    private val fagsakTilknyttetPersonIdent = fagsak(setOf(PersonIdent("11111111111")))
    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        val finnOppgaveRequest =
            FinnOppgaveRequest(
                tema = Tema.ENF,
                behandlingstema = Behandlingstema.Overgangsstønad,
                oppgavetype = Oppgavetype.Fremlegg,
                fristTomDato = LocalDate.of(YearMonth.now().year, 12, 15),
                mappeId = 107,
            )
        every { oppgaveClient.hentOppgaver(finnOppgaveRequest) } returns FinnOppgaveResponseDto(1, listOf(lagEksternTestOppgave()))
        testoppsettService.lagreFagsak(fagsakTilknyttetPersonIdent)
        val behandling = behandling(id = behandlingId, fagsak = fagsakTilknyttetPersonIdent, status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET)
        behandlingRepository.insert(behandling)
        val vedtak = vedtak(behandlingId = behandlingId, år = YearMonth.now().minusYears(1).year)
        vedtakRepository.insert(vedtak)
    }

    private fun lagEksternTestOppgave(tilordnetRessurs: String? = null): no.nav.familie.kontrakter.felles.oppgave.Oppgave =
        no.nav.familie.kontrakter.felles.oppgave
            .Oppgave(id = 1, tilordnetRessurs = tilordnetRessurs, oppgavetype = Oppgavetype.Fremlegg.toString(), fristFerdigstillelse = LocalDate.of(YearMonth.now().year, 12, 15).toString(), mappeId = 107, identer = listOf(OppgaveIdentV2("11111111111", IdentGruppe.FOLKEREGISTERIDENT)))

    @Test
    fun `sjekkNæringsinntektMotForventetInntekt`() {
        kjørSomLeader {
            val behandlingIds = næringsinntektKontrollService.sjekkNæringsinntektMotForventetInntekt()
            assertThat(behandlingIds).hasSize(1)
        }
    }
}
