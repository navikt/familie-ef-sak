package no.nav.familie.ef.sak.regler.vilkårsregel

import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.VilkårType
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiMapping
import no.nav.familie.ef.sak.regler.regelIds

class NyttBarnSammePartner :
        Vilkårsregel(vilkårType = VilkårType.NYTT_BARN_SAMME_PARTNER,
                     regler = setOf(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER),
                     rotregler = regelIds(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER)) {

    companion object {

        val HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER =
                RegelSteg(regelId = RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER,
                          svarMapping = jaNeiMapping())
    }
}
