package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.Testsøknad
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.vilkårVurdering
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.VilkårVurderingRepository
import no.nav.familie.ef.sak.repository.domain.VilkårResultat
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.VilkårVurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*


internal class VurderingServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val customRepository = mockk<CustomRepository>()
    private val vilkårVurderingRepository = mockk<VilkårVurderingRepository>()
    private val vurderingService = VurderingService(behandlingService = behandlingService,
                                                    pdlClient = PdlClientConfig().pdlClient(),
                                                    medlemskapMapper = mockk(relaxed = true),
                                                    customRepository = customRepository,
                                                    vilkårVurderingRepository = vilkårVurderingRepository)

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentOvergangsstønad(any()) } returns Testsøknad.søknad
    }

    @Test
    fun `skal opprette nye VilkårVurdering for alle inngangsvilkår dersom ingen vurderinger finnes`() {
        every { vilkårVurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns emptyList()

        val nyeVilkårVurderinger = slot<List<VilkårVurdering>>()
        every { customRepository.persistAll(capture(nyeVilkårVurderinger)) } answers
                { it.invocation.args.first() as List<VilkårVurdering> }
        val inngangsvilkår = VilkårType.hentInngangsvilkår()

        vurderingService.hentInngangsvilkår(BEHANDLING_ID)

        assertThat(nyeVilkårVurderinger.captured).hasSize(inngangsvilkår.size)
        assertThat(nyeVilkårVurderinger.captured.map { it.type }).containsExactlyElementsOf(inngangsvilkår)
        assertThat(nyeVilkårVurderinger.captured.map { it.resultat }.toSet()).containsOnly(VilkårResultat.IKKE_VURDERT)
        assertThat(nyeVilkårVurderinger.captured.map { it.behandlingId }.toSet()).containsOnly(BEHANDLING_ID)
    }

    @Test
    fun `skal ikke opprette nye VilkårVurderinger for inngangsvilkår som allerede har en vurdering`() {
        every { vilkårVurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(vilkårVurdering(resultat = VilkårResultat.JA,
                                                                                                             type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                                                                             behandlingId = BEHANDLING_ID))

        val nyeVilkårVurderinger = slot<List<VilkårVurdering>>()
        every { customRepository.persistAll(capture(nyeVilkårVurderinger)) } answers
                { it.invocation.args.first() as List<VilkårVurdering> }
        val inngangsvilkår = VilkårType.hentInngangsvilkår()

        val alleVilkårVurderinger = vurderingService.hentInngangsvilkår(BEHANDLING_ID).vurderinger

        assertThat(nyeVilkårVurderinger.captured).hasSize(inngangsvilkår.size - 1)
        assertThat(alleVilkårVurderinger).hasSize(inngangsvilkår.size)
        assertThat(nyeVilkårVurderinger.captured.map { it.type }).doesNotContain(VilkårType.FORUTGÅENDE_MEDLEMSKAP)
    }

    companion object {
        private val BEHANDLING_ID = UUID.randomUUID()
    }
}