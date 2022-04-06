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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TerminbarnRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var terminbarnRepository: TerminbarnRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var barnRepository: BarnRepository

    @Test
    internal fun `ett av to utgåtte terminbarn, forvent ett treff`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))
        val behandling = lagreInnvilgetBehandling(fagsak)
        barnRepository.insertAll(listOf(barn(behandlingId = behandling.id, termindato = LocalDate.now()),
                                        barn(behandlingId = behandling.id,
                                             termindato = LocalDate.now().minusWeeks(5))))

        val barnForUtplukk = terminbarnRepository.finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn()
        assertThat(barnForUtplukk.size).isEqualTo(1)
    }

    @Test
    internal fun `ingen utgåtte terminbarn, forvent ingen treff`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))
        val behandling = lagreInnvilgetBehandling(fagsak)
        barnRepository.insertAll(listOf(barn(behandlingId = behandling.id, termindato = LocalDate.now()),
                                        barn(behandlingId = behandling.id,
                                             termindato = LocalDate.now().minusWeeks(3))))

        val barnForUtplukk = terminbarnRepository.finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn()
        assertThat(barnForUtplukk.size).isEqualTo(0)
    }

    @Test
    internal fun `insert terminbarn, forvent ingen feil`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))
        terminbarnRepository.insert(opprettTerminbarnOppgave(fagsak = fagsak.id))
    }

    @Test
    internal fun `insert og hent utgått terminbarn, forvent existByFagsakIdAndTermindato er lik true`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))
        val behandling = lagreInnvilgetBehandling(fagsak)
        val utgåttTermindato = LocalDate.now().minusWeeks(5)
        barnRepository.insertAll(listOf(barn(behandlingId = behandling.id, termindato = LocalDate.now()),
                                        barn(behandlingId = behandling.id,
                                             termindato = utgåttTermindato)))

        terminbarnRepository.insert(opprettTerminbarnOppgave(fagsak = fagsak.id, termindato = utgåttTermindato))
        assertThat(terminbarnRepository.existsByFagsakIdAndTermindato(fagsakId = fagsak.id,
                                                                      termindato = utgåttTermindato)).isTrue()
    }

    private fun opprettTerminbarnOppgave(fagsak: UUID = UUID.randomUUID(),
                                         termindato: LocalDate = LocalDate.now()): TerminbarnOppgave {
        return TerminbarnOppgave(fagsakId = fagsak,
                                 termindato = termindato,
                                 opprettetTid = LocalDate.now())
    }

    private fun lagreInnvilgetBehandling(fagsak: Fagsak,
                                         tidligereBehandling: Behandling? = null,
                                         opprettetTid: LocalDateTime = tidligereBehandling?.sporbar?.opprettetTid?.plusHours(1)
                                                                       ?: LocalDateTime.now()) =
            behandlingRepository.insert(behandling(fagsak,
                                                   status = BehandlingStatus.FERDIGSTILT,
                                                   resultat = BehandlingResultat.INNVILGET,
                                                   forrigeBehandlingId = tidligereBehandling?.id,
                                                   opprettetTid = opprettetTid))

    private fun barn(behandlingId: UUID, personIdent: String? = null, termindato: LocalDate? = LocalDate.now()): BehandlingBarn {
        return BehandlingBarn(behandlingId = behandlingId,
                              personIdent = personIdent,
                              fødselTermindato = termindato,
                              navn = null,
                              søknadBarnId = UUID.randomUUID())

    }

}