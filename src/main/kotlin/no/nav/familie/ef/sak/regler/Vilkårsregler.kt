package no.nav.familie.ef.sak.regler

import no.nav.familie.ef.sak.regler.vilkår.AktivitetRegel
import no.nav.familie.ef.sak.regler.vilkår.AleneomsorgRegel
import no.nav.familie.ef.sak.regler.vilkår.ForutgåendeMedlemskapRegel
import no.nav.familie.ef.sak.regler.vilkår.MorEllerFarRegel
import no.nav.familie.ef.sak.regler.vilkår.NyttBarnSammePartnerRegel
import no.nav.familie.ef.sak.regler.vilkår.OppholdINorgeRegel
import no.nav.familie.ef.sak.regler.vilkår.SagtOppEllerRedusertRegel
import no.nav.familie.ef.sak.regler.vilkår.SamlivRegel
import no.nav.familie.ef.sak.regler.vilkår.SivilstandRegel
import no.nav.familie.ef.sak.repository.domain.VilkårType

/**
 * Singleton for å holde på alle regler
 */
data class Vilkårsregler private constructor(val vilkårsregler: Map<VilkårType, Vilkårsregel>) {

    companion object {

        val VILKÅRSREGLER = Vilkårsregler(alleVilkårsregler.map { it.vilkårType to it }.toMap())
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
                SagtOppEllerRedusertRegel()
        )

fun hentVilkårsregel(vilkårType: VilkårType): Vilkårsregel {
    return Vilkårsregler.VILKÅRSREGLER.vilkårsregler[vilkårType] ?: error("Finner ikke vilkårsregler for vilkårType=$vilkårType")
}

