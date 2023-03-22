package no.nav.familie.ef.sak.vilkår.regler.vilkår.skolepenger

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.NesteRegel
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class UtdanningErHensiktsmessigRegel : Vilkårsregel(
    vilkårType = VilkårType.ER_UTDANNING_HENSIKTSMESSIG,
    regler = setOf(NAVKONTOR_VURDERING, SAKSBEHANDLER_VURDERING),
    hovedregler = regelIder(NAVKONTOR_VURDERING),
) {

    companion object {
        private val NAVKONTOR_VURDERING =
            RegelSteg(
                regelId = RegelId.NAVKONTOR_VURDERING,
                svarMapping = jaNeiSvarRegel(
                    hvisJa = NesteRegel(RegelId.SAKSBEHANDLER_VURDERING),
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )

        private val SAKSBEHANDLER_VURDERING =
            RegelSteg(
                regelId = RegelId.SAKSBEHANDLER_VURDERING,
                svarMapping = jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )
    }
}
