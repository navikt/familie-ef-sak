package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class InntektRegel : Vilkårsregel(vilkårType = VilkårType.INNTEKT,
                                  regler = setOf(LAVERE_INNTEKT_ENN_GRENSEN, SAMSVARER_INNTEKT_MED_OS),
                                  hovedregler = regelIder(LAVERE_INNTEKT_ENN_GRENSEN, SAMSVARER_INNTEKT_MED_OS)) {

    companion object {


        private val lavereInntektEnnGrensenMapping = mapOf(SvarId.JA to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                                           SvarId.NOEN_MÅNEDER_OVERSTIGER_6G to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                                           SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)

        private val samsvarerInntektMedOsMapping = mapOf(SvarId.JA to SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                                         SvarId.BRUKER_MOTTAR_IKKE_OVERGANGSSTØNAD to SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                                         SvarId.NEI to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)

        private val LAVERE_INNTEKT_ENN_GRENSEN =
                RegelSteg(regelId = RegelId.INNTEKT_LAVERE_ENN_INNTEKTSGRENSE,
                          svarMapping = lavereInntektEnnGrensenMapping)

        private val SAMSVARER_INNTEKT_MED_OS = RegelSteg(regelId = RegelId.INNTEKT_SAMSVARER_MED_OS,
                                                         svarMapping = samsvarerInntektMedOsMapping)
    }

}