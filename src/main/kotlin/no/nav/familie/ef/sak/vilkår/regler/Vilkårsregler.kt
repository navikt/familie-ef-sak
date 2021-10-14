package no.nav.familie.ef.sak.vilkår.regler

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AktivitetRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AleneomsorgRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.ForutgåendeMedlemskapRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.MorEllerFarRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.NyttBarnSammePartnerRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.OppholdINorgeRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SagtOppEllerRedusertRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SamlivRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.TidligareVedtaksperioderRegel

/**
 * Singleton for å holde på alle regler
 */
class Vilkårsregler private constructor(val vilkårsregler: Map<VilkårType, Vilkårsregel>) {

    companion object {

        val VILKÅRSREGLER = Vilkårsregler(alleVilkårsregler.associateBy { it.vilkårType })
    }
}

val alleVilkårsregler: List<Vilkårsregel> =
        listOf(
                ForutgåendeMedlemskapRegel(),
                OppholdINorgeRegel(),
                MorEllerFarRegel(),
                SivilstandRegel(),
                SamlivRegel(),
                AleneomsorgRegel(),
                NyttBarnSammePartnerRegel(),
                AktivitetRegel(),
                SagtOppEllerRedusertRegel(),
                TidligareVedtaksperioderRegel()
        )

fun hentVilkårsregel(vilkårType: VilkårType): Vilkårsregel {
    return Vilkårsregler.VILKÅRSREGLER.vilkårsregler[vilkårType] ?: error("Finner ikke vilkårsregler for vilkårType=$vilkårType")
}

