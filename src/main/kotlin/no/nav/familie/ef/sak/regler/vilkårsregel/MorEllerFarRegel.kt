package no.nav.familie.ef.sak.regler.vilkårsregel

import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttSvarRegel
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.regler.regelIds
import no.nav.familie.ef.sak.repository.domain.VilkårType

class MorEllerFarRegel : Vilkårsregel(vilkårType = VilkårType.MOR_ELLER_FAR,
                                      regler = setOf(OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN),
                                      hovedregler = regelIds(OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN)) {

    companion object {

        private val OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN =
                RegelSteg(regelId = RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                                       hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE))
    }
}
