package no.nav.familie.ef.sak.regler.validering

import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.validering.OppdaterVilkår.utledVilkårResultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OppdaterVilkårTest {


    @Test
    internal fun `utledVilkårResultat - er OPPFYLT når alle vilkår er OPPFYLT`() {
        assertThat(utledVilkårResultat(mapOf(RegelId.OPPHOLD_UNNTAK to Vilkårsresultat.OPPFYLT)))
                .isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    internal fun `utledVilkårResultat - er IKKE_OPPFYLT når det finnes en med IKKE_OPPFYLT`() {
        assertThat(utledVilkårResultat(mapOf(RegelId.OPPHOLD_UNNTAK to Vilkårsresultat.OPPFYLT,
                                             RegelId.MEDLEMSKAP_UNNTAK to Vilkårsresultat.IKKE_OPPFYLT)))
                .isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    @Test
    internal fun `utledVilkårResultat - er IKKE_TATT_STILLING_TIL når det finnes en med IKKE_TATT_STILLING_TIL`() {
        assertThat(utledVilkårResultat(mapOf(RegelId.OPPHOLD_UNNTAK to Vilkårsresultat.OPPFYLT,
                                             RegelId.MEDLEMSKAP_UNNTAK to Vilkårsresultat.IKKE_OPPFYLT,
                                             RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE to Vilkårsresultat.IKKE_TATT_STILLING_TIL)))
                .isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }
}