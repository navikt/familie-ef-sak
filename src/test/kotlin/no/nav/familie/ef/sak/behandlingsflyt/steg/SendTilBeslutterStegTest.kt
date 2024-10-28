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
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.INNVILGET
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseService
import no.nav.familie.ef.sak.behandling.ÅrsakRevurderingService
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTaskPayload
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask.FerdigstillOppgaveTaskData
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.beregning.ValiderOmregningService
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.BehandlingKategori
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

internal class SendTilBeslutterStegTest {
    private val taskService = mockk<TaskService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    private val vedtakService = mockk<VedtakService>()
    private val simuleringService = mockk<SimuleringService>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>()
    private val vurderingService = mockk<VurderingService>()
    private val validerOmregningService = mockk<ValiderOmregningService>(relaxed = true)
    private val årsakRevurderingService = mockk<ÅrsakRevurderingService>(relaxed = true)
    private val oppgaverForOpprettelseService = mockk<OppgaverForOpprettelseService>(relaxed = true)
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val tilordnetRessursService = mockk<TilordnetRessursService>()
    private val simuleringsoppsummering =
        Simuleringsoppsummering(
            perioder = listOf(),
            fomDatoNestePeriode = null,
            etterbetaling = BigDecimal.ZERO,
            feilutbetaling = BigDecimal.ZERO,
            fom = null,
            tomDatoNestePeriode = null,
            forfallsdatoNestePeriode = null,
            tidSimuleringHentet = null,
            tomSisteUtbetaling = null,
        )

    private val beslutteVedtakSteg =
        SendTilBeslutterSteg(
            taskService,
            fagsakService,
            behandlingService,
            vedtaksbrevRepository,
            vedtakService,
            simuleringService,
            tilbakekrevingService,
            vurderingService,
            validerOmregningService,
            årsakRevurderingService,
            oppgaverForOpprettelseService,
            behandlingshistorikkService,
            tilordnetRessursService,
        )
    private val fagsak =
        fagsak(
            stønadstype = StønadType.OVERGANGSSTØNAD,
            identer = setOf(PersonIdent(ident = "12345678901")),
        )
    private val saksbehandlerNavn = "saksbehandlernavn"
    private val vedtaksbrev =
        Vedtaksbrev(
            behandlingId = UUID.randomUUID(),
            brevmal = "",
            saksbehandlersignatur = saksbehandlerNavn,
            beslutterPdf = null,
            enhet = "enhet",
            saksbehandlerident = saksbehandlerNavn,
            saksbehandlerHtml = "",
        )

    private val behandling =
        saksbehandling(
            fagsak,
            Behandling(
                fagsakId = fagsak.id,
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                status = BehandlingStatus.UTREDES,
                steg = beslutteVedtakSteg.stegType(),
                resultat = BehandlingResultat.IKKE_SATT,
                årsak = BehandlingÅrsak.SØKNAD,
                kategori = BehandlingKategori.NASJONAL,
            ),
        )

