package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

internal class TestSaksbehandlingControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vurderingService: VurderingService

    @Autowired
    private lateinit var testSaksbehandlingController: TestSaksbehandlingController

    @Autowired
    private lateinit var grunnlagsdataService: GrunnlagsdataService

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @ParameterizedTest
    @EnumSource(StønadType::class)
    internal fun `automatiskt utfyller vilkår`(stønadType: StønadType) {
        val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = stønadType))
        val behandling = behandlingRepository.insert(behandling(fagsak))
        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        vurderingService.hentEllerOpprettVurderinger(behandling.id)

        testWithBrukerContext(groups = listOf(rolleConfig.saksbehandlerRolle)) {
            testSaksbehandlingController.utfyllVilkår(behandling.id)
        }

        val oppdaterteVurderinger = vurderingService.hentAlleVurderinger(behandling.id)
        assertThat(oppdaterteVurderinger.map { it.resultat }.distinct()).containsExactly(Vilkårsresultat.OPPFYLT)
    }
}
