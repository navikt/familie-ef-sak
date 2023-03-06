package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.vilkår.Gjenbrukt
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class VilkårsvurderingTest {

    private val behandlingIdFørstegangsbehandling = UUID.randomUUID()
    private val behandlingIdRevurdering = UUID.randomUUID()

    @Test
    internal fun `lagGjenbrukt - et vilkår som ikke er gjenbrukt skal peke til behandlingen`() {
        val vilkårsvurdering = Vilkårsvurdering(
            behandlingId = behandlingIdFørstegangsbehandling,
            delvilkårsvurdering = DelvilkårsvurderingWrapper(emptyList()),
            type = VilkårType.MOR_ELLER_FAR,
            gjenbrukt = null
        )
        val gjenbrukt = vilkårsvurdering.lagGjenbrukt()
        assertThat(gjenbrukt).isEqualTo(Gjenbrukt(behandlingIdFørstegangsbehandling,
            vilkårsvurdering.sporbar.endret.endretTid))
    }

    @Test
    internal fun `lagGjenbrukt - skal bruke gjenbrukt hvis den finnes og ikke lage en ny, for å peke til den opprinnelige behandlingen`() {
        val gjenbrukt = Gjenbrukt(behandlingIdFørstegangsbehandling, LocalDateTime.now())
        val vilkårsvurdering = Vilkårsvurdering(
            behandlingId = behandlingIdRevurdering,
            delvilkårsvurdering = DelvilkårsvurderingWrapper(emptyList()),
            type = VilkårType.MOR_ELLER_FAR,
            gjenbrukt = gjenbrukt
        )
        val nyGjenbrukt = vilkårsvurdering.lagGjenbrukt()
        assertThat(nyGjenbrukt).isEqualTo(gjenbrukt)
    }
}