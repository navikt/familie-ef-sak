package no.nav.familie.ef.sak.vilkår.regler.evalutation

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.BegrunnelseType
import no.nav.familie.ef.sak.vilkår.regler.NesteRegel
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel

class VilkårsregelEnHovedregel :
        Vilkårsregel(VilkårType.ALENEOMSORG,
                     setOf(RegelSteg(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                                     jaNeiSvarRegel(hvisNei = NesteRegel(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE,
                                                                         BegrunnelseType.PÅKREVD))),
                           RegelSteg(regelId = RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE,
                                     svarMapping = jaNeiSvarRegel())),
                     hovedregler = setOf(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE))

class VilkårsregelToHovedregler :
        Vilkårsregel(VilkårType.ALENEOMSORG,
                     setOf(RegelSteg(regelId = RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                                     svarMapping = jaNeiSvarRegel(hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)),
                           RegelSteg(regelId = RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE,
                                     svarMapping = jaNeiSvarRegel())),
                     hovedregler = setOf(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                                         RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE))