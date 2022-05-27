package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class InntektRegel : Vilkårsregel(
    vilkårType = VilkårType.INNTEKT,
    regler = setOf(LAVERE_INNTEKT_ENN_GRENSEN),
    hovedregler = regelIder(LAVERE_INNTEKT_ENN_GRENSEN)
) {

    companion object {

        private val inntektRegelMapping = mapOf(
            SvarId.JA to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
            SvarId.NOEN_MÅNEDER_OVERSTIGER_6G to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
            SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
        )

        private val LAVERE_INNTEKT_ENN_GRENSEN =
            RegelSteg(
                regelId = RegelId.INNTEKT_LAVERE_ENN_INNTEKTSGRENSE,
                svarMapping = inntektRegelMapping
            )
    }
}
