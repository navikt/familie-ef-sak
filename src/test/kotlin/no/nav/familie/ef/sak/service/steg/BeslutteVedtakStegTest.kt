package no.nav.familie.ef.sak.service.steg

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.api.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.mapper.IverksettingDtoMapper
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Fil
import no.nav.familie.ef.sak.repository.domain.Oppgave
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.Vedtak
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.service.BehandlingshistorikkService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.TotrinnskontrollService
import no.nav.familie.ef.sak.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.task.PollStatusFraIverksettTask
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Properties
import java.util.UUID

internal class BeslutteVedtakStegTest {

    private val taskRepository = mockk<TaskRepository>()
    private val fagsakService = mockk<FagsakService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>()
    private val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val iverksettingDtoMapper = mockk<IverksettingDtoMapper>()
    private val iverksett = mockk<IverksettClient>()
    private val vedtakService = mockk<VedtakService>()

    private val beslutteVedtakSteg = BeslutteVedtakSteg(taskRepository,
                                                        fagsakService,
                                                        oppgaveService,
                                                        iverksett,
                                                        iverksettingDtoMapper,
                                                        totrinnskontrollService,
                                                        vedtaksbrevRepository,
                                                        behandlingshistorikkService,
                                                        vedtakService)
    private val vedtaksbrev = Vedtaksbrev(UUID.randomUUID(),
                                          "123",
                                          "mal",
                                          "sign1",
                                          "sign2",
                                          Fil("123".toByteArray()))

    private val fagsak = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                                søkerIdenter = setOf(FagsakPerson(ident = "12345678901")))
    private val behandlingId = UUID.randomUUID()

    private val oppgave = Oppgave(id = UUID.randomUUID(), behandlingId = behandlingId, gsakOppgaveId = 123L, type = Oppgavetype.BehandleSak, erFerdigstilt = false)
    private lateinit var taskSlot: MutableList<Task>


    @BeforeEach
    internal fun setUp() {
        mockBrukerContext("sign2")
        taskSlot = mutableListOf()
        every {
            fagsakService.hentAktivIdent(any())
        } returns fagsak.hentAktivIdent()
        every {
            taskRepository.save(capture(taskSlot))
        } returns Task("", "", Properties())
        every { oppgaveService.hentOppgaveSomIkkeErFerdigstilt(any(), any()) } returns oppgave
        every { vedtaksbrevRepository.deleteById(any()) } just Runs
        every { iverksettingDtoMapper.tilDto(any(), any()) } returns mockk()
        every { iverksett.iverksett(any(), any()) } just Runs
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `skal opprette iverksettMotOppdragTask etter beslutte vedtak hvis godkjent`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev
        every { vedtakService.oppdaterBeslutter(behandlingId, any()) } just Runs
        every { vedtakService.hentVedtak(behandlingId) } returns Vedtak(behandlingId = behandlingId, resultatType = ResultatType.INNVILGE, saksbehandlerIdent = "sak1", beslutterIdent = "beslutter1" )
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any())} returns Behandlingshistorikk(behandlingId = behandlingId,
                                                                                                                      steg = StegType.SEND_TIL_BESLUTTER,
                                                                                                                      opprettetAvNavn = "sb sb",
                                                                                                                      opprettetAv = "saksbehandler1")
        val nesteSteg = utførTotrinnskontroll(godkjent = true)
        assertThat(nesteSteg).isEqualTo(StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        assertThat(taskSlot[0].type).isEqualTo(FerdigstillOppgaveTask.TYPE)
        assertThat(taskSlot[1].type).isEqualTo(PollStatusFraIverksettTask.TYPE)
        assertThat(taskSlot[2].type).isEqualTo(BehandlingsstatistikkTask.TYPE)
    }

    @Test
    internal fun `skal opprette opprettBehandleUnderkjentVedtakOppgave etter beslutte vedtak hvis underkjent`() {
        val nesteSteg = utførTotrinnskontroll(godkjent = false)

        val deserializedPayload = objectMapper.readValue<OpprettOppgaveTask.OpprettOppgaveTaskData>(taskSlot[1].payload)

        assertThat(nesteSteg).isEqualTo(StegType.SEND_TIL_BESLUTTER)
        assertThat(taskSlot[0].type).isEqualTo(FerdigstillOppgaveTask.TYPE)
        assertThat(taskSlot[1].type).isEqualTo(OpprettOppgaveTask.TYPE)
        assertThat(deserializedPayload.oppgavetype).isEqualTo(Oppgavetype.BehandleUnderkjentVedtak)
    }

    private fun utførTotrinnskontroll(godkjent: Boolean): StegType {
        val nesteSteg = beslutteVedtakSteg.utførOgReturnerNesteSteg(Behandling(id = behandlingId,
                                                                               fagsakId = fagsak.id,
                                                                               type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                               status = BehandlingStatus.FATTER_VEDTAK,
                                                                               steg = beslutteVedtakSteg.stegType(),
                                                                               resultat = BehandlingResultat.IKKE_SATT),
                                                                    BeslutteVedtakDto(godkjent = godkjent))
        return nesteSteg
    }
}