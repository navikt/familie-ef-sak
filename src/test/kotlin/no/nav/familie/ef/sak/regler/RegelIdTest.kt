package no.nav.familie.ef.sak.regler

import no.nav.familie.ef.sak.repository.domain.VilkårType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RegelIdTest {

    @Test
    internal fun `valider att regelId ikke brukes på flere steder`() {
        val sjekkedeRegelIdMap = mutableMapOf<RegelId, VilkårType>()
        vilkårsreglerPåVilkårType.values.forEach { vilkårsregel ->
            vilkårsregel.regler.keys.forEach { regelId ->
                val harLagtTilRegel = sjekkedeRegelIdMap.put(regelId, vilkårsregel.vilkårType) == null
                if (regelId != RegelId.SLUTT_NODE && !harLagtTilRegel) {
                    error("Regel: $regelId er allerede brukt i ${vilkårsregel.vilkårType}")
                }
            }
        }
    }

    @Test
    internal fun `valider att svarsmapping kun refererer til regelId definiert i reglerne til vilkåret`() {
        vilkårsreglerPåVilkårType.values.forEach { vilkårsregel ->
            val definierteRegler = vilkårsregel.regler.keys
            val definierteMappinger = vilkårsregel.regler.values.flatMap {
                regelSteg -> regelSteg.svarMapping.values.map { svarMapping -> svarMapping.regelId }
            }.filter { it != RegelId.SLUTT_NODE }
            assertThat(definierteRegler).containsAll(definierteMappinger)
        }
    }

    @Test
    internal fun `valider att rotregler er definiert i regler for hver vilkår`() {
        vilkårsreglerPåVilkårType.values.forEach {
            assertThat(it.regler.keys).containsAll(it.hovedregler)
        }
    }
}