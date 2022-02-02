package no.nav.familie.ef.sak.service

import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.TekniskOpphørService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillBehandlingTask
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusTekniskOpphør
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPersonOld
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.infrastruktur.config.IverksettClientMock
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class TekniskOpphørTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var tekniskOpphørService: TekniskOpphørService
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var taskRepository: TaskRepository
    @Autowired lateinit var pollStatusTekniskOpphør: PollStatusTekniskOpphør
    @Autowired lateinit var ferdigstillBehandlingTask: FerdigstillBehandlingTask
    @Autowired lateinit var iverksettClient: IverksettClient
    @Autowired lateinit var tilkjentYtelseService: TilkjentYtelseService

    private val ident = "1234"
    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    internal fun setUp() {
        mockBrukerContext("saksbehandler")
        every { iverksettClient.hentStatus(any()) } returns IverksettStatus.OK_MOT_OPPDRAG

        fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(FagsakPersonOld(ident = ident))))
        behandling = behandlingRepository.insert(behandling(fagsak,
                                                            status = BehandlingStatus.FERDIGSTILT,
                                                            resultat = BehandlingResultat.INNVILGET))
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
        IverksettClientMock.clearMock(iverksettClient)
    }

    @Test
    internal fun `skal opprette ny behandling for teknisk opphør`() {
        håndterTekniskOpphør(fagsak)

        val sistIverksattBehandling = finnSistIverksatteBehandling()
        assertThat(sistIverksattBehandling!!.id).isNotEqualTo(behandling.id)

        assertThat(sistIverksattBehandling.type).isEqualTo(BehandlingType.TEKNISK_OPPHØR)
        assertThat(sistIverksattBehandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(sistIverksattBehandling.resultat).isEqualTo(BehandlingResultat.OPPHØRT)
    }

    @Test
    internal fun `skal ikke lagre tilkjent ytelse for behandling`() {
        håndterTekniskOpphør(fagsak)

        val sistIverksattBehandlingId = finnSistIverksatteBehandling()!!.id

        assertThat(tilkjentYtelseService.hentForBehandling(sistIverksattBehandlingId).andelerTilkjentYtelse)
                .isEmpty()
    }

    private fun finnSistIverksatteBehandling() = behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)

    private fun håndterTekniskOpphør(fagsak: Fagsak) {
        tekniskOpphørService.håndterTeknisktOpphør(fagsak.id)

        clearBrukerContext() // må kjøre task i context av system
        runPollStatusTekniskOpphør()
        runRerdigstillBehandlingTask()
    }

    private fun runPollStatusTekniskOpphør() {
        val task = taskRepository.findAll().first()
        assertThat(task.type).isEqualTo(PollStatusTekniskOpphør.TYPE)
        pollStatusTekniskOpphør.doTask(task)
    }

    private fun runRerdigstillBehandlingTask() {
        val task = taskRepository.findAll().last()
        assertThat(task.type).isEqualTo(FerdigstillBehandlingTask.TYPE)
        ferdigstillBehandlingTask.doTask(task)
    }

}