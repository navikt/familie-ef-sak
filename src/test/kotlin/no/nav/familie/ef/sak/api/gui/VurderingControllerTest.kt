package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
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
import java.util.*

internal class VurderingControllerTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var behandlingService: BehandlingService
    @Autowired lateinit var fagsakService: FagsakService
    @Autowired lateinit var grunnlagsdataService: GrunnlagsdataService

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `skal hente inngangsvilkår`() {
        val respons: ResponseEntity<Ressurs<InngangsvilkårDto>> = opprettInngangsvilkår()

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(respons.body.data).isNotNull
    }

    @Test
    internal fun `skal oppdatere vilkår`() {
        val opprettetVurdering = opprettInngangsvilkår().body.data!!.vurderinger.first()

        val respons: ResponseEntity<Ressurs<UUID>> =
                restTemplate.exchange(localhost("/api/vurdering/inngangsvilkar"),
                                      HttpMethod.POST,
                                      HttpEntity(opprettetVurdering.copy(resultat = Vilkårsresultat.OPPFYLT,
                                                                         begrunnelse = "Godkjent"),
                                                 headers))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(respons.body.data).isEqualTo(opprettetVurdering.id)
    }

    private fun opprettInngangsvilkår(): ResponseEntity<Ressurs<InngangsvilkårDto>> {
        val søknad = SøknadMedVedlegg(Testsøknad.søknadOvergangsstønad, emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsak(søknad.søknad.personalia.verdi.fødselsnummer.verdi.verdi,
                                                          Stønadstype.OVERGANGSSTØNAD)
        val behandling = behandlingService.opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, fagsak.id)
        behandlingService.lagreSøknadForOvergangsstønad(søknad.søknad, behandling.id, fagsak.id, "1234")
        grunnlagsdataService.hentEndringerIRegistergrunnlag(behandling.id)

        return restTemplate.exchange(localhost("/api/vurdering/${behandling.id}/inngangsvilkar"),
                                     HttpMethod.GET,
                                     HttpEntity<Any>(headers))
    }
}