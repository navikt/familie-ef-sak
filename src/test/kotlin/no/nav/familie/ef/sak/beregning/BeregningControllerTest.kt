package no.nav.familie.ef.sak.no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakPerson
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.steg.StegType
import no.nav.familie.ef.sak.vedtak.Avslå
import no.nav.familie.ef.sak.vedtak.Henlegge
import no.nav.familie.ef.sak.vedtak.InntektWrapper
import no.nav.familie.ef.sak.vedtak.Innvilget
import no.nav.familie.ef.sak.vedtak.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.ResultatType
import no.nav.familie.ef.sak.vedtak.Vedtak
import no.nav.familie.ef.sak.vedtak.VedtakDto
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import java.util.UUID

class BeregningControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var vedtakService: VedtakService

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal klare å inserte ett vedtak med resultatet avslå`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                aktiv = true,
                                                                steg = StegType.VEDTA_BLANKETT,
                                                                type = BehandlingType.BLANKETT,
                                                                status = BehandlingStatus.UTREDES))
        val vedtakDto = Avslå(avslåBegrunnelse = "avslår vedtaket")
        val vedtak = Vedtak(behandlingId = behandling.id, avslåBegrunnelse = "avslår vedtaket", resultatType = ResultatType.AVSLÅ)
        val respons: ResponseEntity<Ressurs<UUID>> = fatteVedtak(behandling.id, vedtakDto)


        assertThat(vedtakService.hentVedtak(respons.body.data!!)).isEqualTo(vedtak)
    }

    @Test
    internal fun `Skal feile hvis resultatet er henlegge`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                aktiv = true,
                                                                steg = StegType.VEDTA_BLANKETT,
                                                                type = BehandlingType.BLANKETT,
                                                                status = BehandlingStatus.UTREDES))
        val vedtakDto = Henlegge()
        val respons: ResponseEntity<Ressurs<UUID>> = fatteVedtak(behandling.id, vedtakDto)


        assertThat(respons.body.status).isEqualTo(Ressurs.Status.FEILET)
    }

    @Test
    internal fun `Skal klare å inserte ett vedtak med resultatet innvilge`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                aktiv = true,
                                                                steg = StegType.VEDTA_BLANKETT,
                                                                type = BehandlingType.BLANKETT,
                                                                status = BehandlingStatus.UTREDES))
        val vedtakDto = Innvilget(periodeBegrunnelse = "periode begrunnelse",
                                  inntektBegrunnelse = "inntekt begrunnelse")

        val respons: ResponseEntity<Ressurs<UUID>> = fatteVedtak(behandling.id, vedtakDto)
        val vedtak = Vedtak(behandlingId = behandling.id,
                            periodeBegrunnelse = "periode begrunnelse",
                            inntektBegrunnelse = "inntekt begrunnelse",
                            resultatType = ResultatType.INNVILGE,
                            inntekter = InntektWrapper(emptyList()),
                            avslåBegrunnelse = null,
                            perioder = PeriodeWrapper(emptyList()))

        assertThat(vedtakService.hentVedtak(respons.body.data!!)).isEqualTo(vedtak)
    }

    private fun fatteVedtak(id: UUID, vedtakDto: VedtakDto): ResponseEntity<Ressurs<UUID>> {
        return restTemplate.exchange(localhost("/api/beregning/$id/lagre-vedtak"),
                                     HttpMethod.POST,
                                     HttpEntity(vedtakDto, headers))
    }

}