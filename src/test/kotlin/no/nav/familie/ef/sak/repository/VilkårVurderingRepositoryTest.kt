package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.VilkårVurderingRepository
import no.nav.familie.ef.sak.repository.domain.VilkårResultat
import no.nav.familie.ef.sak.repository.domain.VilkårType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import java.util.*

@ActiveProfiles("local", "mock-oauth")
internal class VilkårVurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var customRepository: CustomRepository
    @Autowired private lateinit var vilkårVurderingRepository: VilkårVurderingRepository

    @Test
    internal fun findByBehandlingId() {
        val fagsak = customRepository.persist(fagsak())
        val behandling = customRepository.persist(behandling(fagsak))

        val vilkårVurdering = customRepository.persist(vilkårVurdering(behandling,
                                                                       VilkårResultat.IKKE_VURDERT,
                                                                       VilkårType.FORUTGÅENDE_MEDLEMSKAP))

        assertThat(vilkårVurderingRepository.findByBehandlingId(UUID.randomUUID())).isEmpty()
        assertThat(vilkårVurderingRepository.findByBehandlingId(behandling.id)).containsOnly(vilkårVurdering)
    }
}