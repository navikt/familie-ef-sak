package no.nav.familie.ef.sak.regler.vilkårsregel

import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttSvarRegel
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.regler.regelIds
import no.nav.familie.ef.sak.repository.domain.VilkårType

class SamlivRegel : Vilkårsregel(vilkårType = VilkårType.SAMLIV,
                                 regler = setOf(LEVER_IKKE_MED_ANNEN_FORELDER, LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD),
                                 hovedregler = regelIds(LEVER_IKKE_MED_ANNEN_FORELDER,
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

