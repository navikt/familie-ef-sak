package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.VedtakService
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
}
