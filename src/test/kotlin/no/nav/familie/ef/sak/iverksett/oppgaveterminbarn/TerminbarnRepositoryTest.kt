package no.nav.familie.ef.sak.no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.iverksett.oppgaveterminbarn.TerminbarnOppgave
import no.nav.familie.ef.sak.iverksett.oppgaveterminbarn.TerminbarnRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TerminbarnRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var terminbarnRepository: TerminbarnRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var barnRepository: BarnRepository

    @Test
    internal fun `to av tre utgåtte terminbarn, forvent to treff`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))
        val behandling = lagreInnvilgetBehandling(fagsak)
        barnRepository.insertAll(
            listOf(
                barn(behandlingId = behandling.id, termindato = LocalDate.now()),
                barn(
                    behandlingId = behandling.id,
                    termindato = LocalDate.now().minusWeeks(5),
                ),
                barn(
                    behandlingId = behandling.id,
                    termindato = LocalDate.now().minusWeeks(10),
                ),
            ),
        )

        val barnForUtplukk =
            terminbarnRepository.finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn(StønadType.OVERGANGSSTØNAD)
        assertThat(barnForUtplukk.size).isEqualTo(2)
    }

    @Test
    internal fun `ingen utgåtte terminbarn, forvent ingen treff`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))
        val behandling = lagreInnvilgetBehandling(fagsak)
        barnRepository.insertAll(
            listOf(
                barn(behandlingId = behandling.id, termindato = LocalDate.now()),
                barn(
                    behandlingId = behandling.id,
                    termindato = LocalDate.now().minusWeeks(3),
                ),
            ),
        )

        val barnForUtplukk =
            terminbarnRepository.finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn(StønadType.OVERGANGSSTØNAD)
        assertThat(barnForUtplukk.size).isEqualTo(0)
    }

    @Test
    internal fun `insert og hent utgått terminbarn, forvent ingen treff`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))
        val behandling = lagreInnvilgetBehandling(fagsak)
        val utgåttTermindato = LocalDate.now().minusWeeks(5)
        barnRepository.insertAll(
            listOf(
                barn(behandlingId = behandling.id, termindato = LocalDate.now()),
                barn(behandlingId = behandling.id, termindato = utgåttTermindato),
            ),
        )
        terminbarnRepository.insert(opprettTerminbarnOppgave(fagsak = fagsak.id, termindato = utgåttTermindato))

        val barnForUtplukk =
            terminbarnRepository.finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn(StønadType.OVERGANGSSTØNAD)
        assertThat(barnForUtplukk.size).isEqualTo(0)
    }

    private fun opprettTerminbarnOppgave(
        fagsak: UUID = UUID.randomUUID(),
        termindato: LocalDate = LocalDate.now(),
    ): TerminbarnOppgave =
        TerminbarnOppgave(
            fagsakId = fagsak,
            termindato = termindato,
            opprettetTid = LocalDate.now(),
        )

    private fun lagreInnvilgetBehandling(
        fagsak: Fagsak,
        tidligereBehandling: Behandling? = null,
        opprettetTid: LocalDateTime =
            tidligereBehandling?.sporbar?.opprettetTid?.plusHours(1)
                ?: LocalDateTime.now(),
    ) = behandlingRepository.insert(
        behandling(
            fagsak,
            status = BehandlingStatus.FERDIGSTILT,
            resultat = BehandlingResultat.INNVILGET,
            forrigeBehandlingId = tidligereBehandling?.id,
            opprettetTid = opprettetTid,
        ),
    )

    private fun barn(
        behandlingId: UUID,
        personIdent: String? = null,
        termindato: LocalDate? = LocalDate.now(),
    ): BehandlingBarn =
        BehandlingBarn(
            behandlingId = behandlingId,
            personIdent = personIdent,
            fødselTermindato = termindato,
            navn = null,
            søknadBarnId = UUID.randomUUID(),
        )
}
