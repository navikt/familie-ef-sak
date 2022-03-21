package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.NesteRegel
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class AlderPåBarnRegel : Vilkårsregel(vilkårType = VilkårType.ALDER_PÅ_BARN,
                                      regler = setOf(HAR_ALDER_LAVERE_ENN_GRENSEVERDI, UNNTAK_ALDER),
                                      hovedregler = regelIder(HAR_ALDER_LAVERE_ENN_GRENSEVERDI)) {

    companion object {

        private val unntakAlderMapping =
                setOf(SvarId.TRENGER_MER_TILSYN_ENN_JEVNALDRENDE,
                      SvarId.FORSØRGER_HAR_LANGVARIG_ELLER_UREGELMESSIG_ARBEIDSTID)
                        .associateWith {
                            SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
                        } + mapOf(SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)

        private val UNNTAK_ALDER =
                RegelSteg(regelId = RegelId.UNNTAK_ALDER,
                          svarMapping = unntakAlderMapping)

        private val HAR_ALDER_LAVERE_ENN_GRENSEVERDI =
                RegelSteg(regelId = RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI,
                          svarMapping = jaNeiSvarRegel(hvisJa = NesteRegel(UNNTAK_ALDER.regelId),
                                                       hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE))


    }
}
