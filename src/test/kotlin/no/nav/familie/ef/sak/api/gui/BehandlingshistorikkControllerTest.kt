package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkRepository
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.behandlingshistorikk.dto.BehandlingshistorikkDto
import no.nav.familie.ef.sak.behandlingshistorikk.dto.HendelseshistorikkDto
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.domain.JsonWrapper
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingshistorikkControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingshistorikkRepository: BehandlingshistorikkRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal returnere 403 FORBIDDEN med status IKKE_TILGANG dersom man ikke har tilgang til brukeren`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("ikkeTilgang"))))
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val respons = hentHistorikk(behandling.id)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        assertThat(respons.body?.data).isNull()
    }

    @Test
    internal fun `skal kun returnere den første hendelsen av typen OPPRETTET - etterfølgende hendelser av denne typen skal lukes vekk`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        leggInnHistorikk(behandling, "1", LocalDateTime.now(), StegType.VILKÅR)
        leggInnHistorikk(behandling, "2", LocalDateTime.now().minusDays(1), StegType.VILKÅR)
        leggInnHistorikk(behandling, "3", LocalDateTime.now().plusDays(1), StegType.VILKÅR)

        val respons = hentHistorikk(behandling.id)
        assertThat(respons.body?.data!!.map { it.endretAvNavn }).containsExactly("2")
    }

    @Test
    internal fun `skal returnere hendelser av alle typer i riktig rekkefølge for invilget behandling `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        leggInnHistorikk(behandling, "1", LocalDateTime.now(), StegType.VILKÅR)
        leggInnHistorikk(behandling, "2", LocalDateTime.now().plusDays(1), StegType.BEREGNE_YTELSE)
        leggInnHistorikk(behandling, "3", LocalDateTime.now().plusDays(2), StegType.SEND_TIL_BESLUTTER)
        leggInnHistorikk(
            behandling,
            "4",
            LocalDateTime.now().plusDays(3),
            StegType.BESLUTTE_VEDTAK,
            stegUtfall = StegUtfall.BESLUTTE_VEDTAK_GODKJENT,
        )
        leggInnHistorikk(behandling, "5", LocalDateTime.now().plusDays(4), StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        leggInnHistorikk(behandling, "6", LocalDateTime.now().plusDays(5), StegType.LAG_SAKSBEHANDLINGSBLANKETT)
        leggInnHistorikk(behandling, "7", LocalDateTime.now().plusDays(6), StegType.FERDIGSTILLE_BEHANDLING)
        leggInnHistorikk(behandling, "8", LocalDateTime.now().plusDays(7), StegType.PUBLISER_VEDTAKSHENDELSE)
        leggInnHistorikk(behandling, "9", LocalDateTime.now().plusDays(8), StegType.BEHANDLING_FERDIGSTILT)
        behandlingRepository.update(behandling.copy(resultat = BehandlingResultat.INNVILGET, vedtakstidspunkt = SporbarUtils.now()))
        val respons = hentHistorikk(behandling.id)
        assertThat(respons.body?.data!!.map { it.endretAvNavn }).containsExactly("7", "4", "3", "1")
    }

    @Test
    internal fun `skal returnere hendelser av alle typer i riktig rekkefølge for henlagt behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        leggInnHistorikk(behandling, "1", LocalDateTime.now(), StegType.VILKÅR)
        leggInnHistorikk(behandling, "2", LocalDateTime.now().plusDays(1), StegType.BEREGNE_YTELSE)
        leggInnHistorikk(behandling, "3", LocalDateTime.now().plusDays(2), StegType.SEND_TIL_BESLUTTER)
        leggInnHistorikk(
            behandling,
            "4",
            LocalDateTime.now().plusDays(3),
            StegType.BESLUTTE_VEDTAK,
            stegUtfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
        )
        leggInnHistorikk(behandling, "5", LocalDateTime.now().plusDays(6), StegType.FERDIGSTILLE_BEHANDLING)
        leggInnHistorikk(behandling, "6", LocalDateTime.now().plusDays(8), StegType.BEHANDLING_FERDIGSTILT)
        behandlingRepository.update(
            behandling.copy(
                resultat = BehandlingResultat.HENLAGT,
                vedtakstidspunkt = SporbarUtils.now(),
            ),
        )
        val respons = hentHistorikk(behandling.id)
        assertThat(respons.body?.data!!.map { it.endretAvNavn }).containsExactly("6", "4", "3", "1")
    }

    @Test
    internal fun `skal returnere alle hendelser dersom en behandling blir underkjent i totrinnskontroll, deretter sendt til beslutter på nytt og deretter godkjent`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        leggInnHistorikk(behandling, "1", LocalDateTime.now(), StegType.VILKÅR)
        leggInnHistorikk(behandling, "2", LocalDateTime.now().plusDays(1), StegType.BEREGNE_YTELSE)
        leggInnHistorikk(behandling, "3", LocalDateTime.now().plusDays(2), StegType.SEND_TIL_BESLUTTER)
        leggInnHistorikk(
            behandling,
            "4",
            LocalDateTime.now().plusDays(3),
            StegType.BESLUTTE_VEDTAK,
            stegUtfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
        )
        leggInnHistorikk(behandling, "5", LocalDateTime.now().plusDays(4), StegType.SEND_TIL_BESLUTTER)
        leggInnHistorikk(
            behandling,
            "6",
            LocalDateTime.now().plusDays(5),
            StegType.BESLUTTE_VEDTAK,
            stegUtfall = StegUtfall.BESLUTTE_VEDTAK_GODKJENT,
        )

        val respons = hentHistorikk(behandling.id)
        assertThat(respons.body?.data!!.map { it.endretAvNavn }).containsExactly("6", "5", "4", "3", "1")
    }

    @Test
    internal fun `skal returnere metadata som json`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val jsonMap = mapOf("key" to "value")
        val metadata = JsonWrapper(jsonMapper.writeValueAsString(jsonMap))
        behandlingshistorikkRepository.insert(
            Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = behandling.steg,
                metadata = metadata,
            ),
        )

        val respons = hentHistorikk(behandling.id)
        assertThat(
            respons.body
                ?.data
                ?.first()
                ?.metadata,
        ).isEqualTo(jsonMap)
    }

    private fun leggInnHistorikk(
        behandling: Behandling,
        opprettetAv: String,
        endretTid: LocalDateTime,
        steg: StegType? = null,
        stegUtfall: StegUtfall? = null,
    ) {
        behandlingshistorikkRepository.insert(
            Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = steg ?: behandling.steg,
                utfall = stegUtfall,
                opprettetAv = opprettetAv,
                opprettetAvNavn = opprettetAv,
                endretTid = endretTid,
            ),
        )
    }

    private fun hentHistorikk(id: UUID): ResponseEntity<Ressurs<List<HendelseshistorikkDto>>> =
        testRestTemplate.exchange(
            localhost("/api/behandlingshistorikk/$id"),
            HttpMethod.GET,
            HttpEntity<Ressurs<List<BehandlingshistorikkDto>>>(headers),
        )
}
