package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.NesteRegel
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.SvarRegel
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class SagtOppEllerRedusertRegel :
    Vilkårsregel(
        vilkårType = VilkårType.SAGT_OPP_ELLER_REDUSERT,
        regler = setOf(SAGT_OPP_ELLER_REDUSERT, RIMELIG_GRUNN_SAGT_OPP),
        hovedregler = regelIder(SAGT_OPP_ELLER_REDUSERT),
    ) {
    companion object {
        private val RIMELIG_GRUNN_SAGT_OPP =
            RegelSteg(
                regelId = RegelId.RIMELIG_GRUNN_SAGT_OPP,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )

        private val SAGT_OPP_ELLER_REDUSERT =
            RegelSteg(
                regelId = RegelId.SAGT_OPP_ELLER_REDUSERT,
                jaNeiIkkeRelevantSagtOppSvarRegel(
                    hvisJa = NesteRegel(RIMELIG_GRUNN_SAGT_OPP.regelId),
                    hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    hvisIkkeRelevantIkkeFørstegangssøknad = SluttSvarRegel.OPPFYLT,
                ),
            )

        fun jaNeiIkkeRelevantSagtOppSvarRegel(
            hvisJa: SvarRegel = SluttSvarRegel.OPPFYLT,
            hvisNei: SvarRegel = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
            hvisIkkeRelevantIkkeFørstegangssøknad: SvarRegel = SluttSvarRegel.OPPFYLT,
        ): Map<SvarId, SvarRegel> = mapOf(SvarId.JA to hvisJa, SvarId.NEI to hvisNei, SvarId.IKKE_RELEVANT_IKKE_FØRSTEGANGSSØKNAD to hvisIkkeRelevantIkkeFørstegangssøknad)
    }
}
