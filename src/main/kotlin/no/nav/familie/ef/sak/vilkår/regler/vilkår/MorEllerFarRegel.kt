package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class MorEllerFarRegel : Vilkårsregel(vilkårType = VilkårType.MOR_ELLER_FAR,
                                      regler = setOf(OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN),
                                      hovedregler = regelIder(OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN)) {

    companion object {

        private val OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN =
                RegelSteg(regelId = RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                                       hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE))
    }
}
