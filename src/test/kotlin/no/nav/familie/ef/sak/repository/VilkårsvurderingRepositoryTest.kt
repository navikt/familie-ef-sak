package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class VilkårsvurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository
    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByBehandlingId() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val vilkårsvurdering = vilkårsvurderingRepository.insert(vilkårsvurdering(behandling.id,
                                                                                  Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                                                  VilkårType.FORUTGÅENDE_MEDLEMSKAP))

        assertThat(vilkårsvurderingRepository.findByBehandlingId(UUID.randomUUID())).isEmpty()
        assertThat(vilkårsvurderingRepository.findByBehandlingId(behandling.id)).containsOnly(vilkårsvurdering)
    }

    @Test
    internal fun oppdaterEndretTid() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val vilkårsvurdering = vilkårsvurderingRepository.insert(vilkårsvurdering(behandling.id,
                                                                                  Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                                                  VilkårType.FORUTGÅENDE_MEDLEMSKAP))
        val nyttTidspunkt = LocalDateTime.now().minusDays(1)

        vilkårsvurderingRepository.oppdaterEndretTid(vilkårsvurdering.id, nyttTidspunkt)

        assertThat(vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurdering.id).sporbar.endret.endretTid).isEqualTo(nyttTidspunkt)
    }
}