package no.nav.familie.ef.sak.no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.dto.FinnOppgaveRequestDto
import no.nav.familie.ef.sak.oppgave.dto.OppgaveResponseDto
import no.nav.familie.ef.sak.oppgave.dto.SaksbehandlerDto
import no.nav.familie.ef.sak.oppgave.dto.SaksbehandlerRolle
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import java.util.UUID
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType

internal class OppgaveControllerIntegrasjonsTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var oppgaveRepository: OppgaveRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal feile hvis personIdenten ikke finnes i pdl`() {
        val response = søkOppgave("19117313797")
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.FUNKSJONELL_FEIL)
        assertThat(response.body?.frontendFeilmelding).isEqualTo("Finner ingen personer for valgt personident")
    }

    @Test
    internal fun `Skal hente ansvarlig saksbehandler for behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        oppgaveRepository.insert(Oppgave(behandlingId = behandling.id, gsakOppgaveId = 24684L, type = Oppgavetype.BehandleSak))

        val response = hentAnsvarligSaksbehandler(behandling.id)

        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(response.body?.data?.fornavn).isEqualTo("julenissen")
        assertThat(response.body?.data?.etternavn).isEqualTo("Saksbehandlersen")
        assertThat(response.body?.data?.rolle).isEqualTo(SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER)
    }

    @Test
    internal fun `Skal returnere saksbehandlerRolle ANNEN SAKSBEHANDLER dersom behandling har annen ansvarlig saksbehandler`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        oppgaveRepository.insert(Oppgave(behandlingId = behandling.id, gsakOppgaveId = 24682L, type = Oppgavetype.BehandleSak))

        val response = hentAnsvarligSaksbehandler(behandling.id)

        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(response.body?.data?.fornavn).isEqualTo("julenissen")
        assertThat(response.body?.data?.etternavn).isEqualTo("Saksbehandlersen")
        assertThat(response.body?.data?.rolle).isEqualTo(SaksbehandlerRolle.ANNEN_SAKSBEHANDLER)
    }

    @Test
    internal fun `Skal returnere saksbehandlerRolle IKKE SATT dersom behandling ikke har ansvarlig saksbehandler`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        oppgaveRepository.insert(Oppgave(behandlingId = behandling.id, gsakOppgaveId = 24683L, type = Oppgavetype.BehandleSak))

        val response = hentAnsvarligSaksbehandler(behandling.id)

        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(response.body?.data?.fornavn).isEqualTo("")
        assertThat(response.body?.data?.etternavn).isEqualTo("")
        assertThat(response.body?.data?.rolle).isEqualTo(SaksbehandlerRolle.IKKE_SATT)
    }

    @Test
    internal fun `Skal returnere OPPGAVE_FINNES_IKKE dersom behandlingen akkurat er sendt til beslutter uten at behandle-sak-oppgaven er ferdigstilt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak, steg = StegType.BESLUTTE_VEDTAK))

        oppgaveRepository.insert(Oppgave(behandlingId = behandling.id, gsakOppgaveId = 24683L, type = Oppgavetype.BehandleSak))

        val response = hentAnsvarligSaksbehandler(behandling.id)

        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(response.body?.data?.fornavn).isEqualTo("")
        assertThat(response.body?.data?.etternavn).isEqualTo("")
        assertThat(response.body?.data?.rolle).isEqualTo(SaksbehandlerRolle.OPPGAVE_FINNES_IKKE)
    }

    @Test
    internal fun `Skal returnere OPPGAVE_FINNES_IKKE dersom behandlingen akkurat er sendt til beslutter uten at behandle-underkjent-sak-oppgaven er ferdigstilt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak, steg = StegType.BESLUTTE_VEDTAK))

        oppgaveRepository.insert(Oppgave(behandlingId = behandling.id, gsakOppgaveId = 24683L, type = Oppgavetype.BehandleUnderkjentVedtak))

        val response = hentAnsvarligSaksbehandler(behandling.id)

        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(response.body?.data?.fornavn).isEqualTo("")
        assertThat(response.body?.data?.etternavn).isEqualTo("")
        assertThat(response.body?.data?.rolle).isEqualTo(SaksbehandlerRolle.OPPGAVE_FINNES_IKKE)
    }


    @Test
    internal fun `Skal returnere OPPGAVE_FINNES_IKKE dersom behandlingen akkurat er besluttet uten at godkjenne-vedtak-oppgaven er ferdigstilt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak, steg = StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT))

        oppgaveRepository.insert(Oppgave(behandlingId = behandling.id, gsakOppgaveId = 24683L, type = Oppgavetype.GodkjenneVedtak))

        val response = hentAnsvarligSaksbehandler(behandling.id)

        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(response.body?.data?.fornavn).isEqualTo("")
        assertThat(response.body?.data?.etternavn).isEqualTo("")
        assertThat(response.body?.data?.rolle).isEqualTo(SaksbehandlerRolle.OPPGAVE_FINNES_IKKE)
    }

    @Test
    internal fun `Skal returnere IKKE_SATT dersom behandlingen akkurat er er i beslutte-vedtak-steg uten ansvarlig saksbehandler`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak, steg = StegType.BESLUTTE_VEDTAK))

        oppgaveRepository.insert(Oppgave(behandlingId = behandling.id, gsakOppgaveId = 24683L, type = Oppgavetype.GodkjenneVedtak))

        val response = hentAnsvarligSaksbehandler(behandling.id)

        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(response.body?.data?.fornavn).isEqualTo("")
        assertThat(response.body?.data?.etternavn).isEqualTo("")
        assertThat(response.body?.data?.rolle).isEqualTo(SaksbehandlerRolle.IKKE_SATT)
    }

    private fun søkOppgave(personIdent: String): ResponseEntity<Ressurs<OppgaveResponseDto>> =
        restTemplate.exchange(
            localhost("/api/oppgave/soek"),
            HttpMethod.POST,
            HttpEntity(FinnOppgaveRequestDto(ident = personIdent), headers),
        )

    private fun hentAnsvarligSaksbehandler(behandlingId: UUID): ResponseEntity<Ressurs<SaksbehandlerDto>> =
        restTemplate.exchange(
            localhost("/api/oppgave/$behandlingId/ansvarlig-saksbehandler"),
            HttpMethod.GET,
            HttpEntity<Ressurs<SaksbehandlerDto>>(headers),
        )
}
