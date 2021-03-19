package no.nav.familie.ef.sak.regler.vilkår

import no.nav.familie.ef.sak.regler.NesteRegel
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttSvarRegel
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.regler.regelIds
import no.nav.familie.ef.sak.repository.domain.VilkårType

class ForutgåendeMedlemskapRegel : Vilkårsregel(vilkårType = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                regler = setOf(SØKER_MEDLEM_I_FOLKETRYGDEN, MEDLEMSKAP_UNNTAK),
                                                hovedregler = regelIds(SØKER_MEDLEM_I_FOLKETRYGDEN)) {

    companion object {

        private val unntakSvarMapping = setOf(
                SvarId.MEDLEM_MER_ENN_5_ÅR_AVBRUDD_MINDRE_ENN_10_ÅR,
                SvarId.MEDLEM_MER_ENN_7_ÅR_AVBRUDD_MER_ENN_10ÅR,
                SvarId.I_LANDET_FOR_GJENFORENING_ELLER_GIFTE_SEG,
                SvarId.ANDRE_FORELDER_MEDLEM_SISTE_5_ÅR,
                SvarId.ANDRE_FORELDER_MEDLEM_MINST_5_ÅR_AVBRUDD_MINDRE_ENN_10_ÅR,
                SvarId.ANDRE_FORELDER_MEDLEM_MINST_7_ÅR_AVBRUDD_MER_ENN_10_ÅR,
                SvarId.TOTALVURDERING_OPPFYLLER_FORSKRIFT)
                                                .map { it to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE }.toMap() +
                                        mapOf(SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)
        private val MEDLEMSKAP_UNNTAK =
                RegelSteg(regelId = RegelId.MEDLEMSKAP_UNNTAK,
                          svarMapping = unntakSvarMapping)

        private val SØKER_MEDLEM_I_FOLKETRYGDEN =
                RegelSteg(regelId = RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                                       hvisNei = NesteRegel(MEDLEMSKAP_UNNTAK.regelId)))
    }

}