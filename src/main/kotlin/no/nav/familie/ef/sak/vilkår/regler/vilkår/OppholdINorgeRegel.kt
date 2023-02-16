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

class OppholdINorgeRegel : Vilkårsregel(
    vilkårType = VilkårType.LOVLIG_OPPHOLD,
    regler = setOf(BOR_OG_OPPHOLDER_SEG_I_NORGE, OPPHOLD_UNNTAK),
    hovedregler = regelIder(BOR_OG_OPPHOLDER_SEG_I_NORGE)
) {

    companion object {

        private val OPPHOLD_UNNTAK =
            RegelSteg(
                regelId = RegelId.OPPHOLD_UNNTAK,
                svarMapping = mapOf(
                    SvarId.ARBEID_NORSK_ARBEIDSGIVER to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    SvarId.UTENLANDSOPPHOLD_MINDRE_ENN_6_UKER to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    SvarId.OPPHOLDER_SEG_I_ANNET_EØS_LAND to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
                )
            )

        private val BOR_OG_OPPHOLDER_SEG_I_NORGE =
            RegelSteg(
                regelId = RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                svarMapping = jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = NesteRegel(OPPHOLD_UNNTAK.regelId)
                )
            )
    }
}
