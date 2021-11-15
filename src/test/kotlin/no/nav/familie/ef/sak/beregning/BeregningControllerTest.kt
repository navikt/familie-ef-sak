package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.søknad.SøknadMedVedlegg
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
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
    @Autowired private lateinit var vilkårsvurderingService: VurderingService
    @Autowired private lateinit var søknadService: SøknadService
    @Autowired private lateinit var grunnlagsdataService: GrunnlagsdataService

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal klare å inserte ett vedtak med resultatet avslå`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                steg = StegType.VEDTA_BLANKETT,
                                                                type = BehandlingType.BLANKETT,
                                                                status = BehandlingStatus.UTREDES))
        val vedtakDto = Avslå(avslåBegrunnelse = "avslår vedtaket")
        val vedtak = Vedtak(behandlingId = behandling.id, avslåBegrunnelse = "avslår vedtaket", resultatType = ResultatType.AVSLÅ)
        val respons: ResponseEntity<Ressurs<UUID>> = fatteVedtak(behandling.id, vedtakDto)


        assertThat(vedtakService.hentVedtak(respons.body.data!!)).isEqualTo(vedtak)
    }

    @Test
    internal fun `Skal klare å inserte ett vedtak med resultatet innvilge`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak,
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

    @Test
    internal fun `Skal returnere riktig feilmelding i response når fullfør ikke er mulig pga valideringsfeil`() {
        val behandling = lagFagsakOgBehandling()

        val vedtakDto = Innvilget(periodeBegrunnelse = "periode begrunnelse",
                                  inntektBegrunnelse = "inntekt begrunnelse")

        vilkårsvurderingService.hentEllerOpprettVurderinger(behandlingId = behandling.id) // ingen ok.

        val respons: ResponseEntity<Ressurs<UUID>> = fullførVedtak(behandling.id, vedtakDto)

        assertThat(respons.body.frontendFeilmelding).isEqualTo("Kan ikke fullføre en behandling med resultat innvilget hvis ikke alle vilkår er oppfylt")

    }

    private fun lagFagsakOgBehandling(): Behandling {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                steg = StegType.VEDTA_BLANKETT,
                                                                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                status = BehandlingStatus.UTREDES))

        val søknad = SøknadMedVedlegg(Testsøknad.søknadOvergangsstønad, emptyList())


        søknadService.lagreSøknadForOvergangsstønad(søknad.søknad, behandling.id, fagsak.id, "1234")
        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)


        return behandling
    }

    private fun fatteVedtak(id: UUID, vedtakDto: VedtakDto): ResponseEntity<Ressurs<UUID>> {
        return restTemplate.exchange(localhost("/api/beregning/$id/lagre-blankettvedtak"),
                                     HttpMethod.POST,
                                     HttpEntity(vedtakDto, headers))
    }


    private fun fullførVedtak(id: UUID, vedtakDto: VedtakDto): ResponseEntity<Ressurs<UUID>> {
        return restTemplate.exchange(localhost("/api/beregning/$id/fullfor"),
                                     HttpMethod.POST,
                                     HttpEntity(vedtakDto, headers))
    }


}