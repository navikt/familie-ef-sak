package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.gui.VedtakControllerTest.Saksbehandler.BESLUTTER
import no.nav.familie.ef.sak.api.gui.VedtakControllerTest.Saksbehandler.BESLUTTER_2
import no.nav.familie.ef.sak.api.gui.VedtakControllerTest.Saksbehandler.SAKSBEHANDLER
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.infrastruktur.config.JsonMapperProvider
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppfølgingsoppgave.OppfølgingsoppgaveService
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.SendTilBeslutterDto
import no.nav.familie.ef.sak.vedtak.dto.TotrinnkontrollStatus
import no.nav.familie.ef.sak.vedtak.dto.TotrinnskontrollStatusDto
import no.nav.familie.ef.sak.vedtak.dto.ÅrsakUnderkjent
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.AvslagÅrsak
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.exchange
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class VedtakControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @Autowired
    private lateinit var vedtaksbrevService: VedtaksbrevService

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var søknadService: SøknadService

    @Autowired
    private lateinit var grunnlagsdataService: GrunnlagsdataService

    @Autowired
    private lateinit var oppgaveRepository: OppgaveRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var oppgaveService: OppgaveService

    @Autowired
    private lateinit var behandlingshistorikkService: BehandlingshistorikkService

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var oppfølgingsoppgaveService: OppfølgingsoppgaveService

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)
    private val saksbehandling = saksbehandling(fagsak, behandling)

    private enum class Saksbehandler(
        val beslutter: Boolean = false,
    ) {
        SAKSBEHANDLER,
        BESLUTTER(true),
        BESLUTTER_2(true),
    }

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
    }

    @Test
    internal fun `totrinn er uaktuell når behandlingen ikke er klar for totrinn`() {
        opprettBehandling(steg = StegType.VILKÅR)
        validerTotrinnskontrollUaktuelt(BESLUTTER)
    }

    @Test
    internal fun `skal sette behandling til fatter vedtak når man sendt til beslutter`() {
        opprettBehandling()

        sendTilBeslutter(SAKSBEHANDLER)
        validerBehandlingFatterVedtak()
    }

    @Test
    internal fun `skal kaste feil ved innvilgelse hvis vilkårsvurderinger mangler`() {
        val behandlingId = opprettBehandling(vedtakResultatType = ResultatType.INNVILGE)
        lagVilkårsvurderinger(behandlingId, ikkeLag = 1)

        val exception =
            assertThrows<HttpClientErrorException.BadRequest> {
                sendTilBeslutter(SAKSBEHANDLER)
            }
        val ressurs = JsonMapperProvider.jsonMapper.readValue(exception.responseBodyAsString, Ressurs::class.java)
        assertThat(ressurs.frontendFeilmelding).isEqualTo("Kan ikke innvilge hvis ikke alle vilkår er oppfylt for behandlingId: $behandlingId")
    }

    @Test
    internal fun `skal kaste feil ved innvilgelse hvis en ikke er innvilget`() {
        val behandlingId = opprettBehandling(vedtakResultatType = ResultatType.INNVILGE)
        lagVilkårsvurderinger(behandlingId, Vilkårsresultat.IKKE_OPPFYLT)

        val exception =
            assertThrows<HttpClientErrorException.BadRequest> {
                sendTilBeslutter(SAKSBEHANDLER)
            }
        val ressurs = JsonMapperProvider.jsonMapper.readValue(exception.responseBodyAsString, Ressurs::class.java)
        assertThat(ressurs.frontendFeilmelding).isEqualTo("Kan ikke innvilge hvis ikke alle vilkår er oppfylt for behandlingId: $behandlingId")
    }

    @Test
    internal fun `skal sette behandling til fatter vedtak når man sendt til beslutter ved innvilgelse`() {
        val behandlingId = opprettBehandling(vedtakResultatType = ResultatType.INNVILGE)
        lagVilkårsvurderinger(behandlingId, Vilkårsresultat.OPPFYLT)

        sendTilBeslutter(SAKSBEHANDLER)

        validerBehandlingFatterVedtak()
    }

    @Test
    internal fun `skal sette behandling til iverksett når man har godkjent totrinnskontroll`() {
        val behandlingId = opprettBehandling()

        sendTilBeslutter(SAKSBEHANDLER)
        byttEierPåLagretOppgave(behandlingId, 24681L, 24682L)
        godkjennTotrinnskontroll(BESLUTTER)
        validerBehandlingIverksetter()
    }

    @Test
    internal fun `hvis man underkjenner den så skal man få ut det som status`() {
        val behandlingId = opprettBehandling()

        sendTilBeslutter(SAKSBEHANDLER)

        byttEierPåLagretOppgave(behandlingId, 24681L, 24682L)
        underkjennTotrinnskontroll(BESLUTTER)

        validerTotrinnskontrollUnderkjent(SAKSBEHANDLER)
        validerTotrinnskontrollUnderkjent(BESLUTTER)
        validerTotrinnskontrollUnderkjent(BESLUTTER_2)
    }

    @Test
    internal fun `en annen beslutter enn den som sendte til beslutter må godkjenne behandlingen`() {
        val behandlingId = opprettBehandling(saksbehandler = BESLUTTER)

        sendTilBeslutter(BESLUTTER)
        validerTotrinnskontrollIkkeAutorisert(SAKSBEHANDLER)
        validerTotrinnskontrollIkkeAutorisert(BESLUTTER)
        validerTotrinnskontrollKanFatteVedtak(BESLUTTER_2)

        assertThrows<HttpClientErrorException.BadRequest> {
            godkjennTotrinnskontroll(SAKSBEHANDLER)
        }
        assertThrows<HttpClientErrorException.BadRequest> {
            godkjennTotrinnskontroll(BESLUTTER, responseBadRequest())
        }

        byttEierPåLagretOppgave(behandlingId, 24682L, 24685L)
        godkjennTotrinnskontroll(BESLUTTER_2)
    }

    @Test
    internal fun `skal gi totrinnskontroll uaktuelt hvis totrinnskontrollen er godkjent`() {
        val behandlingId = opprettBehandling()

        sendTilBeslutter(SAKSBEHANDLER)
        byttEierPåLagretOppgave(behandlingId, 24681L, 24682L)
        godkjennTotrinnskontroll(BESLUTTER)

        validerTotrinnskontrollUaktuelt(SAKSBEHANDLER)
        validerTotrinnskontrollUaktuelt(BESLUTTER)
        validerTotrinnskontrollUaktuelt(BESLUTTER_2)
    }

    @Test
    internal fun `hvis man underkjenner behandlingen må man sende den til beslutter på nytt og sen godkjenne den`() {
        val behandlingId = opprettBehandling()
        sendTilBeslutter(SAKSBEHANDLER)

        byttEierPåLagretOppgave(behandlingId, 24681L, 24682L)
        underkjennTotrinnskontroll(BESLUTTER)

        byttEierPåLagretOppgave(behandlingId, 24682L, 24681L)
        sendTilBeslutter(SAKSBEHANDLER)

        byttEierPåLagretOppgave(behandlingId, 24681L, 24682L)
        underkjennTotrinnskontroll(BESLUTTER)

        validerBehandlingUtredes()

        byttEierPåLagretOppgave(behandlingId, 24682L, 24681L)
        sendTilBeslutter(SAKSBEHANDLER)

        byttEierPåLagretOppgave(behandlingId, 24681L, 24682L)
        godkjennTotrinnskontroll(BESLUTTER)
    }

    @Test
    internal fun `en annen beslutter enn den som sendte behandlingen til beslutter må godkjenne behandlingen`() {
        val behandlingId = opprettBehandling(saksbehandler = BESLUTTER)
        sendTilBeslutter(BESLUTTER)
        validerTotrinnskontrollIkkeAutorisert(SAKSBEHANDLER)
        validerTotrinnskontrollIkkeAutorisert(BESLUTTER)
        validerTotrinnskontrollKanFatteVedtak(BESLUTTER_2)

        assertThrows<HttpClientErrorException.BadRequest> {
            godkjennTotrinnskontroll(BESLUTTER)
        }

        byttEierPåLagretOppgave(behandlingId, 24682L, 24685L)
        godkjennTotrinnskontroll(BESLUTTER_2)
    }

    @Test
    internal fun `kan ikke godkjenne totrinnskontroll når behandling utredes`() {
        opprettBehandling()
        val exception =
            assertThrows<HttpClientErrorException.BadRequest> {
                godkjennTotrinnskontroll(BESLUTTER)
            }
        assertThat(exception.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    internal fun `kan ikke sende til besluttning før behandling er i riktig steg`() {
        opprettBehandling(steg = StegType.VILKÅR)

        val exception =
            assertThrows<HttpClientErrorException.BadRequest> {
                godkjennTotrinnskontroll(BESLUTTER)
            }
        assertThat(exception.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    internal fun `kan ikke sende til besluttning som saksbehandler`() {
        opprettBehandling(saksbehandler = BESLUTTER)
        sendTilBeslutter(BESLUTTER)

        assertThrows<HttpClientErrorException.BadRequest> {
            godkjennTotrinnskontroll(SAKSBEHANDLER)
        }
    }

    @Test
    internal fun `skal automatisk utføre besluttesteg når en behandling avslås pga mindre inntektsendringer`() {
        opprettBehandling(
            steg = StegType.SEND_TIL_BESLUTTER,
            vedtakResultatType = ResultatType.AVSLÅ,
            status = BehandlingStatus.UTREDES,
            avlsåÅrsak = AvslagÅrsak.MINDRE_INNTEKTSENDRINGER,
        )
        sendTilBeslutter(SAKSBEHANDLER)

        validerTotrinnskontrollUaktuelt(BESLUTTER)
        validerTotrinnskontrollUaktuelt(SAKSBEHANDLER)
        validerTotrinnskontrollUaktuelt(BESLUTTER_2)

        validerBehandlingIverksetter()
    }

    @Test
    internal fun `skal automatisk utføre besluttesteg når en behandling avslås pga kortvarig avbrudd jobb`() {
        opprettBehandling(
            steg = StegType.SEND_TIL_BESLUTTER,
            vedtakResultatType = ResultatType.AVSLÅ,
            status = BehandlingStatus.UTREDES,
            avlsåÅrsak = AvslagÅrsak.KORTVARIG_AVBRUDD_JOBB,
        )
        sendTilBeslutter(SAKSBEHANDLER)

        validerTotrinnskontrollUaktuelt(BESLUTTER)
        validerTotrinnskontrollUaktuelt(SAKSBEHANDLER)
        validerTotrinnskontrollUaktuelt(BESLUTTER_2)

        validerBehandlingIverksetter()
    }

    @Test
    internal fun `skal lagre oppgaver som skal opprettes`() {
        val lagAndelMedInntekt1ÅrFremITiden: (behandlingId: UUID) -> TilkjentYtelse = { behandlingId ->
            val andel =
                lagAndelTilkjentYtelse(
                    beløp = 100,
                    fraOgMed = LocalDate.now(),
                    tilOgMed = LocalDate.now().plusYears(2),
                    kildeBehandlingId = behandlingId,
                )
            lagTilkjentYtelse(behandlingId = behandlingId, andelerTilkjentYtelse = listOf(andel))
        }

        val behandlingId =
            opprettBehandling(
                steg = StegType.SEND_TIL_BESLUTTER,
                vedtakResultatType = ResultatType.INNVILGE,
                status = BehandlingStatus.UTREDES,
                avlsåÅrsak = AvslagÅrsak.MINDRE_INNTEKTSENDRINGER,
                tilkjentYtelse = lagAndelMedInntekt1ÅrFremITiden,
            )

        lagVilkårsvurderinger(behandlingId, Vilkårsresultat.OPPFYLT)

        sendTilBeslutter(
            SAKSBEHANDLER,
            SendTilBeslutterDto(
                oppgavetyperSomSkalOpprettes = listOf(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID),
            ),
        )

        assertThat(oppfølgingsoppgaveService.hentOppgaverForOpprettelseEllerNull(behandlingId)?.oppgavetyper)
            .containsExactly(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)
    }

    @Nested
    inner class AngreSendTilBeslutter {
        @BeforeEach
        fun setUp() {
            mockBrukerContext(SAKSBEHANDLER.name)
        }

        @AfterEach
        fun tearDown() {
            clearBrukerContext()
        }

        @Test
        internal fun `skal feile hvis en annen saksbehandler prøver å angre send til beslutter`() {
            opprettBehandling(steg = StegType.SEND_TIL_BESLUTTER, status = BehandlingStatus.UTREDES)
            sendTilBeslutter(SAKSBEHANDLER)
            assertThrows<HttpClientErrorException.BadRequest> {
                angreSendTilBeslutter(BESLUTTER)
            }
        }

        @Test
        internal fun `skal feile hvis vedtak ikke er i steg BESLUTTE_VEDTAK`() {
            opprettBehandling(steg = StegType.SEND_TIL_BESLUTTER, status = BehandlingStatus.FATTER_VEDTAK)
            behandlingshistorikkService.opprettHistorikkInnslag(
                Behandlingshistorikk(
                    behandlingId = behandling.id,
                    steg = StegType.SEND_TIL_BESLUTTER,
                    opprettetAv = SAKSBEHANDLER.name,
                ),
            )
            opprettOppgave(oppgaveType = Oppgavetype.GodkjenneVedtak, sakshandler = BESLUTTER)

            assertThrows<HttpClientErrorException.BadRequest> {
                angreSendTilBeslutter(SAKSBEHANDLER)
            }
        }

        @Test
        internal fun `skal feile hvis oppgave er plukket av noen andre`() {
            opprettBehandling(steg = StegType.SEND_TIL_BESLUTTER, status = BehandlingStatus.UTREDES)
            sendTilBeslutter(SAKSBEHANDLER)
            opprettOppgave(oppgaveType = Oppgavetype.GodkjenneVedtak, sakshandler = BESLUTTER)
            assertThrows<HttpClientErrorException.BadRequest> {
                angreSendTilBeslutter(SAKSBEHANDLER)
            }
        }

        @Test
        internal fun `skal kunne angre send til beslutter`() {
            val behandlingId = opprettBehandling(steg = StegType.SEND_TIL_BESLUTTER, status = BehandlingStatus.UTREDES)
            opprettOppgave(oppgaveType = Oppgavetype.GodkjenneVedtak)
            sendTilBeslutter(SAKSBEHANDLER)

            val behandlingFørAngre = behandlingService.hentBehandling(behandlingId)
            assertThat(behandlingFørAngre.steg == StegType.BESLUTTE_VEDTAK)
            assertThat(behandlingFørAngre.status == BehandlingStatus.FATTER_VEDTAK)

            angreSendTilBeslutter(SAKSBEHANDLER, responseOK())

            val oppdatertBehandling = behandlingService.hentBehandling(behandlingId)
            assertThat(oppdatertBehandling.steg == StegType.SEND_TIL_BESLUTTER)
            assertThat(oppdatertBehandling.status == BehandlingStatus.UTREDES)

            val historikk = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId)
            assertThat(historikk.utfall == StegUtfall.ANGRE_SEND_TIL_BESLUTTER)

            val gjeldendeTasks =
                taskService.findAll().filter { task -> task.metadata["behandlingId"] == behandlingId.toString() }
            assertThat(
                gjeldendeTasks.single { task ->
                    task.type == FerdigstillOppgaveTask.TYPE && task.metadata["oppgavetype"] == "GodkjenneVedtak"
                },
            ).isNotNull
            assertThat(
                gjeldendeTasks.single { task ->
                    task.type == OpprettOppgaveTask.TYPE && task.metadata["oppgavetype"] == "BehandleSak"
                },
            ).isNotNull
        }

        @Test
        internal fun `skal kunne angre send til beslutter når godkjenne vedtak-oppgaven er plukket av saksbehandler`() {
            opprettBehandling(steg = StegType.SEND_TIL_BESLUTTER, status = BehandlingStatus.UTREDES)
            sendTilBeslutter(SAKSBEHANDLER)
            opprettOppgave(oppgaveType = Oppgavetype.GodkjenneVedtak, sakshandler = SAKSBEHANDLER)
            angreSendTilBeslutter(SAKSBEHANDLER, responseOK())
        }
    }

    @Test
    internal fun `Skal kaste feil dersom vedtaket er beregnet med foreldet g-beløp`() {
        val tilkjentYtelseGammelG: (behandlingId: UUID) -> TilkjentYtelse = { behandlingId ->
            val andel =
                lagAndelTilkjentYtelse(
                    beløp = 100,
                    fraOgMed = LocalDate.now(),
                    tilOgMed = LocalDate.now().plusYears(2),
                    kildeBehandlingId = behandlingId,
                )
            lagTilkjentYtelse(behandlingId = behandlingId, andelerTilkjentYtelse = listOf(andel), grunnbeløpsmåned = YearMonth.of(2025, 4))
        }
        val behandlingId = opprettBehandling(steg = StegType.SEND_TIL_BESLUTTER, status = BehandlingStatus.UTREDES, vedtakResultatType = ResultatType.INNVILGE, tilkjentYtelse = tilkjentYtelseGammelG)
        lagVilkårsvurderinger(behandlingId, Vilkårsresultat.OPPFYLT)
        sendTilBeslutter(SAKSBEHANDLER)
        byttEierPåLagretOppgave(behandlingId, 24681L, 24688L)

        val exception =
            assertThrows<HttpClientErrorException.BadRequest> {
                godkjennTotrinnskontroll(BESLUTTER)
            }
        val ressurs = JsonMapperProvider.jsonMapper.readValue(exception.responseBodyAsString, Ressurs::class.java)
        assertThat(ressurs.frontendFeilmelding).isEqualTo("Kan ikke iverksette med utdatert grunnbeløp gyldig fra 2025-04. Denne behandlingen må beregnes og simuleres på nytt")

        val lagretBehandling = behandlingService.hentBehandling(behandlingId)
        assertThat(lagretBehandling.status).isEqualTo(BehandlingStatus.FATTER_VEDTAK)
    }

    private fun opprettBehandling(
        status: BehandlingStatus = BehandlingStatus.UTREDES,
        steg: StegType = StegType.SEND_TIL_BESLUTTER,
        vedtakResultatType: ResultatType = ResultatType.AVSLÅ,
        avlsåÅrsak: AvslagÅrsak = AvslagÅrsak.VILKÅR_IKKE_OPPFYLT,
        tilkjentYtelse: (behandlingId: UUID) -> TilkjentYtelse = {
            tilkjentYtelse(
                behandlingId = it,
                fagsak.hentAktivIdent(),
            )
        },
        saksbehandler: Saksbehandler = SAKSBEHANDLER,
    ): UUID {
        val lagretBehandling =
            behandlingRepository.insert(
                behandling.copy(
                    status = status,
                    steg = steg,
                ),
            )

        vedtakRepository.insert(
            vedtak(lagretBehandling.id, vedtakResultatType).copy(
                avslåÅrsak = avlsåÅrsak,
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            ),
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(lagretBehandling.id))
        søknadService.lagreSøknadForOvergangsstønad(
            Testsøknad.søknadOvergangsstønad,
            lagretBehandling.id,
            fagsak.id,
            "1",
        )
        grunnlagsdataService.opprettGrunnlagsdata(lagretBehandling.id)
        opprettOppgave(lagretBehandling.id, saksbehandler)
        return lagretBehandling.id
    }

    private fun opprettOppgave(
        behandlingId: UUID,
        saksbehandler: Saksbehandler = SAKSBEHANDLER,
    ) {
        val oppgaveId = if (saksbehandler == SAKSBEHANDLER) 24681L else 24682L
        val oppgave =
            Oppgave(
                gsakOppgaveId = oppgaveId,
                behandlingId = behandlingId,
                type = Oppgavetype.BehandleSak,
            )
        oppgaveRepository.insert(oppgave)
    }

    private fun byttEierPåLagretOppgave(
        behandlingId: UUID,
        gammelOppgaveId: Long,
        nyOppgaveId: Long,
    ) {
        val gammelOppgave = oppgaveRepository.findByGsakOppgaveId(gammelOppgaveId) ?: throw IllegalArgumentException("gammel oppgave må finnes")
        oppgaveRepository.delete(gammelOppgave)

        val oppgave =
            Oppgave(
                gsakOppgaveId = nyOppgaveId,
                behandlingId = behandlingId,
                type = Oppgavetype.BehandleSak,
            )
        oppgaveRepository.insert(oppgave)
    }

    private fun opprettOppgave(
        oppgaveType: Oppgavetype = Oppgavetype.GodkjenneVedtak,
        sakshandler: Saksbehandler = SAKSBEHANDLER,
    ) {
        oppgaveService.opprettOppgave(
            oppgavetype = oppgaveType,
            behandlingId = behandling.id,
            tilordnetNavIdent = sakshandler.name,
        )
    }

    private fun <T> responseOK(): (ResponseEntity<Ressurs<T>>) -> Unit =
        {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        }

    private fun <T> responseServerError(): (ResponseEntity<Ressurs<T>>) -> Unit =
        {
            assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }

    private fun <T> responseBadRequest(): (ResponseEntity<Ressurs<T>>) -> Unit =
        {
            assertThat(it.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

    private fun sendTilBeslutter(
        saksbehandler: Saksbehandler,
        request: SendTilBeslutterDto? = null,
        validator: (ResponseEntity<Ressurs<UUID>>) -> Unit = responseOK(),
    ) {
        headers.setBearerAuth(token(saksbehandler))
        lagSaksbehandlerBrev(saksbehandler.name)
        val response =
            restTemplate.exchange<Ressurs<UUID>>(
                localhost("/api/vedtak/${behandling.id}/send-til-beslutter"),
                HttpMethod.POST,
                HttpEntity<Any>(request, headers),
            )
        validator.invoke(response)
    }

    private fun angreSendTilBeslutter(
        saksbehandler: Saksbehandler,
        validator: (ResponseEntity<Ressurs<UUID>>) -> Unit = responseOK(),
    ) {
        headers.setBearerAuth(token(saksbehandler))
        val response =
            restTemplate.exchange<Ressurs<UUID>>(
                localhost("/api/vedtak/${behandling.id}/angre-send-til-beslutter"),
                HttpMethod.POST,
                HttpEntity<Any>(headers),
            )
        validator.invoke(response)
    }

    private fun godkjennTotrinnskontroll(
        saksbehandler: Saksbehandler,
        validator: (ResponseEntity<Ressurs<UUID>>) -> Unit = responseOK(),
    ) {
        beslutteVedtak(saksbehandler, BeslutteVedtakDto(true), validator)
    }

    private fun underkjennTotrinnskontroll(
        saksbehandler: Saksbehandler,
        validator: (ResponseEntity<Ressurs<UUID>>) -> Unit = responseOK(),
    ) {
        beslutteVedtak(
            saksbehandler,
            BeslutteVedtakDto(false, "begrunnelse", listOf(ÅrsakUnderkjent.TIDLIGERE_VEDTAKSPERIODER)),
            validator,
        )
    }

    private fun beslutteVedtak(
        saksbehandler: Saksbehandler,
        beslutteVedtak: BeslutteVedtakDto,
        validator: (ResponseEntity<Ressurs<UUID>>) -> Unit,
    ) {
        headers.setBearerAuth(token(saksbehandler))
        val response =
            restTemplate.exchange<Ressurs<UUID>>(
                localhost("/api/vedtak/${behandling.id}/beslutte-vedtak"),
                HttpMethod.POST,
                HttpEntity(beslutteVedtak, headers),
            )
        validator.invoke(response)
    }

    private fun beslutteVedtakForventFeil(
        saksbehandler: Saksbehandler,
        beslutteVedtak: BeslutteVedtakDto,
        validator: (ResponseEntity<Ressurs<UUID>>) -> Unit,
    ) {
        headers.setBearerAuth(token(saksbehandler))
        val response =
            restTemplate.exchange<Ressurs<UUID>>(
                localhost("/api/vedtak/${behandling.id}/beslutte-vedtak"),
                HttpMethod.POST,
                HttpEntity(beslutteVedtak, headers),
            )
        validator.invoke(response)
    }

    private fun hentTotrinnskontrollStatus(saksbehandler: Saksbehandler): TotrinnskontrollStatusDto {
        headers.setBearerAuth(token(saksbehandler))
        val response =
            restTemplate
                .exchange<Ressurs<TotrinnskontrollStatusDto>>(
                    localhost("/api/vedtak/${behandling.id}/totrinnskontroll"),
                    HttpMethod.GET,
                    HttpEntity<Any>(headers),
                )
        responseOK<TotrinnskontrollStatusDto>().invoke(response)
        return response.body?.data!!
    }

    private fun validerBehandlingUtredes() = validerBehandling(BehandlingStatus.UTREDES, StegType.SEND_TIL_BESLUTTER)

    private fun validerBehandlingIverksetter() = validerBehandling(BehandlingStatus.IVERKSETTER_VEDTAK, StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)

    private fun validerBehandlingFatterVedtak() = validerBehandling(BehandlingStatus.FATTER_VEDTAK, StegType.BESLUTTE_VEDTAK)

    private fun validerBehandling(
        status: BehandlingStatus,
        steg: StegType,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        assertThat(behandling.status).isEqualTo(status)
        assertThat(behandling.steg).isEqualTo(steg)
    }

    private fun validerTotrinnskontrollUaktuelt(saksbehandler: Saksbehandler) {
        val response = hentTotrinnskontrollStatus(saksbehandler)
        assertThat(response.status).isEqualTo(TotrinnkontrollStatus.UAKTUELT)
        assertThat(response.totrinnskontroll).isNull()
    }

    private fun validerTotrinnskontrollIkkeAutorisert(saksbehandler: Saksbehandler) {
        val response = hentTotrinnskontrollStatus(saksbehandler)
        assertThat(response.status).isEqualTo(TotrinnkontrollStatus.IKKE_AUTORISERT)
        assertThat(response.totrinnskontroll).isNotNull
    }

    private fun validerTotrinnskontrollKanFatteVedtak(saksbehandler: Saksbehandler) {
        val response = hentTotrinnskontrollStatus(saksbehandler)
        assertThat(response.status).isEqualTo(TotrinnkontrollStatus.KAN_FATTE_VEDTAK)
        assertThat(response.totrinnskontroll).isNull()
    }

    private fun validerTotrinnskontrollUnderkjent(saksbehandler: Saksbehandler) {
        val response = hentTotrinnskontrollStatus(saksbehandler)
        assertThat(response.status).isEqualTo(TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT)
        assertThat(response.totrinnskontroll).isNotNull
    }

    private fun token(saksbehandler: Saksbehandler): String {
        val rolle = if (saksbehandler.beslutter) rolleConfig.beslutterRolle else rolleConfig.saksbehandlerRolle
        return onBehalfOfToken(role = rolle, saksbehandler = saksbehandler.name)
    }

    private fun lagSaksbehandlerBrev(saksbehandlerSignatur: String) {
        val brevRequest = jsonMapper.readTree("123")
        mockBrukerContext(saksbehandlerSignatur)
        val saksbehandling = behandlingService.hentSaksbehandling(saksbehandling.id)
        vedtaksbrevService.lagSaksbehandlerSanitybrev(saksbehandling, brevRequest, "brevMal")
        clearBrukerContext()
    }

    private fun lagVilkårsvurderinger(
        behandlingId: UUID,
        resultat: Vilkårsresultat = Vilkårsresultat.OPPFYLT,
        ikkeLag: Int = 0,
    ) {
        val vilkårsvurderinger =
            VilkårType
                .hentVilkårForStønad(OVERGANGSSTØNAD)
                .map {
                    vilkårsvurdering(
                        behandlingId = behandlingId,
                        resultat = resultat,
                        type = it,
                        delvilkårsvurdering = listOf(),
                    )
                }.dropLast(ikkeLag)
        vilkårsvurderingRepository.insertAll(vilkårsvurderinger)
    }
}
