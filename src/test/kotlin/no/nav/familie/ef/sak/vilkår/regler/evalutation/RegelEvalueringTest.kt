package no.nav.familie.ef.sak.vilkår.regler.evalutation

import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.VurderingDto
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AlderPåBarnRegel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RegelEvalueringTest {

    @Test
    fun `utledVilkårResultat - er OPPFYLT når alle vilkår er OPPFYLT`() {
        assertThat(RegelEvaluering.utledVilkårResultat(mapOf(RegelId.OPPHOLD_UNNTAK to Vilkårsresultat.OPPFYLT)))
            .isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `utledVilkårResultat - er IKKE_OPPFYLT når det finnes en med IKKE_OPPFYLT`() {
        assertThat(
            RegelEvaluering.utledVilkårResultat(
                mapOf(
                    RegelId.OPPHOLD_UNNTAK to Vilkårsresultat.OPPFYLT,
                    RegelId.MEDLEMSKAP_UNNTAK to Vilkårsresultat.IKKE_OPPFYLT,
                ),
            ),
        )
            .isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    @Test
    fun `utledVilkårResultat - utledResultat skal gi AUTOMATISK_OPPFYLT når alle delvilkår er AUTOMATISK_OPPFYLT`() {
        assertThat(
            RegelEvaluering.utledVilkårResultat(
                mapOf(
                    RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI to Vilkårsresultat.AUTOMATISK_OPPFYLT,
                ),
            ),
        )
            .isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `utledVilkårResultat - utledResultat skal gi OPPFYLT når delvilkår er AUTOMATISK_OPPFYLT`() {
        val vurderingDto = VurderingDto(RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI, SvarId.NEI)
        val delvilkår = DelvilkårsvurderingDto(Vilkårsresultat.AUTOMATISK_OPPFYLT, listOf(vurderingDto))

        val regelResultat = RegelEvaluering.utledResultat(AlderPåBarnRegel(), listOf(delvilkår))
        assertThat(regelResultat.vilkår).isEqualTo(Vilkårsresultat.OPPFYLT)

        assertThat(regelResultat.delvilkår.keys.size).isEqualTo(1)
        assertThat(regelResultat.delvilkår.keys.first()).isEqualTo(RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI)
        assertThat(regelResultat.delvilkår.values.size).isEqualTo(1)
        assertThat(regelResultat.delvilkår.values.first()).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `utledVilkårResultat - er IKKE_TATT_STILLING_TIL når det finnes en med IKKE_TATT_STILLING_TIL`() {
        assertThat(
            RegelEvaluering.utledVilkårResultat(
                mapOf(
                    RegelId.OPPHOLD_UNNTAK to Vilkårsresultat.OPPFYLT,
                    RegelId.MEDLEMSKAP_UNNTAK to Vilkårsresultat.IKKE_OPPFYLT,
                    RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE to
                        Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                ),
            ),
        )
            .isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }
}
