package no.nav.familie.ef.sak.regler.vilkår

import no.nav.familie.ef.sak.regler.NesteRegel
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttSvarRegel
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.regler.regelIder
import no.nav.familie.ef.sak.repository.domain.VilkårType

class SagtOppEllerRedusertRegel : Vilkårsregel(vilkårType = VilkårType.SAGT_OPP_ELLER_REDUSERT,
                                               regler = setOf(SAGT_OPP_ELLER_REDUSERT, RIMELIG_GRUNN_SAGT_OPP),
                                               hovedregler = regelIder(SAGT_OPP_ELLER_REDUSERT)) {

    companion object {

        private val RIMELIG_GRUNN_SAGT_OPP =
                RegelSteg(regelId = RegelId.RIMELIG_GRUNN_SAGT_OPP,
                          jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                         hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE))

        private val SAGT_OPP_ELLER_REDUSERT =
                RegelSteg(regelId = RegelId.SAGT_OPP_ELLER_REDUSERT,
                          jaNeiSvarRegel(hvisJa = NesteRegel(RIMELIG_GRUNN_SAGT_OPP.regelId),
                                         hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE))

    }
}
