package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class AktivitetArbeidRegel :
    Vilkårsregel(
        vilkårType = VilkårType.AKTIVITET_ARBEID,
        regler = setOf(ER_I_ARBEID_ELLER_SYK),
        hovedregler = regelIder(ER_I_ARBEID_ELLER_SYK),
    ) {
    companion object {
        private val arbeidEllerSykMapping =
            setOf(
                SvarId.ER_I_ARBEID,
                SvarId.ETABLERER_EGEN_VIRKSOMHET,
                SvarId.HAR_FORBIGÅENDE_SYKDOM,
            ).associateWith {
                SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
            } +
                mapOf(SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)
        private val ER_I_ARBEID_ELLER_SYK =
            RegelSteg(
                regelId = RegelId.ER_I_ARBEID_ELLER_FORBIGÅENDE_SYKDOM,
                svarMapping = arbeidEllerSykMapping,
            )
    }
}
