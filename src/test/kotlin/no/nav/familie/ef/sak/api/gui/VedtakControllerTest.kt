package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.gui.VedtakControllerTest.Saksbehandler.BESLUTTER
import no.nav.familie.ef.sak.api.gui.VedtakControllerTest.Saksbehandler.BESLUTTER_2
import no.nav.familie.ef.sak.api.gui.VedtakControllerTest.Saksbehandler.SAKSBEHANDLER
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.AVSLÅTT
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.TotrinnkontrollStatus
import no.nav.familie.ef.sak.vedtak.dto.TotrinnskontrollStatusDto
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.UUID

internal class VedtakControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var rolleConfig: RolleConfig
    @Autowired private lateinit var vedtaksbrevService: VedtaksbrevService
    @Autowired private lateinit var vedtakRepository: VedtakRepository
    @Autowired private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository
    @Autowired private lateinit var søknadService: SøknadService
    @Autowired private lateinit var grunnlagsdataService: GrunnlagsdataService


    private val fagsak = fagsak(setOf(FagsakPerson("")))
    private val behandling = behandling(fagsak)

    private enum class Saksbehandler(val beslutter: Boolean = false) {
        SAKSBEHANDLER,
        BESLUTTER(true),
        BESLUTTER_2(true)
    }

    @BeforeEach
    internal fun setUp() {
        fagsakRepository.insert(fagsak)
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
    internal fun `skal sette behandling til iverksett når man godkjent totrinnskontroll`() {
        opprettBehandling()

        sendTilBeslutter(SAKSBEHANDLER)
        godkjennTotrinnskontroll(BESLUTTER)
        validerBehandlingIverksetter()
    }

    @Test
    internal fun `hvis man underkjenner den så skal man få ut det som status`() {
        opprettBehandling()

        sendTilBeslutter(SAKSBEHANDLER)
        underkjennTotrinnskontroll(BESLUTTER)
        validerTotrinnskontrollUnderkjent(SAKSBEHANDLER)
        validerTotrinnskontrollUnderkjent(BESLUTTER)
        validerTotrinnskontrollUnderkjent(BESLUTTER_2)
    }

    @Test
    internal fun `en annen beslutter enn den som sendte til beslutter må godkjenne behandlingen`() {
        opprettBehandling()

        sendTilBeslutter(BESLUTTER)
        validerTotrinnskontrollIkkeAutorisert(SAKSBEHANDLER)
        validerTotrinnskontrollIkkeAutorisert(BESLUTTER)
        validerTotrinnskontrollKanFatteVedtak(BESLUTTER_2)

        godkjennTotrinnskontroll(SAKSBEHANDLER, responseServerError())
        godkjennTotrinnskontroll(BESLUTTER, responseServerError())
        godkjennTotrinnskontroll(BESLUTTER_2)
    }

    @Test
    internal fun `skal gi totrinnskontroll uaktuelt hvis totrinnskontrollen er godkjent`() {
        opprettBehandling()

        sendTilBeslutter(SAKSBEHANDLER)
        godkjennTotrinnskontroll(BESLUTTER)

        validerTotrinnskontrollUaktuelt(SAKSBEHANDLER)
        validerTotrinnskontrollUaktuelt(BESLUTTER)
        validerTotrinnskontrollUaktuelt(BESLUTTER_2)
    }


    @Test
    internal fun `hvis man underkjenner behandlingen må man sende den til beslutter på nytt og sen godkjenne den`() {
        opprettBehandling()
        sendTilBeslutter(SAKSBEHANDLER)
        underkjennTotrinnskontroll(BESLUTTER)

        sendTilBeslutter(SAKSBEHANDLER)
        underkjennTotrinnskontroll(BESLUTTER)

        validerBehandlingUtredes()

        sendTilBeslutter(SAKSBEHANDLER)
        godkjennTotrinnskontroll(BESLUTTER)
    }

    @Test
    internal fun `en annen beslutter enn den som sendte behandlingen til beslutter må godkjenne behandlingen`() {
        opprettBehandling()
        sendTilBeslutter(BESLUTTER)
        validerTotrinnskontrollIkkeAutorisert(SAKSBEHANDLER)
        validerTotrinnskontrollIkkeAutorisert(BESLUTTER)
        validerTotrinnskontrollKanFatteVedtak(BESLUTTER_2)

        godkjennTotrinnskontroll(BESLUTTER, responseServerError())

        godkjennTotrinnskontroll(BESLUTTER_2)
    }

    @Test
    internal fun `kan ikke godkjenne totrinnskontroll når behandling utredes`() {
        opprettBehandling()
        godkjennTotrinnskontroll(BESLUTTER) {
            assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Test
    internal fun `kan ikke sende til besluttning før behandling er i riktig steg`() {
        opprettBehandling(steg = StegType.VILKÅR)
        godkjennTotrinnskontroll(BESLUTTER) {
            assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun opprettBehandling(status: BehandlingStatus = BehandlingStatus.UTREDES,
                                  steg: StegType = StegType.SEND_TIL_BESLUTTER, resultat: BehandlingResultat = AVSLÅTT) {

        val lagretBehandling = behandlingRepository.insert(behandling.copy(status = status,
                                                                           steg = steg,
                                                                           resultat = resultat))
        vedtakRepository.insert(vedtak(lagretBehandling.id, ResultatType.AVSLÅ))
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingId = lagretBehandling.id, fagsak.hentAktivIdent()))
        søknadService.lagreSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, lagretBehandling.id, fagsak.id, "1")
        grunnlagsdataService.opprettGrunnlagsdata(lagretBehandling.id)

    }

    private fun <T> responseOK(): (ResponseEntity<Ressurs<T>>) -> Unit = {
        assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(it.body.status).isEqualTo(Ressurs.Status.SUKSESS)
    }

    private fun <T> responseServerError(): (ResponseEntity<Ressurs<T>>) -> Unit = {
        assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private fun sendTilBeslutter(saksbehandler: Saksbehandler,
                                 validator: (ResponseEntity<Ressurs<UUID>>) -> Unit = responseOK()) {
        headers.setBearerAuth(token(saksbehandler))
        lagSaksbehandlerBrev()
        val response = restTemplate.exchange<Ressurs<UUID>>(localhost("/api/vedtak/${behandling.id}/send-til-beslutter"),
                                                            HttpMethod.POST,
                                                            HttpEntity<Any>(headers))
        validator.invoke(response)
    }

    private fun godkjennTotrinnskontroll(saksbehandler: Saksbehandler,
                                         validator: (ResponseEntity<Ressurs<UUID>>) -> Unit = responseOK()) {
        beslutteVedtak(saksbehandler, BeslutteVedtakDto(true), validator)
    }

    private fun underkjennTotrinnskontroll(saksbehandler: Saksbehandler,
                                           validator: (ResponseEntity<Ressurs<UUID>>) -> Unit = responseOK()) {
        beslutteVedtak(saksbehandler, BeslutteVedtakDto(false, "begrunnelse"), validator)
    }

    private fun beslutteVedtak(saksbehandler: Saksbehandler, beslutteVedtak: BeslutteVedtakDto,
                               validator: (ResponseEntity<Ressurs<UUID>>) -> Unit) {
        lagBeslutterBrev(saksbehandler.name)
        headers.setBearerAuth(token(saksbehandler))
        val response = restTemplate.exchange<Ressurs<UUID>>(localhost("/api/vedtak/${behandling.id}/beslutte-vedtak"),
                                                            HttpMethod.POST,
                                                            HttpEntity(beslutteVedtak, headers))
        validator.invoke(response)
    }

    private fun hentTotrinnskontrollStatus(saksbehandler: Saksbehandler): TotrinnskontrollStatusDto {
        headers.setBearerAuth(token(saksbehandler))
        val response = restTemplate
                .exchange<Ressurs<TotrinnskontrollStatusDto>>(localhost("/api/vedtak/${behandling.id}/totrinnskontroll"),
                                                              HttpMethod.GET,
                                                              HttpEntity<Any>(headers))
        responseOK<TotrinnskontrollStatusDto>().invoke(response)
        return response.body.data!!
    }

    private fun validerBehandlingUtredes() = validerBehandling(BehandlingStatus.UTREDES, StegType.SEND_TIL_BESLUTTER)

    private fun validerBehandlingIverksetter() =
            validerBehandling(BehandlingStatus.IVERKSETTER_VEDTAK, StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)

    private fun validerBehandlingFatterVedtak() = validerBehandling(BehandlingStatus.FATTER_VEDTAK, StegType.BESLUTTE_VEDTAK)

    private fun validerBehandling(status: BehandlingStatus, steg: StegType) {
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

    private fun lagSaksbehandlerBrev() {
        val brevRequest = objectMapper.readTree("123")
        mockBrukerContext("saksbehandlernavn")
        vedtaksbrevService.lagSaksbehandlerBrev(behandling.id, brevRequest, "brevMal")
        clearBrukerContext()
    }

    private fun lagBeslutterBrev(beslutter: String) {
        mockBrukerContext(beslutter)
        try {
            vedtaksbrevService.lagBeslutterBrev(behandling.id)
        } catch (e: Feil) {
            // Ønsker ikke å kaste feil fra denne hvis det eks er "feil steg", feil steg ønsker vi å teste i beslutteVedtak
        }
        clearBrukerContext()
    }

}
