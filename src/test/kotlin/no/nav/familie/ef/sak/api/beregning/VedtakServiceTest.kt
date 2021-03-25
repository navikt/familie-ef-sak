package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.VedtakRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.service.steg.StegType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

internal class VedtakServiceTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var vedtakService: VedtakService
    @Autowired private lateinit var vedtakRepository: VedtakRepository

    @Autowired private lateinit var fagsakRepository: FagsakRepository

    @Test
    fun `lagre og hent vedtak, lagre igjen - da skal første slettes`() {

        /** Pre */
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                steg = StegType.VILKÅR,
                                                                status = BehandlingStatus.UTREDES,
                                                                type = BehandlingType.BLANKETT))

        val tomBegrunnelse = ""
        val vedtakRequest = VedtakDto(resultatType = ResultatType.INNVILGE,
                                      tomBegrunnelse,
                                      tomBegrunnelse, emptyList(), emptyList())

        /** Skal ikke gjøre noe når den ikke er opprettet **/
        vedtakService.slettVedtakHvisFinnes(behandling.id)

        /** Opprett */
        vedtakService.lagreVedtak(vedtakRequest, behandling.id)

        /** Verifiser opprettet */
        val vedtakLagret = vedtakRepository.findByIdOrNull(behandling.id)
        assertThat(vedtakLagret?.resultatType).isEqualTo(ResultatType.INNVILGE)
        assertThat(vedtakLagret?.periodeBegrunnelse).isEqualTo(tomBegrunnelse)

        /** Slett og opprett ny **/
        val periodeBegrunnelse = "Begrunnelse"
        vedtakService.slettVedtakHvisFinnes(behandling.id)
        assertThat(vedtakRepository.findAll()).isEmpty()
        vedtakService.lagreVedtak(vedtakRequest.copy(periodeBegrunnelse = periodeBegrunnelse), behandling.id)

        /** Verifiser nytt **/
        val nyttVedtakLagret = vedtakRepository.findByIdOrNull(behandling.id)
        assertThat(nyttVedtakLagret?.resultatType).isEqualTo(ResultatType.INNVILGE)
        assertThat(nyttVedtakLagret?.periodeBegrunnelse).isEqualTo(periodeBegrunnelse)

    }

    @Test
    fun `skal hente lagret vedtak hvis finnes`(){
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                steg = StegType.VILKÅR,
                                                                status = BehandlingStatus.UTREDES,
                                                                type = BehandlingType.BLANKETT))

        val tomBegrunnelse = ""
        val vedtakDto = VedtakDto(resultatType = ResultatType.INNVILGE,
                                      tomBegrunnelse,
                                      tomBegrunnelse, emptyList(), emptyList())

        vedtakService.lagreVedtak(vedtakDto, behandling.id)

        assertThat(vedtakService.hentVedtakHvisEksisterer(behandling.id)).usingRecursiveComparison().isEqualTo(vedtakDto)
    }


}