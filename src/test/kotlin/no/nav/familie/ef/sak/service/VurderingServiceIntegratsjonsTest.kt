package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.regler.HovedregelMetadata
import no.nav.familie.ef.sak.regler.vilkår.SivilstandRegel
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.service.SøknadService
import no.nav.familie.ef.sak.service.VurderingService
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired


internal class VurderingServiceIntegratsjonsTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var vurderingService: VurderingService
    @Autowired lateinit var søknadService: SøknadService

    @Test
    internal fun `Skal kopiere vurderinger fra behandling 1 til 2 `() {


        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val revurdering = behandlingRepository.insert(behandling(fagsak))
        val søknadskjema: SøknadsskjemaOvergangsstønad = lagreSøknad(behandling, fagsak)
        val delvilkårsvurdering = SivilstandRegel().initereDelvilkårsvurdering(HovedregelMetadata(søknadskjema,
                                                                                                  Sivilstandstype.ENKE_ELLER_ENKEMANN))
        val vilkårsvurderinger = listOf(vilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                                         type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                         behandlingId = behandling.id),
                                        vilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                                         type = VilkårType.SIVILSTAND,
                                                         behandlingId = behandling.id,
                                                         delvilkårsvurdering = delvilkårsvurdering)
        )
        vilkårsvurderingRepository.insertAll(vilkårsvurderinger)

        vurderingService.kopierVurderingerTilNyBehandling(behandling.id, revurdering.id)

        //val orginalVurderinger = vilkårsvurderingRepository.findByBehandlingId(behandling.id)
        val revurderingsVurderinger = vilkårsvurderingRepository.findByBehandlingId(revurdering.id)

        Assertions.assertThat(revurderingsVurderinger).isNotNull
        val sivilstand = revurderingsVurderinger.find { it.type === VilkårType.SIVILSTAND }!!
        Assertions.assertThat(sivilstand.delvilkårsvurdering.delvilkårsvurderinger.first()).isNotNull // TODO sjekke hva?


        // sjekk delvilkårsvurdering


    }

    private fun lagreSøknad(behandling: Behandling,
                            fagsak: Fagsak): SøknadsskjemaOvergangsstønad {
        søknadService.lagreSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, behandling.id, fagsak.id, "1L")
        return søknadService.hentOvergangsstønad(behandling.id)
    }

}