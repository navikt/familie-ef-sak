package no.nav.familie.ef.sak.regler.vilkårsregel.vilkårsregel

import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.VilkårType
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiMapping
import no.nav.familie.ef.sak.regler.regelIds

class MorEllerFar : Vilkårsregel(vilkårType = VilkårType.MOR_ELLER_FAR,
                                 regler = setOf(harBrukerOmsorgForBarn),
                                 rotregler = regelIds(harBrukerOmsorgForBarn)) {

    companion object {

        val harBrukerOmsorgForBarn =
                RegelSteg(regelId = RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN,
                          svarMapping = jaNeiMapping())
    }
}
