package no.nav.familie.ef.sak.regler.evalutation

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.SluttSvarRegel
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat

/**
 * @param vilkårType type vilkår
 * @param vilkår [Vilkårsresultat] for vilkåret
 * @param delvilkår [Vilkårsresultat] for hver hovedregel
 */
data class RegelResultat(val vilkårType: VilkårType,
                         val vilkår: Vilkårsresultat,
                         val delvilkår: Map<RegelId, Vilkårsresultat>) {

    fun resultatHovedregel(hovedregel: RegelId) =
            delvilkår[hovedregel] ?: throw Feil("Savner resultat for regelId=$hovedregel vilkårType=$vilkårType")
}

object RegelEvaluering {

    /**
     * @return [RegelResultat] med resultat for vilkåret og delvilkår
     */
    fun utledResultat(vilkårsregel: Vilkårsregel, delvilkår: List<DelvilkårsvurderingDto>): RegelResultat {
        val delvilkårResultat = delvilkår.map { vurdering ->
            vurdering.hovedregel() to utledResultatForDelvilkår(vilkårsregel, vurdering)
        }.toMap()
        return RegelResultat(vilkårType = vilkårsregel.vilkårType,
                             vilkår = utledVilkårResultat(delvilkårResultat),
                             delvilkår = delvilkårResultat)
    }

    fun utledVilkårResultat(delvilkårResultat: Map<RegelId, Vilkårsresultat>): Vilkårsresultat {
        return when {
            delvilkårResultat.values.all { it == Vilkårsresultat.OPPFYLT } -> Vilkårsresultat.OPPFYLT
            delvilkårResultat.values.all { it == Vilkårsresultat.OPPFYLT || it == Vilkårsresultat.IKKE_OPPFYLT } ->
                Vilkårsresultat.IKKE_OPPFYLT
            delvilkårResultat.values.any { it == Vilkårsresultat.IKKE_TATT_STILLING_TIL } -> Vilkårsresultat.IKKE_TATT_STILLING_TIL
            delvilkårResultat.values.any { it == Vilkårsresultat.SKAL_IKKE_VURDERES } -> Vilkårsresultat.SKAL_IKKE_VURDERES //TODO?
            else -> error("Håndterer ikke situasjonen med resultat=${delvilkårResultat.values}")
        }
    }

    /**
     * Dette setter foreløpig resultat, men fortsetter å validere resterende svar slik att man fortsatt har ett gyldig svar
     */
    private fun utledResultatForDelvilkår(vilkårsregel: Vilkårsregel,
                                          vurdering: DelvilkårsvurderingDto): Vilkårsresultat {
        vurdering.vurderinger.forEach { svar ->
            val regel = vilkårsregel.regel(svar.regelId)
            val svarId = svar.svar ?: return Vilkårsresultat.IKKE_TATT_STILLING_TIL
            val svarMapping = regel.svarMapping(svarId)

            if (RegelValidering.manglerPåkrevdBegrunnelse(svarMapping, svar)) {
                return Vilkårsresultat.IKKE_TATT_STILLING_TIL
            }

            if (svarMapping is SluttSvarRegel) {
                return svarMapping.resultat.vilkårsresultat
            }
        }
        error("Noe gikk galt, skal ikke komme til sluttet her")
    }

}