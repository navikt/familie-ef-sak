package no.nav.familie.ef.sak.vilkår.regler.vilkår.skolepenger

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class DokumentasjonAvUtdanningRegel :
    Vilkårsregel(
        vilkårType = VilkårType.DOKUMENTASJON_AV_UTDANNING,
        regler = setOf(DOKUMENTASJON_AV_UTDANNING, DOKUMENTASJON_AV_UTGIFTER_UTDANNING),
        hovedregler = regelIder(DOKUMENTASJON_AV_UTDANNING, DOKUMENTASJON_AV_UTGIFTER_UTDANNING),
    ) {
    companion object {
        private val DOKUMENTASJON_AV_UTDANNING =
            RegelSteg(
                regelId = RegelId.DOKUMENTASJON_AV_UTDANNING,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )

        private val DOKUMENTASJON_AV_UTGIFTER_UTDANNING =
            RegelSteg(
                regelId = RegelId.DOKUMENTASJON_AV_UTGIFTER_UTDANNING,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )
    }
}
