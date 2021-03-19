package no.nav.familie.ef.sak.regler

import no.nav.familie.ef.sak.regler.vilkårsregel.AleneomsorgRegel
import no.nav.familie.ef.sak.regler.vilkårsregel.ForutgåendeMedlemskapRegel
import no.nav.familie.ef.sak.regler.vilkårsregel.MorEllerFarRegel
import no.nav.familie.ef.sak.regler.vilkårsregel.NyttBarnSammePartnerRegel
import no.nav.familie.ef.sak.regler.vilkårsregel.OppholdINorgeRegel
import no.nav.familie.ef.sak.regler.vilkårsregel.SamlivRegel
import no.nav.familie.ef.sak.regler.vilkårsregel.SivilstandRegel
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
                NyttBarnSammePartnerRegel()
        )

