package no.nav.familie.ef.sak.regler.evalutation

import no.nav.familie.ef.sak.regler.BegrunnelseType
import no.nav.familie.ef.sak.regler.NesteRegel
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttSvarRegel
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.repository.domain.VilkårType

class VilkårsregelEnHovedregel :
        Vilkårsregel(VilkårType.ALENEOMSORG,
                     setOf(RegelSteg(regelId = RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                                     svarMapping = jaNeiSvarRegel(hvisNei = NesteRegel(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE,
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