    private val revurdering = behandling.copy(type = BehandlingType.REVURDERING, resultat = INNVILGET)

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
            taskService.save(capture(taskSlot))
        } returns Task("", "", Properties())
        every { tilordnetRessursService.hentEFOppgaveSomIkkeErFerdigstilt(any(), any()) } returns
            Oppgave(
                behandlingId = behandling.id,
                gsakOppgaveId = 1234L,
                type = Oppgavetype.BehandleSak,
            )

        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev
        every { vedtaksbrevRepository.update(any()) } returns vedtaksbrev

        every { vurderingService.erAlleVilkårOppfylt(any()) } returns true

        every { vedtaksbrevRepository.existsById(any()) } returns true
        every { simuleringService.hentLagretSimuleringsoppsummering(any()) } returns simuleringsoppsummering

        every { tilbakekrevingService.harSaksbehandlerTattStillingTilTilbakekreving(any()) } returns true
        every { tilbakekrevingService.finnesÅpenTilbakekrevingsBehandling(any()) } returns true
        every { vedtakService.hentVedtak(any()) } returns vedtak(behandling.id)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns
            Behandlingshistorikk(behandlingId = UUID.randomUUID(), steg = StegType.SEND_TIL_BESLUTTER)
        mockBrukerContext(saksbehandlerNavn)
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `Innvilget behandling - alt ok`() {
        val innvilgetBehandling = behandling.copy(resultat = INNVILGET)
        every { vedtakService.hentVedtaksresultat(any()) } returns ResultatType.INNVILGE
        beslutteVedtakSteg.validerSteg(innvilgetBehandling)

        verify(exactly = 1) { årsakRevurderingService.validerHarGyldigRevurderingsinformasjon(any()) }
    }

    @Test
    internal fun `Innvilget behandling - IKKE ok hvis erAlleVilkårOppfylt false`() {
        every { vurderingService.erAlleVilkårOppfylt(any()) } returns false
        val innvilgetBehandling = behandling.copy(resultat = INNVILGET)
        every { vedtakService.hentVedtaksresultat(any()) } returns ResultatType.INNVILGE
        val frontendFeilmelding =
            assertThrows<ApiFeil> { beslutteVedtakSteg.validerSteg(innvilgetBehandling) }.feil
        val forvetetFeilmelding =
            "Kan ikke innvilge hvis ikke alle vilkår er oppfylt for behandlingId: ${innvilgetBehandling.id}"
        assertThat(frontendFeilmelding).isEqualTo(forvetetFeilmelding)
    }

    @Test
    internal fun `Skal kaste feil hvis saksbehandler skulle tatt stilling til tilbakekreving`() {
        // Gitt at vi har feilutbetaling,
        // ikke har sak i tilbakekreving,
        // behandling og vedtak er av relevant type og
        // saksbehandler ikke har tatt stilling til tilbakekrevingsvarsel.
        mockTilbakekrevingValideringsfeil()
        val feil = assertThrows<ApiFeil> { beslutteVedtakSteg.validerSteg(revurdering) }
        assertThat(feil.feil).isEqualTo("Feilutbetaling detektert. Må ta stilling til feilutbetalingsvarsel under simulering")
    }

    @Test
    internal fun `Skal ikke kaste feil hvis ikke det har vært en feilutbetaling`() {
        mockTilbakekrevingValideringsfeil()
        // Gitt at vi IKKE har feilutbetaling,
        every { simuleringService.hentLagretSimuleringsoppsummering(any()) } returns simuleringsoppsummering
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
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), StegType.BESLUTTE_VEDTAK) } returns
            Behandlingshistorikk(
                behandlingId = UUID.randomUUID(),
                steg = StegType.BESLUTTE_VEDTAK,
                utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
            )
        utførOgVerifiserKall(Oppgavetype.BehandleUnderkjentVedtak)
        verifiserVedtattBehandlingsstatistikkTask()
    }

    @Test
    internal fun `Skal kaste feil hvis oppgave med type BehandleUnderkjentVedtak eller BehandleSak ikke finnes`() {
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), StegType.BESLUTTE_VEDTAK) } returns
            Behandlingshistorikk(
                behandlingId = UUID.randomUUID(),
                steg = StegType.BESLUTTE_VEDTAK,
                utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
            )
        every { tilordnetRessursService.hentEFOppgaveSomIkkeErFerdigstilt(any(), any()) } returns null
        every { vedtakService.hentVedtaksresultat(any()) } returns ResultatType.INNVILGE
        val feil = assertThrows<Feil> { beslutteVedtakSteg.validerSteg(behandling) }
        assertThat(feil.frontendFeilmelding).contains("Oppgaven for behandlingen er ikke tilgjengelig. Vennligst vent og prøv igjen om litt.")
    }

    @Test
    internal fun `Skal feile hvis saksbehandlersignatur i vedtaksbrev er ulik saksbehandleren som sendte til beslutter`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(saksbehandlersignatur = "Saksbehandler A")
        every { vedtakService.hentVedtaksresultat(any()) } returns ResultatType.INNVILGE
        mockBrukerContext("Saksbehandler B")

        assertThrows<ApiFeil> { beslutteVedtakSteg.validerSteg(behandling) }
    }

    @Test
    internal fun `skal ikke hente brev når man håndterer behandling med årsak korrigering uten brev`() {
        every { vedtakService.hentVedtaksresultat(any()) } returns ResultatType.INNVILGE
        val behandling = behandling.copy(årsak = BehandlingÅrsak.KORRIGERING_UTEN_BREV)

        beslutteVedtakSteg.validerSteg(behandling)

        verify(exactly = 0) {
            vedtaksbrevRepository.findByIdOrNull(any())
        }
    }

    @Test
    internal fun `Sjekker at beslutter ident blir med i opprett oppgave task`() {
        val beslutterIdent = "B123456"
        every { vedtakService.oppdaterSaksbehandler(any(), any()) } just Runs
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), StegType.BESLUTTE_VEDTAK) } returns
            Behandlingshistorikk(
                behandlingId = UUID.randomUUID(),
                steg = StegType.BESLUTTE_VEDTAK,
                utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
                opprettetAv = beslutterIdent,
                opprettetAvNavn = "beslutterNavn",
            )

        utførSteg()

        assertThat(taskSlot[0].type).isEqualTo(OpprettOppgaveTask.TYPE)
        assertThat(taskSlot[0].payload).contains(beslutterIdent)
    }

    @Test
    internal fun `beslutter ident blir ikke med i opprett oppgave task hvis underkjent for over en mnd siden`() {
        val beslutterIdent = "B123456"
        every { vedtakService.oppdaterSaksbehandler(any(), any()) } just Runs
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), StegType.BESLUTTE_VEDTAK) } returns
            Behandlingshistorikk(
                behandlingId = UUID.randomUUID(),
                steg = StegType.BESLUTTE_VEDTAK,
                utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
                opprettetAv = beslutterIdent,
                opprettetAvNavn = "beslutterNavn",
                endretTid = LocalDateTime.now().minusMonths(1).minusDays(1),
            )

        utførSteg()

        assertThat(taskSlot[0].type).isEqualTo(OpprettOppgaveTask.TYPE)
        assertThat(taskSlot[0].payload).doesNotContain(beslutterIdent)
    }

    @Test
    internal fun `Første gangen finnes det ikke beslutter ident i behandlingsshistorikk`() {
        every { vedtakService.oppdaterSaksbehandler(any(), any()) } just Runs
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), StegType.BESLUTTE_VEDTAK) } returns null

        utførSteg()

        assertThat(taskSlot[0].type).isEqualTo(OpprettOppgaveTask.TYPE)
        val tilordnetNavIdent = objectMapper.readValue<OpprettOppgaveTaskData>(taskSlot[0].payload).tilordnetNavIdent
        assertThat(tilordnetNavIdent).isNull()
    }

    private fun verifiserVedtattBehandlingsstatistikkTask() {
        assertThat(taskSlot[2].type).isEqualTo(BehandlingsstatistikkTask.TYPE)
        assertThat(objectMapper.readValue<BehandlingsstatistikkTaskPayload>(taskSlot[2].payload).hendelse)
            .isEqualTo(Hendelse.VEDTATT)
    }

    private fun utførOgVerifiserKall(oppgavetype: Oppgavetype) {
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
        // tilbakekrevingService.
        every { vedtakService.hentVedtaksresultat(any()) } returns ResultatType.INNVILGE
        every { simuleringService.hentLagretSimuleringsoppsummering(any()) } returns
            simuleringsoppsummering.copy(
                feilutbetaling =
                    BigDecimal(
                        1000,
                    ),
            )

        every { tilbakekrevingService.harSaksbehandlerTattStillingTilTilbakekreving(any()) } returns false
        every { tilbakekrevingService.finnesÅpenTilbakekrevingsBehandling(any()) } returns false
    }
}
