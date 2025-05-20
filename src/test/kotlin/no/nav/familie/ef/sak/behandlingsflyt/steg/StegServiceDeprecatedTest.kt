package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.IVERKSETTER_VEDTAK
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

internal class StegServiceDeprecatedTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var stegServiceDeprecated: StegServiceDeprecated

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @Autowired
    lateinit var behandlingshistorikkRepository: BehandlingshistorikkRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vedtakService: VedtakService

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `kast feil når man resetter med et steg etter behandlingen sitt steg`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling =
            behandlingRepository.insert(
                behandling(
                    status = BehandlingStatus.UTREDES,
                    fagsak = fagsak,
                    steg = StegType.VILKÅR,
                ),
            )

        assertThrows<IllegalStateException> {
            stegServiceDeprecated.resetSteg(behandling.id, steg = StegType.BEREGNE_YTELSE)
        }
    }

    @Test
    internal fun `steg på behandlingen beholdes når man resetter på samme steg`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling =
            behandlingRepository.insert(
                behandling(
                    status = BehandlingStatus.UTREDES,
                    fagsak = fagsak,
                    steg = StegType.BEREGNE_YTELSE,
                ),
            )

        stegServiceDeprecated.resetSteg(behandling.id, steg = StegType.BEREGNE_YTELSE)
        assertThat(behandlingRepository.findByIdOrThrow(behandling.id).steg).isEqualTo(StegType.BEREGNE_YTELSE)
    }

    @Test
    internal fun `steg på behandlingen oppdateres når man resetter med et tidligere steg`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling =
            behandlingRepository.insert(
                behandling(
                    status = BehandlingStatus.UTREDES,
                    fagsak = fagsak,
                    steg = StegType.BEREGNE_YTELSE,
                ),
            )

        stegServiceDeprecated.resetSteg(behandling.id, steg = StegType.VILKÅR)
        assertThat(behandlingRepository.findByIdOrThrow(behandling.id).steg).isEqualTo(StegType.VILKÅR)
    }

    @Test
    internal fun `skal feile hvis behandling iverksettes og man prøver godkjenne saksbehandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingSomIverksettes(fagsak)
        vedtakService.lagreVedtak(InnvilgelseOvergangsstønad("", ""), behandling.id, fagsak.stønadstype)
        BrukerContextUtil.mockBrukerContext("navIdent")
        val beslutteVedtakDto = BeslutteVedtakDto(true, "")
        val feil =
            assertThrows<ApiFeil> {
                stegServiceDeprecated.håndterBeslutteVedtak(saksbehandling(fagsak, behandling), beslutteVedtakDto)
            }
        assertThat(feil.message).isEqualTo("Behandlingen er allerede besluttet. Status på behandling er 'Iverksetter vedtak'")
    }

    private fun behandlingSomIverksettes(fagsak: Fagsak): Behandling {
        val nyBehandling = behandling(fagsak, IVERKSETTER_VEDTAK, VENTE_PÅ_STATUS_FRA_IVERKSETT)
        return behandlingRepository.insert(nyBehandling)
    }
}
