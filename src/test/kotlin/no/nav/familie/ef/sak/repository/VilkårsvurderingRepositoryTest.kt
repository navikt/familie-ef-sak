package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class VilkårsvurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByBehandlingId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val vilkårsvurdering = vilkårsvurderingRepository.insert(vilkårsvurdering(behandling.id,
                                                                                  Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                                                  VilkårType.FORUTGÅENDE_MEDLEMSKAP))

        assertThat(vilkårsvurderingRepository.findByBehandlingId(UUID.randomUUID())).isEmpty()
        assertThat(vilkårsvurderingRepository.findByBehandlingId(behandling.id)).containsOnly(vilkårsvurdering)
    }

    @Test
    internal fun oppdaterEndretTid() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val vilkårsvurdering = vilkårsvurderingRepository.insert(vilkårsvurdering(behandling.id,
                                                                                  Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                                                  VilkårType.FORUTGÅENDE_MEDLEMSKAP))
        val nyttTidspunkt = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MILLIS)

        vilkårsvurderingRepository.oppdaterEndretTid(vilkårsvurdering.id, nyttTidspunkt)

        assertThat(vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurdering.id).sporbar.endret.endretTid).isEqualTo(
                nyttTidspunkt)
    }

    @Test
    internal fun `setter maskinellt opprettet på vilkår`() {
        val saksbehandler = "C000"
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val vilkårsvurdering: Vilkårsvurdering = testWithBrukerContext(preferredUsername = saksbehandler) {
            vilkårsvurderingRepository.insert(
                    vilkårsvurdering(behandling.id, Vilkårsresultat.IKKE_TATT_STILLING_TIL, VilkårType.FORUTGÅENDE_MEDLEMSKAP))
        }
        assertThat(vilkårsvurdering.sporbar.opprettetAv).isEqualTo(saksbehandler)
        assertThat(vilkårsvurdering.sporbar.endret.endretAv).isEqualTo(saksbehandler)

        vilkårsvurderingRepository.settMaskinelltOpprettet(vilkårsvurdering.id)
        val oppdatertVilkårsvurdering = vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurdering.id)
        assertThat(oppdatertVilkårsvurdering.sporbar.opprettetAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
        assertThat(oppdatertVilkårsvurdering.sporbar.endret.endretAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
    }
}