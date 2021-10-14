package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class SamlivRegel : Vilkårsregel(vilkårType = VilkårType.SAMLIV,
                                 regler = setOf(LEVER_IKKE_MED_ANNEN_FORELDER, LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD),
                                 hovedregler = regelIder(LEVER_IKKE_MED_ANNEN_FORELDER,
                                                         LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD)) {

    companion object {

        private val LEVER_IKKE_MED_ANNEN_FORELDER =
                RegelSteg(regelId = RegelId.LEVER_IKKE_MED_ANNEN_FORELDER,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                                       hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE))

        private val LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD =
                RegelSteg(regelId = RegelId.LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                                       hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE))
    }
}

