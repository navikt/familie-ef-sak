package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class AktivitetRegel : Vilkårsregel(vilkårType = VilkårType.AKTIVITET,
                                    regler = setOf(FYLLER_BRUKER_AKTIVITETSPLIKT),
                                    hovedregler = regelIder(FYLLER_BRUKER_AKTIVITETSPLIKT)) {

    companion object {

        private val FYLLER_BRUKER_AKTIVITETSPLIKT =
                RegelSteg(regelId = RegelId.FYLLER_BRUKER_AKTIVITETSPLIKT,
                          jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                         hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE))

    }
}
