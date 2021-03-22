package no.nav.familie.ef.sak.regler.vilkår

import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttSvarRegel
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.regler.regelIds
import no.nav.familie.ef.sak.repository.domain.VilkårType

class AktivitetRegel : Vilkårsregel(vilkårType = VilkårType.AKTIVITET,
                                    regler = setOf(FYLLER_BRUKER_AKTIVITETSPLIKT),
                                    hovedregler = regelIds(FYLLER_BRUKER_AKTIVITETSPLIKT)) {

    companion object {

        private val FYLLER_BRUKER_AKTIVITETSPLIKT =
                RegelSteg(regelId = RegelId.FYLLER_BRUKER_AKTIVITETSPLIKT,
                          jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                         hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE))

    }
}
