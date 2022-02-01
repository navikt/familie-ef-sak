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
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTaskPayload
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
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
    private val iverksettingDtoMapper = mockk<IverksettingDtoMapper>()
    private val iverksett = mockk<IverksettClient>()
    private val vedtakService = mockk<VedtakService>()
    private val behandlingService = mockk<BehandlingService>()

    private val beslutteVedtakSteg = BeslutteVedtakSteg(taskRepository,
                                                        fagsakService,
                                                        oppgaveService,
                                                        iverksett,
                                                        iverksettingDtoMapper,
                                                        totrinnskontrollService,
                                                        vedtaksbrevRepository,
                                                        behandlingService,
                                                        vedtakService)

    private val innloggetBeslutter = "sign2"
    private val vedtaksbrev = Vedtaksbrev(behandlingId = UUID.randomUUID(),
                                          saksbehandlerBrevrequest = "123",
                                          brevmal = "mal",
                                          saksbehandlersignatur = "sign1",
                                          besluttersignatur = innloggetBeslutter,
                                          beslutterPdf = Fil("123".toByteArray()),
                                          enhet = "enhet",
                                          saksbehandlerident = "saksbIdent",
                                          beslutterident = innloggetBeslutter)

    private val fagsak = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                                søkerIdenter = setOf(FagsakPerson(ident = "12345678901")))
    private val behandlingId = UUID.randomUUID()

    private val oppgave = Oppgave(id = UUID.randomUUID(),
                                  behandlingId = behandlingId,
                                  gsakOppgaveId = 123L,
                                  type = Oppgavetype.BehandleSak,
                                  erFerdigstilt = false)
    private lateinit var taskSlot: MutableList<Task>


    @BeforeEach
    internal fun setUp() {
        mockBrukerContext(innloggetBeslutter)
        taskSlot = mutableListOf()
        every {
            fagsakService.hentAktivIdent(any())
        } returns fagsak.hentAktivIdent()
        every {
            fagsakService.fagsakMedOppdatertPersonIdent(any())
        } returns fagsak
        every {
            taskRepository.save(capture(taskSlot))
        } returns Task("", "", Properties())
        every { oppgaveService.hentOppgaveSomIkkeErFerdigstilt(any(), any()) } returns oppgave
        every { vedtaksbrevRepository.deleteById(any()) } just Runs
        every { iverksettingDtoMapper.tilDto(any(), any()) } returns mockk()
        every { iverksett.iverksett(any(), any()) } just Runs
        every { vedtakService.hentVedtak(any()) } returns Vedtak(behandlingId = UUID.randomUUID(),
                                                                 resultatType = ResultatType.INNVILGE,
                                                                 periodeBegrunnelse = null,
                                                                 inntektBegrunnelse = null,
                                                                 avslåBegrunnelse = null,
                                                                 perioder = null,
                                                                 inntekter = null,
                                                                 saksbehandlerIdent = null,
                                                                 beslutterIdent = null)

    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `skal opprette iverksettMotOppdragTask etter beslutte vedtak hvis godkjent`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev
        every { vedtakService.oppdaterBeslutter(behandlingId, any()) } just Runs
        every { vedtakService.hentVedtak(behandlingId) } returns Vedtak(behandlingId = behandlingId,
                                                                        resultatType = ResultatType.INNVILGE,
                                                                        saksbehandlerIdent = "sak1",
                                                                        beslutterIdent = "beslutter1")
        every { behandlingService.oppdaterResultatPåBehandling(any(), any()) } answers {
            behandling(fagsak, resultat = secondArg())
        }

        val nesteSteg = utførTotrinnskontroll(godkjent = true)
        assertThat(nesteSteg).isEqualTo(StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        assertThat(taskSlot[0].type).isEqualTo(FerdigstillOppgaveTask.TYPE)
        assertThat(taskSlot[1].type).isEqualTo(PollStatusFraIverksettTask.TYPE)
        assertThat(taskSlot[2].type).isEqualTo(BehandlingsstatistikkTask.TYPE)
        assertThat(objectMapper.readValue<BehandlingsstatistikkTaskPayload>(taskSlot[2].payload).hendelse)
                .isEqualTo(Hendelse.BESLUTTET)
        verify(exactly = 1) { behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.INNVILGET) }
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
        return beslutteVedtakSteg.utførOgReturnerNesteSteg(Behandling(id = behandlingId,
                                                                      fagsakId = fagsak.id,
                                                                      type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                      status = BehandlingStatus.FATTER_VEDTAK,
                                                                      steg = beslutteVedtakSteg.stegType(),
                                                                      resultat = BehandlingResultat.IKKE_SATT,
                                                                      årsak = BehandlingÅrsak.SØKNAD),
                                                           BeslutteVedtakDto(godkjent = godkjent))
    }
}