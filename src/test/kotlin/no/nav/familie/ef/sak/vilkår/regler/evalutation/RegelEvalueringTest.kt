package no.nav.familie.ef.sak.vilkår.regler.evalutation

import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
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
        assertThat(RegelEvaluering.utledVilkårResultat(mapOf(RegelId.OPPHOLD_UNNTAK to Vilkårsresultat.OPPFYLT,
                                                             RegelId.MEDLEMSKAP_UNNTAK to Vilkårsresultat.IKKE_OPPFYLT)))
                .isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    @Test
    fun `utledVilkårResultat - er IKKE_TATT_STILLING_TIL når det finnes en med IKKE_TATT_STILLING_TIL`() {
        assertThat(RegelEvaluering.utledVilkårResultat(mapOf(RegelId.OPPHOLD_UNNTAK to Vilkårsresultat.OPPFYLT,
                                                             RegelId.MEDLEMSKAP_UNNTAK to Vilkårsresultat.IKKE_OPPFYLT,
                                                             RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE to Vilkårsresultat.IKKE_TATT_STILLING_TIL)))
                .isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }
}