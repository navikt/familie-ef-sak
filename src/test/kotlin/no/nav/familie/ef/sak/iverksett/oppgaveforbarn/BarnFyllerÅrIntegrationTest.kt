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
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødsel
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.internal.TaskService
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

    @Autowired private lateinit var taskService: TaskService

    @Autowired private lateinit var grunnlagsdataService: GrunnlagsdataService

    @Test
    fun `barn har blitt mer enn 6 mnd, skal opprette og lagre oppgave`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET))
        val fødselsdato = LocalDate.now().minusMonths(6).minusDays(1)

        val barnPersonIdent = "01012067050" // Se PdlClientConfig
        barnRepository.insert(BehandlingBarn(personIdent = barnPersonIdent, behandlingId = behandling.id))
        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        oppdaterGrunnlagsdata(behandling, fødselsdato)
        vedtakRepository.insert(vedtak(behandling.id))
        lagreFremtidligAndel(behandling, 4000)

        barnFyllerÅrOppfølgingsoppgaveService.opprettTasksForAlleBarnSomHarFyltÅr()

        val tasks = taskService.findAll().toList()
        assertThat(tasks.size).isEqualTo(1)

        val opprettOppgavePayload = objectMapper.readValue<OpprettOppgavePayload>(tasks.first().payload)
        assertThat(opprettOppgavePayload.alder).isEqualTo(Alder.SEKS_MND)
        assertThat(opprettOppgavePayload.barnPersonIdent).isEqualTo(barnPersonIdent)

        assertThatThrownBy { barnFyllerÅrOppfølgingsoppgaveService.opprettTasksForAlleBarnSomHarFyltÅr() }
            .isInstanceOf(UnexpectedRollbackException::class.java)

        val tasksEtterAndreKjøring = taskService.findAll().toList()
        assertThat(tasksEtterAndreKjøring.size).isEqualTo(1)
    }

    @Test
    fun `barn har blitt mer enn 6 mnd, skal ikke opprette og lagre oppgave fordi behandling er ikke iverksatt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, BehandlingStatus.OPPRETTET, resultat = BehandlingResultat.INNVILGET))

        val barnPersonIdent = FnrGenerator.generer()
        barnRepository.insert(BehandlingBarn(personIdent = barnPersonIdent, behandlingId = behandling.id))

        vedtakRepository.insert(vedtak(behandling.id))
        lagreFremtidligAndel(behandling, 3000)

        barnFyllerÅrOppfølgingsoppgaveService.opprettTasksForAlleBarnSomHarFyltÅr()
        assertThat(taskService.findAll().toList().isEmpty()).isTrue
    }

    private fun oppdaterGrunnlagsdata(
        behandling: Behandling,
        fødselsdato: LocalDate,
    ) {
        val lagretGrunnlagsdata = grunnlagsdataService.hentLagretGrunnlagsdata(behandling.id)
        val oppdatertBarneListe =
            lagretGrunnlagsdata.data.barn.map { barn ->
                if (barn == lagretGrunnlagsdata.data.barn.first()) {
                    barn.copy(fødsel = listOf(fødsel(fødselsdato)))
                } else {
                    barn
                }
            }
        val lagretGrunnlagsdataMedOppdatertBarn = lagretGrunnlagsdata.copy(data = lagretGrunnlagsdata.data.copy(barn = oppdatertBarneListe))
        grunnlagsdataService.oppdaterEndringer(lagretGrunnlagsdataMedOppdatertBarn)
    }

    private fun lagreFremtidligAndel(
        behandling: Behandling,
        beløp: Int,
    ): TilkjentYtelse {
        val andel =
            lagAndelTilkjentYtelse(
                beløp = beløp,
                kildeBehandlingId = behandling.id,
                fraOgMed = LocalDate.now().minusMonths(1),
                tilOgMed = LocalDate.now().plusMonths(1),
            )
        return tilkjentYtelseRepository.insert(
            lagTilkjentYtelse(
                behandlingId = behandling.id,
                andelerTilkjentYtelse = listOf(andel),
            ),
        )
    }
}
