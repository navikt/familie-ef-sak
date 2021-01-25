package no.nav.familie.ef.sak.api.gui

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.api.dto.TotrinnkontrollStatus
import no.nav.familie.ef.sak.api.dto.TotrinnskontrollStatusDto
import no.nav.familie.ef.sak.api.gui.VedtakControllerTest.Saksbehandler.*
import no.nav.familie.ef.sak.config.RolleConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.test.JwkGenerator
import no.nav.security.token.support.test.JwtTokenGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.*

internal class VedtakControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var rolleConfig: RolleConfig

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
        opprettBehandling()
        validerTotrinnskontrollUaktuelt(SAKSBEHANDLER)
    }

    @Test
    internal fun `godkjenner vedtak`() {
        opprettBehandling()

        sendTilBeslutter(SAKSBEHANDLER)
        validerBehandlingFatterVedtak()

        validerTotrinnskontrollIkkeAutorisert(SAKSBEHANDLER)
        validerTotrinnskontrollKanFatteVedtak(BESLUTTER)
        godkjennTotrinnskontroll(BESLUTTER)

        validerBehandlingIverksetter()

        validerTotrinnskontrollUaktuelt(SAKSBEHANDLER)
    }

    @Test
    internal fun `underkjenner vedtak og senere godkjenner det`() {
        opprettBehandling()
        sendTilBeslutter(SAKSBEHANDLER)
        validerBehandlingFatterVedtak()
        validerTotrinnskontrollIkkeAutorisert(SAKSBEHANDLER)

        underkjennTotrinnskontroll(BESLUTTER)
        validerBehandlingSendTilBeslutter()
        validerTotrinnskontrollUnderkjent(SAKSBEHANDLER)
        sendTilBeslutter(SAKSBEHANDLER)
        validerBehandlingFatterVedtak()

        godkjennTotrinnskontroll(BESLUTTER)
        validerBehandlingFatterVedtak()

        validerTotrinnskontrollUaktuelt(SAKSBEHANDLER)
        validerTotrinnskontrollUaktuelt(BESLUTTER)
    }

    @Test
    internal fun `beslutter saksbehandler behandling, en annen beslutter må godkjenne totrinnskontroll`() {
        opprettBehandling()
        sendTilBeslutter(BESLUTTER)
        validerBehandlingFatterVedtak()
        validerTotrinnskontrollIkkeAutorisert(SAKSBEHANDLER)
        validerTotrinnskontrollIkkeAutorisert(BESLUTTER)

        godkjennTotrinnskontroll(BESLUTTER) {
            assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR) // TODO burde man returnere noe annet?
        }

        godkjennTotrinnskontroll(BESLUTTER_2)

        validerBehandlingIverksetter()

        validerTotrinnskontrollUaktuelt(SAKSBEHANDLER)
        validerTotrinnskontrollUaktuelt(BESLUTTER)
        validerTotrinnskontrollUaktuelt(BESLUTTER_2)
    }

    @Test
    internal fun `kan ikke beslutte før behandling er i riktig steg`() {
        opprettBehandling()
        godkjennTotrinnskontroll(BESLUTTER) {
            assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Test
    internal fun `kan ikke sende til besluttning før behandling er i riktig steg`() {
        opprettBehandling(steg = StegType.VILKÅRSVURDERE_INNGANGSVILKÅR)
        godkjennTotrinnskontroll(BESLUTTER) {
            assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun opprettBehandling(status: BehandlingStatus = BehandlingStatus.UTREDES,
                                  steg: StegType = StegType.SEND_TIL_BESLUTTER) {
        behandlingRepository.insert(behandling.copy(status = status,
                                                    steg = steg))
    }

    private fun <T : Any> responseSuccessValidator(): (ResponseEntity<Ressurs<T>>) -> Unit = {
        assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(it.body.status).isEqualTo(Ressurs.Status.SUKSESS)
    }

    private fun sendTilBeslutter(saksbehandler: Saksbehandler,
                                 validator: (ResponseEntity<Ressurs<UUID>>) -> Unit = responseSuccessValidator()) {
        headers.setBearerAuth(token(saksbehandler))
        val response = restTemplate.exchange<Ressurs<UUID>>(localhost("/api/vedtak/${behandling.id}/send-til-beslutter"),
                                                            HttpMethod.POST,
                                                            HttpEntity<Any>(headers))
        validator.invoke(response)
    }

    private fun godkjennTotrinnskontroll(saksbehandler: Saksbehandler,
                                         validator: (ResponseEntity<Ressurs<UUID>>) -> Unit = responseSuccessValidator()) {
        beslutteVedtak(saksbehandler, BeslutteVedtakDto(true), validator)
    }

    private fun underkjennTotrinnskontroll(saksbehandler: Saksbehandler,
                                           validator: (ResponseEntity<Ressurs<UUID>>) -> Unit = responseSuccessValidator()) {
        beslutteVedtak(saksbehandler, BeslutteVedtakDto(false, "begrunnelse"), validator)
    }

    private fun beslutteVedtak(saksbehandler: Saksbehandler, beslutteVedtak: BeslutteVedtakDto,
                               validator: (ResponseEntity<Ressurs<UUID>>) -> Unit) {
        headers.setBearerAuth(token(saksbehandler))
        val response = restTemplate.exchange<Ressurs<UUID>>(localhost("/api/vedtak/${behandling.id}/beslutte-vedtak"),
                                                            HttpMethod.POST,
                                                            HttpEntity(beslutteVedtak, headers))
        validator.invoke(response)
    }

    private fun hentTotrinnskontrollStatus(saksbehandler: Saksbehandler): TotrinnskontrollStatusDto {
        headers.setBearerAuth(token(saksbehandler))
        val response =
                restTemplate.exchange<Ressurs<TotrinnskontrollStatusDto>>(localhost("/api/vedtak/${behandling.id}/totrinnskontroll"),
                                                                          HttpMethod.GET,
                                                                          HttpEntity<Any>(headers))
        responseSuccessValidator<TotrinnskontrollStatusDto>().invoke(response)
        return response.body.data!!
    }

    private fun validerBehandlingIverksetter() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        assertThat(behandling.status).isEqualTo(BehandlingStatus.IVERKSETTER_VEDTAK)
        assertThat(behandling.steg).isEqualTo(StegType.IVERKSETT_MOT_OPPDRAG)
    }

    private fun validerBehandlingFatterVedtak() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        assertThat(behandling.status).isEqualTo(BehandlingStatus.FATTER_VEDTAK)
        assertThat(behandling.steg).isEqualTo(StegType.BESLUTTE_VEDTAK)
    }

    private fun validerBehandlingSendTilBeslutter() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        assertThat(behandling.status).isEqualTo(BehandlingStatus.UTREDES)
        assertThat(behandling.steg).isEqualTo(StegType.SEND_TIL_BESLUTTER)
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
        var claimsSet = JwtTokenGenerator.createSignedJWT("subject").jwtClaimsSet // ??
        claimsSet = JWTClaimsSet.Builder(claimsSet)
                .claim("preferred_username", saksbehandler)
                .claim("groups", listOf(rolle))
                .build()
        val createSignedJWT = JwtTokenGenerator.createSignedJWT(JwkGenerator.getDefaultRSAKey(), claimsSet)
        return createSignedJWT.serialize()
    }

}
