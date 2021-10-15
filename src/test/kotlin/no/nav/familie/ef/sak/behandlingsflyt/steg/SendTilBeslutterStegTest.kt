package no.nav.familie.ef.sak.behandlingsflyt.steg

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask.FerdigstillOppgaveTaskData
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Properties
import java.util.UUID

internal class SendTilBeslutterStegTest {

    private val taskRepository = mockk<TaskRepository>()
    private val fagsakService = mockk<FagsakService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    private val vedtakService = mockk<VedtakService>()


    private val beslutteVedtakSteg =
            SendTilBeslutterSteg(taskRepository, oppgaveService, fagsakService, behandlingService, vedtaksbrevRepository, vedtakService)
    private val fagsak = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                                søkerIdenter = setOf(FagsakPerson(ident = "12345678901")))
    private val vedtaksbrev = Vedtaksbrev(behandlingId = UUID.randomUUID(),
                                          saksbehandlerBrevrequest = "",
                                          brevmal = "",
                                          "",
                                          "",
                                          null)

    private val behandling = Behandling(fagsakId = fagsak.id,
                                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                        status = BehandlingStatus.UTREDES,
                                        steg = beslutteVedtakSteg.stegType(),
                                        resultat = BehandlingResultat.IKKE_SATT,
                                        årsak = BehandlingÅrsak.SØKNAD)
    private lateinit var taskSlot: MutableList<Task>

    @BeforeEach
    internal fun setUp() {
        taskSlot = mutableListOf()
        every {
            fagsakService.hentFagsak(any())
        } returns fagsak
        every {
            fagsakService.hentAktivIdent(any())
        } returns "12345678901"
        every {
            taskRepository.save(capture(taskSlot))
        } returns Task("", "", Properties())
        every { oppgaveService.hentOppgaveSomIkkeErFerdigstilt(any(), any()) } returns null

        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev
        every { vedtaksbrevRepository.update(any()) } returns vedtaksbrev

    }

    @Test
    internal fun `Skal avslutte oppgave BehandleSak hvis den finnes`() {
        utførOgVerifiserKall(Oppgavetype.BehandleSak)
    }

    @Test
    internal fun `Skal avslutte oppgave BehandleUnderkjentVedtak hvis den finnes`() {
        utførOgVerifiserKall(Oppgavetype.BehandleUnderkjentVedtak)
    }


    private fun utførOgVerifiserKall(oppgavetype: Oppgavetype) {
        every { oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, any()) } returns Oppgave(id = UUID.randomUUID(),
                                                                                                     behandlingId = behandling.id,
                                                                                                     gsakOppgaveId = 123L,
                                                                                                     type = Oppgavetype.BehandleSak,
                                                                                                     erFerdigstilt = false)

        every { vedtakService.oppdaterSaksbehandler(any(), any()) } just Runs
        mockBrukerContext("saksbehandlernavn")

        utførSteg()
        clearBrukerContext()

        verify { behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FATTER_VEDTAK) }

        assertThat(taskSlot[0].type).isEqualTo(OpprettOppgaveTask.TYPE)
        assertThat(objectMapper.readValue<OpprettOppgaveTaskData>(taskSlot[0].payload).oppgavetype)
                .isEqualTo(Oppgavetype.GodkjenneVedtak)

        assertThat(taskSlot[1].type).isEqualTo(FerdigstillOppgaveTask.TYPE)
        assertThat(objectMapper.readValue<FerdigstillOppgaveTaskData>(taskSlot[1].payload).oppgavetype)
                .isEqualTo(oppgavetype)
    }

    private fun utførSteg() {
        beslutteVedtakSteg.utførSteg(behandling, null)
    }
}