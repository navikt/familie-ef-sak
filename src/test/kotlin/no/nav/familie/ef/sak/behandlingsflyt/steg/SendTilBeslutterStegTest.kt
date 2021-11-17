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
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Properties
import java.util.UUID

internal class SendTilBeslutterStegTest {

    private val taskRepository = mockk<TaskRepository>()
    private val fagsakService = mockk<FagsakService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    private val vedtakService = mockk<VedtakService>()
    private val simuleringService = mockk<SimuleringService>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>()
    private val simuleringsoppsummering = Simuleringsoppsummering(perioder = listOf(),
                                                                  fomDatoNestePeriode = null,
                                                                  etterbetaling = BigDecimal.ZERO,
                                                                  feilutbetaling = BigDecimal.ZERO,
                                                                  fom = null,
                                                                  tomDatoNestePeriode = null,
                                                                  forfallsdatoNestePeriode = null,
                                                                  tidSimuleringHentet = null,
                                                                  tomSisteUtbetaling = null)

    private val beslutteVedtakSteg =
            SendTilBeslutterSteg(taskRepository,
                                 oppgaveService,
                                 fagsakService,
                                 behandlingService,
                                 vedtaksbrevRepository,
                                 vedtakService,
                                 simuleringService,
                                 tilbakekrevingService)
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

    private val revurdering = behandling.copy(type = BehandlingType.REVURDERING)

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
    internal fun `Skal kaste feil hvis saksbehandler skulle tatt stilling til tilbakekreving`() {
        // Gitt at vi har feilutbetaling,
        // ikke har sak i tilbakekreving,
        // behandling og vedtak er av relevant type og
        // saksbehandler ikke har tatt stilling til tilbakekrevingsvarsel.
        mockTilbakekrevingValideringsfeil()
        val feil = assertThrows<Feil> { beslutteVedtakSteg.validerSteg(revurdering) }
        assertThat(feil.frontendFeilmelding).isEqualTo("Feilutbetaling detektert. Må ta stilling til feilutbetalingsvarsel under simulering")
    }

    @Test
    internal fun `Skal ikke kaste feil hvis ikke det har vært en feilutbetaling`() {
        mockTilbakekrevingValideringsfeil()
        // Gitt at vi IKKE har feilutbetaling,
        every { simuleringService.hentLagretSimuleringsresultat(any()) } returns simuleringsoppsummering
        beslutteVedtakSteg.validerSteg(revurdering)
    }

    @Test
    internal fun `Skal ikke kaste feil hvis saksbehandler har tatt stilling til tilbakekreving`() {
        mockTilbakekrevingValideringsfeil()
        every { tilbakekrevingService.harSaksbehandlerTattStillingTilTilbakekreving(any()) } returns true
        beslutteVedtakSteg.validerSteg(revurdering)
    }

    @Test
    internal fun `Skal ikke kaste feil når det finnes åpen sak i tilbakekrevings app`() {
        mockTilbakekrevingValideringsfeil()
        every { tilbakekrevingService.finnesÅpenTilbakekrevingsBehandling(any()) } returns true
        beslutteVedtakSteg.validerSteg(revurdering)
    }

    @Test
    internal fun `Skal avslutte oppgave BehandleSak hvis den finnes`() {
        utførOgVerifiserKall(Oppgavetype.BehandleSak)
        verifiserVedtattBehandlingsstatistikkTask()
    }

    @Test
    internal fun `Skal avslutte oppgave BehandleUnderkjentVedtak hvis den finnes`() {
        utførOgVerifiserKall(Oppgavetype.BehandleUnderkjentVedtak)
        verifiserVedtattBehandlingsstatistikkTask()
    }

    private fun verifiserVedtattBehandlingsstatistikkTask() {
        assertThat(taskSlot[2].type).isEqualTo(BehandlingsstatistikkTask.TYPE)
        assertThat(objectMapper.readValue<BehandlingsstatistikkTaskPayload>(taskSlot[2].payload).hendelse)
                .isEqualTo(Hendelse.VEDTATT)
    }


    private fun utførOgVerifiserKall(oppgavetype: Oppgavetype) {
        every { oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, any()) }
                .returns(Oppgave(id = UUID.randomUUID(),
                                 behandlingId = behandling.id,
                                 gsakOppgaveId = 123L,
                                 type = Oppgavetype.BehandleSak,
                                 erFerdigstilt = false))

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

    // Mock feilutbetaling,
    // ikke har sak i tilbakekreving,
    // behandling og vedtak er av relevant type og
    // saksbehandler ikke har tatt stilling til tilbakekrevingsvarsel.
    private fun mockTilbakekrevingValideringsfeil() {
        every { vedtaksbrevRepository.existsById(any()) } returns true
        // tilbakekrevingService.
        every { vedtakService.hentVedtak(any()) } returns Vedtak(
                resultatType = ResultatType.INNVILGE,
                behandlingId = revurdering.id,
                periodeBegrunnelse = null,
                inntektBegrunnelse = null,
                avslåBegrunnelse = null,
                perioder = null,
                inntekter = null,
                saksbehandlerIdent = null,
                opphørFom = null,
                beslutterIdent = null,
        )
        every { simuleringService.hentLagretSimuleringsresultat(any()) } returns simuleringsoppsummering.copy(feilutbetaling = BigDecimal(
                1000))

        every { tilbakekrevingService.harSaksbehandlerTattStillingTilTilbakekreving(any()) } returns false
        every { tilbakekrevingService.finnesÅpenTilbakekrevingsBehandling(any()) } returns false
    }

}
