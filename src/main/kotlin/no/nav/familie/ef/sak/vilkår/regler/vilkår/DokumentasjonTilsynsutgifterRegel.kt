package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class DokumentasjonTilsynsutgifterRegel :
    Vilkårsregel(
        vilkårType = VilkårType.DOKUMENTASJON_TILSYNSUTGIFTER,
        regler = setOf(HAR_DOKUMENTERTE_TILSYNSUTGIFTER),
        hovedregler = regelIder(HAR_DOKUMENTERTE_TILSYNSUTGIFTER),
    ) {
    companion object {
        private val HAR_DOKUMENTERTE_TILSYNSUTGIFTER =
            RegelSteg(
                regelId = RegelId.HAR_DOKUMENTERTE_TILSYNSUTGIFTER,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                        hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
    }
}
