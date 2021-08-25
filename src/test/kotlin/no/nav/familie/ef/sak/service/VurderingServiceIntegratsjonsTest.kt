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
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.service.SøknadService
import no.nav.familie.ef.sak.service.VurderingService
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID


internal class VurderingServiceIntegratsjonsTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var vurderingService: VurderingService
    @Autowired lateinit var søknadService: SøknadService

    @Test
    internal fun `kopierVurderingerTilNyBehandling - skal kopiere vurderinger til ny behandling`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val revurdering = behandlingRepository.insert(behandling(fagsak))
        val søknadskjema = lagreSøknad(behandling, fagsak)
        val vilkårForBehandling = opprettVilkårsvurderinger(søknadskjema, behandling).first()

        vurderingService.kopierVurderingerTilNyBehandling(behandling.id, revurdering.id)

        val vilkårForRevurdering = vilkårsvurderingRepository.findByBehandlingId(revurdering.id).first()

        assertThat(vilkårForBehandling.id).isNotEqualTo(vilkårForRevurdering.id)
        assertThat(vilkårForBehandling.behandlingId).isNotEqualTo(vilkårForRevurdering.behandlingId)
        assertThat(vilkårForBehandling.sporbar.opprettetTid).isNotEqualTo(vilkårForRevurdering.sporbar.opprettetTid)

        assertThat(vilkårForBehandling.resultat).isEqualTo(vilkårForRevurdering.resultat)
        assertThat(vilkårForBehandling.type).isEqualTo(vilkårForRevurdering.type)
        assertThat(vilkårForBehandling.barnId).isNotNull
        assertThat(vilkårForBehandling.barnId).isEqualTo(vilkårForRevurdering.barnId)
        assertThat(vilkårForBehandling.delvilkårsvurdering).isEqualTo(vilkårForRevurdering.delvilkårsvurdering)
    }

    @Test
    internal fun `kopierVurderingerTilNyBehandling - skal kaste feil hvis det ikke finnes noen vurderinger`() {
        val tidligereBehandlingId = UUID.randomUUID()
        val fagsak = fagsakRepository.insert(fagsak())
        val revurdering = behandlingRepository.insert(behandling(fagsak))

        assertThat(catchThrowable { vurderingService.kopierVurderingerTilNyBehandling(tidligereBehandlingId, revurdering.id) })
                .hasMessage("Tidligere behandling=$tidligereBehandlingId har ikke noen vilkår")
    }

    private fun opprettVilkårsvurderinger(søknadskjema: SøknadsskjemaOvergangsstønad,
                                          behandling: Behandling): List<Vilkårsvurdering> {
        val barnId = søknadskjema.barn.first().id
        val hovedregelMetadata = HovedregelMetadata(søknadskjema, Sivilstandstype.ENKE_ELLER_ENKEMANN)
        val delvilkårsvurdering = SivilstandRegel().initereDelvilkårsvurdering(hovedregelMetadata)
        val vilkårsvurderinger = listOf(vilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                                         type = VilkårType.SIVILSTAND,
                                                         behandlingId = behandling.id,
                                                         barnId = barnId,
                                                         delvilkårsvurdering = delvilkårsvurdering))
        return vilkårsvurderingRepository.insertAll(vilkårsvurderinger)
    }

    private fun lagreSøknad(behandling: Behandling,
                            fagsak: Fagsak): SøknadsskjemaOvergangsstønad {
        søknadService.lagreSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, behandling.id, fagsak.id, "1L")
        return søknadService.hentOvergangsstønad(behandling.id)
    }

}