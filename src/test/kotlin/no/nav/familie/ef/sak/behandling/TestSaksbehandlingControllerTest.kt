package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.vilkår.VurderingService
import org.junit.jupiter.api.Test
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

    @Test
    internal fun `vilkår for overgangsstønad`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        vurderingService.hentEllerOpprettVurderinger(behandling.id)
        testWithBrukerContext {
            testSaksbehandlingController.utfyllVilkår(behandling.id)
        }
        val hentAlleVurderinger = vurderingService.hentAlleVurderinger(behandling.id)
        println(hentAlleVurderinger)
    }
}