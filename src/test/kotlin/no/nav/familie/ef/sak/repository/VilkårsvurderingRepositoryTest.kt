package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.Opphavsvilkår
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class VilkårsvurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByBehandlingId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val vurderinger = listOf(Vurdering(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.JA, "ja"))
        val vilkårsvurdering = vilkårsvurderingRepository.insert(
            vilkårsvurdering(
                behandlingId = behandling.id,
                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                delvilkårsvurdering = listOf(Delvilkårsvurdering(Vilkårsresultat.OPPFYLT, vurderinger)),
                barnId = null,
                opphavsvilkår = Opphavsvilkår(behandling.id, SporbarUtils.now())
            )
        )

        assertThat(vilkårsvurderingRepository.findByBehandlingId(UUID.randomUUID())).isEmpty()
        assertThat(vilkårsvurderingRepository.findByBehandlingId(behandling.id)).containsOnly(vilkårsvurdering)
    }

    @Test
    internal fun `vilkårsvurdering uten opphavsvilkår`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val vilkårsvurdering = vilkårsvurderingRepository.insert(
            vilkårsvurdering(
                behandlingId = behandling.id,
                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                opphavsvilkår = null
            )
        )
        assertThat(vilkårsvurderingRepository.findByBehandlingId(behandling.id)).containsOnly(vilkårsvurdering)
    }

    @Test
    internal fun oppdaterEndretTid() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val vilkårsvurdering = vilkårsvurderingRepository.insert(
            vilkårsvurdering(
                behandling.id,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                VilkårType.FORUTGÅENDE_MEDLEMSKAP
            )
        )
        val nyttTidspunkt = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MILLIS)

        vilkårsvurderingRepository.oppdaterEndretTid(vilkårsvurdering.id, nyttTidspunkt)

        assertThat(vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurdering.id).sporbar.endret.endretTid).isEqualTo(
            nyttTidspunkt
        )
    }

    @Test
    internal fun `setter maskinellt opprettet på vilkår`() {
        val saksbehandler = "C000"
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val vilkårsvurdering: Vilkårsvurdering = testWithBrukerContext(preferredUsername = saksbehandler) {
            vilkårsvurderingRepository.insert(
                vilkårsvurdering(behandling.id, Vilkårsresultat.IKKE_TATT_STILLING_TIL, VilkårType.FORUTGÅENDE_MEDLEMSKAP)
            )
        }
        assertThat(vilkårsvurdering.sporbar.opprettetAv).isEqualTo(saksbehandler)
        assertThat(vilkårsvurdering.sporbar.endret.endretAv).isEqualTo(saksbehandler)

        vilkårsvurderingRepository.settMaskinelltOpprettet(vilkårsvurdering.id)
        val oppdatertVilkårsvurdering = vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurdering.id)
        assertThat(oppdatertVilkårsvurdering.sporbar.opprettetAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
        assertThat(oppdatertVilkårsvurdering.sporbar.endret.endretAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
    }
}
