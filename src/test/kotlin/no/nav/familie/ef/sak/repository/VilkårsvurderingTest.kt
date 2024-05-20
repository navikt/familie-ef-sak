package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.vilkår.Opphavsvilkår
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
    internal fun `opprettOpphavsvilkår - et vilkår som ikke er gjenbrukt skal peke til behandlingen`() {
        val vilkårsvurdering =
            Vilkårsvurdering(
                behandlingId = behandlingIdFørstegangsbehandling,
                delvilkårsvurdering = DelvilkårsvurderingWrapper(emptyList()),
                type = VilkårType.MOR_ELLER_FAR,
                opphavsvilkår = null,
            )
        val opphavsvilkår = vilkårsvurdering.opprettOpphavsvilkår()
        assertThat(opphavsvilkår).isEqualTo(
            Opphavsvilkår(
                behandlingIdFørstegangsbehandling,
                vilkårsvurdering.sporbar.endret.endretTid,
            ),
        )
    }

    @Test
    internal fun `opprettOpphavsvilkår - skal bruke opphavsvilkår hvis den finnes og ikke lage en ny, for å peke til den opprinnelige behandlingen`() {
        val opphavsvilkår = Opphavsvilkår(behandlingIdFørstegangsbehandling, LocalDateTime.now())
        val vilkårsvurdering =
            Vilkårsvurdering(
                behandlingId = behandlingIdRevurdering,
                delvilkårsvurdering = DelvilkårsvurderingWrapper(emptyList()),
                type = VilkårType.MOR_ELLER_FAR,
                opphavsvilkår = opphavsvilkår,
            )
        val nyttOpphavsvilkår = vilkårsvurdering.opprettOpphavsvilkår()
        assertThat(nyttOpphavsvilkår).isEqualTo(opphavsvilkår)
    }
}
