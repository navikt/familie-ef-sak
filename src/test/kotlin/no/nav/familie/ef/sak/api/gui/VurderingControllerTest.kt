package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.NullstillVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.OppdaterVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VilkårDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.GrunnlagsdataService
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
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal class VurderingControllerTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var behandlingService: BehandlingService
    @Autowired lateinit var fagsakService: FagsakService
    @Autowired lateinit var grunnlagsdataService: GrunnlagsdataService

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `skal hente vilkår`() {
        val respons: ResponseEntity<Ressurs<VilkårDto>> = opprettInngangsvilkår()

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(respons.body.data).isNotNull
    }

    @Test
    internal fun `oppdaterVilkår - skal sjekke att behandlingId som blir sendt inn er lik den som finnes i vilkårsvurderingen`() {
        val opprettetVurdering = opprettInngangsvilkår().body.data!!
        val fagsak = fagsakService.hentEllerOpprettFagsakMedBehandlinger("0", Stønadstype.OVERGANGSSTØNAD)
        val behandling = behandlingService.opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, fagsak.id)

        val oppdaterVilkårsvurdering = lagOppdaterVilkårsvurdering(opprettetVurdering, VilkårType.FORUTGÅENDE_MEDLEMSKAP)
                .copy(behandlingId = behandling.id)
        validerSjekkPåBehandlingId(oppdaterVilkårsvurdering, "vilkar")
    }

    @Test
    internal fun `nullstillVilkår - skal sjekke att behandlingId som blir sendt inn er lik den som finnes i vilkårsvurderingen`() {
        val opprettetVurdering = opprettInngangsvilkår().body.data!!

        val fagsak = fagsakService.hentEllerOpprettFagsakMedBehandlinger("0", Stønadstype.OVERGANGSSTØNAD)
        val behandling = behandlingService.opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, fagsak.id)
        val nullstillVurdering = NullstillVilkårsvurderingDto(opprettetVurdering.vurderinger.first().id, behandling.id)

        validerSjekkPåBehandlingId(nullstillVurdering, "nullstill")
    }

    private fun validerSjekkPåBehandlingId(request: Any, path: String) {
        val respons: ResponseEntity<Ressurs<VilkårsvurderingDto>> =
                restTemplate.exchange(localhost("/api/vurdering/$path"),
                                      HttpMethod.POST,
                                      HttpEntity(request, headers))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(respons.body!!.frontendFeilmelding).isEqualTo("BehandlingId er feil, her har noe gått galt")
    }

    @Test
    internal fun `skal oppdatere vurderingen for FORUTGÅENDE_MEDLEMSKAP som har ett spørsmål som vi setter til JA`() {
        val opprettetVurdering = opprettInngangsvilkår().body.data!!
        val oppdatertVilkårsvarMedJa = lagOppdaterVilkårsvurdering(opprettetVurdering, VilkårType.FORUTGÅENDE_MEDLEMSKAP)
        val respons: ResponseEntity<Ressurs<VilkårsvurderingDto>> =
                restTemplate.exchange(localhost("/api/vurdering/vilkar"),
                                      HttpMethod.POST,
                                      HttpEntity(oppdatertVilkårsvarMedJa, headers))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(respons.body.data?.id).isEqualTo(oppdatertVilkårsvarMedJa.id)
    }

    private fun lagOppdaterVilkårsvurdering(opprettetVurdering: VilkårDto, vilkårType: VilkårType): OppdaterVilkårsvurderingDto {
        return opprettetVurdering.vurderinger.first { it.vilkårType == vilkårType }.let {
            lagOppdaterVilkårsvurderingMedSvarJa(it)
        }
    }

    private fun lagOppdaterVilkårsvurderingMedSvarJa(it: VilkårsvurderingDto) =
            OppdaterVilkårsvurderingDto(id = it.id,
                                        behandlingId = it.behandlingId,
                                        delvilkårsvurderinger = it.delvilkårsvurderinger.map {
                                            it.copy(vurderinger = it.vurderinger.map { vurderingDto ->
                                                vurderingDto.copy(svar = SvarId.JA)
                                            })
                                        })


    private fun opprettInngangsvilkår(): ResponseEntity<Ressurs<VilkårDto>> {
        val søknad = SøknadMedVedlegg(Testsøknad.søknadOvergangsstønad, emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsakMedBehandlinger(søknad.søknad.personalia.verdi.fødselsnummer.verdi.verdi,
                                                                         Stønadstype.OVERGANGSSTØNAD)
        val behandling = behandlingService.opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, fagsak.id)
        behandlingService.lagreSøknadForOvergangsstønad(søknad.søknad, behandling.id, fagsak.id, "1234")
        grunnlagsdataService.hentEndringerIRegistergrunnlag(behandling.id)

        return restTemplate.exchange(localhost("/api/vurdering/${behandling.id}/vilkar"),
                                     HttpMethod.GET,
                                     HttpEntity<Any>(headers))
    }
}