package no.nav.familie.ef.sak.vilkår.regler

import no.nav.familie.ef.sak.vilkår.VilkårType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RegelIdTest {

    private val vilkårsregler = Vilkårsregler.VILKÅRSREGLER.vilkårsregler.values

    @Test
    internal fun `valider att regelId ikke brukes på flere steder`() {
        val sjekkedeRegelIdMap = mutableMapOf<RegelId, VilkårType>()
        vilkårsregler.forEach { vilkårsregel ->
            vilkårsregel.regler.keys.forEach { regelId ->
                val harLagtTilRegel = sjekkedeRegelIdMap.put(regelId, vilkårsregel.vilkårType) == null
                if (regelId != RegelId.SLUTT_NODE && !harLagtTilRegel) {
                    error("Regel: $regelId er allerede brukt i ${vilkårsregel.vilkårType}")
                }
            }
        }
    }

    @Test
    internal fun `det finnes en kobling til alle regler`() {
        fun getRegler(vilkårsregel: Vilkårsregel, regelId: RegelId): List<RegelId> {
            val regler = vilkårsregel.regel(regelId).svarMapping.values
                    .map(SvarRegel::regelId)
                    .filterNot { it == RegelId.SLUTT_NODE }
            val childrenRegler = regler.flatMap { getRegler(vilkårsregel, it) }
            return regler + childrenRegler
        }
        vilkårsregler.forEach { vilkårsregel ->
            val alleRegler = vilkårsregel.regler.keys
            val reglerFraHovedregler = vilkårsregel.hovedregler + vilkårsregel.hovedregler
                    .flatMap { getRegler(vilkårsregel, it) }
            assertThat(alleRegler).containsExactlyInAnyOrderElementsOf(reglerFraHovedregler)
        }
    }

    @Test
    internal fun `valider att rotregler er definiert i regler for hver vilkår`() {
        vilkårsregler.forEach {
            assertThat(it.regler.keys).containsAll(it.hovedregler)
        }
    }
}