package no.nav.familie.ef.sak.regler.vilkårsregel

import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttRegel
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiMapping
import no.nav.familie.ef.sak.regler.regelIds
import no.nav.familie.ef.sak.repository.domain.VilkårType

class NyttBarnSammePartnerRegel : Vilkårsregel(vilkårType = VilkårType.NYTT_BARN_SAMME_PARTNER,
                                               regler = setOf(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER),
                                               hovedregler = regelIds(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER)) {

    companion object {

        private val HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER =
                RegelSteg(regelId = RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER,
                          svarMapping = jaNeiMapping(hvisJa = SluttRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                                     hvisNei = SluttRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE))
    }
}
