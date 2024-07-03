package no.nav.familie.ef.sak.vilkår.regler.vilkår.skolepenger

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class RettTilOvergangsstønadRegel :
    Vilkårsregel(
        vilkårType = VilkårType.RETT_TIL_OVERGANGSSTØNAD,
        regler = setOf(RETT_TIL_OVERGANGSSTØNAD),
        hovedregler = regelIder(RETT_TIL_OVERGANGSSTØNAD),
    ) {
    companion object {
        private val RETT_TIL_OVERGANGSSTØNAD =
            RegelSteg(
                regelId = RegelId.RETT_TIL_OVERGANGSSTØNAD,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )
    }
}
