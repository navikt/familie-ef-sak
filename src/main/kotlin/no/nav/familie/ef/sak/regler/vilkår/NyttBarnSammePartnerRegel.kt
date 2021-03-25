package no.nav.familie.ef.sak.regler.vilkår

import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttSvarRegel
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.regler.regelIder
import no.nav.familie.ef.sak.repository.domain.VilkårType

class NyttBarnSammePartnerRegel : Vilkårsregel(vilkårType = VilkårType.NYTT_BARN_SAMME_PARTNER,
                                               regler = setOf(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER),
                                               hovedregler = regelIder(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER)) {

    companion object {

        private val HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER =
                RegelSteg(regelId = RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                                       hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE))
    }
}
