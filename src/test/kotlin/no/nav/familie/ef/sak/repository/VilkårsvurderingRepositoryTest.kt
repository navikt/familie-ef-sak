package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

internal class VilkårsvurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository
    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByBehandlingId() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val vilkårsvurdering = vilkårsvurderingRepository.insert(vilkårsvurdering(behandling.id,
                                                                                  Vilkårsresultat.IKKE_VURDERT,
                                                                                  Vilkårstype.FORUTGÅENDE_MEDLEMSKAP))

        assertThat(vilkårsvurderingRepository.findByBehandlingId(UUID.randomUUID())).isEmpty()
        assertThat(vilkårsvurderingRepository.findByBehandlingId(behandling.id)).containsOnly(vilkårsvurdering)
    }
}