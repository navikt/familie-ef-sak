package no.nav.familie.ef.sak.regler

import no.nav.familie.ef.sak.regler.vilkår.*
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
                SagtOppEllerRedusertRegel(),
                TidligareVedtaksperioderRegel()
        )

fun hentVilkårsregel(vilkårType: VilkårType): Vilkårsregel {
    return Vilkårsregler.VILKÅRSREGLER.vilkårsregler[vilkårType] ?: error("Finner ikke vilkårsregler for vilkårType=$vilkårType")
}

