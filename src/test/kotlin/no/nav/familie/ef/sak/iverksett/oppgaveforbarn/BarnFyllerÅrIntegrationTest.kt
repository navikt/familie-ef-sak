package no.nav.familie.ef.sak.no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.Alder
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.BarnFyllerÅrOppfølgingsoppgaveService
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.OpprettOppgavePayload
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.UnexpectedRollbackException
import java.time.LocalDate

class BarnFyllerÅrIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var barnFyllerÅrOppfølgingsoppgaveService: BarnFyllerÅrOppfølgingsoppgaveService

    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Autowired private lateinit var barnRepository: BarnRepository

    @Autowired private lateinit var vedtakRepository: VedtakRepository

    @Autowired private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired private lateinit var taskRepository: TaskRepository

    @Test
    fun `barn har blitt mer enn 6 mnd, skal opprette og lagre oppgave`() {
        val fødselsdato = LocalDate.now().minusDays(183)

        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET))

        val barnPersonIdent = FnrGenerator.generer(fødselsdato)
        barnRepository.insert(BehandlingBarn(behandlingId = behandling.id, personIdent = barnPersonIdent))

        vedtakRepository.insert(vedtak(behandling.id))
        lagreFremtidligAndel(behandling, 4000)

        barnFyllerÅrOppfølgingsoppgaveService.opprettTasksForAlleBarnSomHarFyltÅr()

        val tasks = taskRepository.findAll().toList()
        assertThat(tasks.size).isEqualTo(1)

        val opprettOppgavePayload = objectMapper.readValue<OpprettOppgavePayload>(tasks.first().payload)
        assertThat(opprettOppgavePayload.alder).isEqualTo(Alder.SEKS_MND)
        assertThat(opprettOppgavePayload.barnPersonIdent).isEqualTo(barnPersonIdent)

        assertThatThrownBy { barnFyllerÅrOppfølgingsoppgaveService.opprettTasksForAlleBarnSomHarFyltÅr()}
            .isInstanceOf(UnexpectedRollbackException::class.java)

        val tasksEtterAndreKjøring = taskRepository.findAll().toList()
        assertThat(tasksEtterAndreKjøring.size).isEqualTo(1)
    }

    @Test
    fun `barn har blitt mer enn 6 mnd, skal ikke opprette og lagre oppgave fordi behandling er ikke iverksatt`() {
        val fødselsdato = LocalDate.now().minusDays(183)

        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, BehandlingStatus.OPPRETTET, resultat = BehandlingResultat.INNVILGET))

        val barnPersonIdent = FnrGenerator.generer(fødselsdato)
        barnRepository.insert(BehandlingBarn(behandlingId = behandling.id, personIdent = barnPersonIdent))

        vedtakRepository.insert(vedtak(behandling.id))
        lagreFremtidligAndel(behandling, 3000)

        barnFyllerÅrOppfølgingsoppgaveService.opprettTasksForAlleBarnSomHarFyltÅr()
        assertThat(taskRepository.findAll().toList().isEmpty()).isTrue
    }

    private fun lagreFremtidligAndel(behandling: Behandling, beløp: Int): TilkjentYtelse {
        val andel = lagAndelTilkjentYtelse(
            beløp = beløp,
            kildeBehandlingId = behandling.id,
            fraOgMed = LocalDate.now().minusMonths(1),
            tilOgMed = LocalDate.now().plusMonths(1)
        )
        return tilkjentYtelseRepository.insert(
            lagTilkjentYtelse(
                behandlingId = behandling.id,
                andelerTilkjentYtelse = listOf(andel)
            )
        )
    }
}
