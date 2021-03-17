package no.nav.familie.ef.sak.regler.validering

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.OppdaterVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VilkårSvarDto
import no.nav.familie.ef.sak.api.dto.svarTilDomene
import no.nav.familie.ef.sak.regler.BegrunnelseType
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelNode
import no.nav.familie.ef.sak.regler.SluttRegel
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.alleVilkårsregler
import no.nav.familie.ef.sak.repository.domain.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering

/**
 * TODO skal validere att siste spørsmål  for hvert delvilkår mangler svar eller er sluttnode, slik att man kan verifisere att man har lagt till neste spørsmål
 */

data class ValideringResultat(val vilkår: Vilkårsresultat,
                              val delvilkår: Map<RegelId, Vilkårsresultat>)

object OppdaterVilkår {

    fun oppdater(vilkårsvurdering: Vilkårsvurdering, vilkårsvurderingDto: OppdaterVilkårsvurderingDto): Vilkårsvurdering {
        val vilkårsregler = alleVilkårsregler().single { it.vilkårType === vilkårsvurdering.type }
        val delvilkår = vilkårsvurderingDto.delvilkårsvurderinger

        validerAttAlleRotvilkårFinnesMed(vilkårsregler, delvilkår)

        val vilkårsresultat = utledVilkårResultat(vilkårsregler, delvilkår)
        val oppdaterteDelvilkår = oppdaterDelvilkår(vilkårsvurderingDto, vilkårsvurdering, vilkårsresultat)
        return vilkårsvurdering.copy(resultat = vilkårsresultat.vilkår,
                                     delvilkårsvurdering = oppdaterteDelvilkår)
    }

    /**
     * Oppdaterer delvilkår
     * Den beholder den opprinnelige rekkefølgen som finnes på delvilkåren i databasen, slik att frontend kan sende inn de i en annen rekkefølge
     */
    private fun oppdaterDelvilkår(vilkårsvurderingDto: OppdaterVilkårsvurderingDto,
                                  vilkårsvurdering: Vilkårsvurdering,
                                  vilkårsresultat: ValideringResultat): DelvilkårsvurderingWrapper {
        val vurderingerPåType = vilkårsvurderingDto.delvilkårsvurderinger.associateBy { it.type }
        val delvilkårsvurderinger = vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger.map {
            if (it.resultat == Vilkårsresultat.SKAL_IKKE_VURDERES) {
                it
            } else {
                val resultat = vilkårsresultat.delvilkår[it.type] ?: throw Feil("Savner resultat for ${it.type}")
                val svar = vurderingerPåType[it.type] ?: throw Feil("Savner svar for ${it.type}")
                it.copy(resultat = resultat,
                        svar = svar.svarTilDomene())
            }
        }.toList()
        return vilkårsvurdering.delvilkårsvurdering.copy(delvilkårsvurderinger = delvilkårsvurderinger)
    }

    private fun utledVilkårResultat(vilkårsregler: Vilkårsregel, delvilkår: List<DelvilkårsvurderingDto>): ValideringResultat {
        val delvilkårResultat = delvilkår.map { vurdering ->
            vurdering.type to validerDelvilkårOgReturerVilkårsresultat(vilkårsregler, vurdering)
        }.toMap()

        return ValideringResultat(utledVilkårResultat(delvilkårResultat), delvilkårResultat)
    }

    fun utledVilkårResultat(delvilkårResultat: Map<RegelId, Vilkårsresultat>): Vilkårsresultat {
        return when {
            delvilkårResultat.values.all { it == Vilkårsresultat.OPPFYLT } -> {
                Vilkårsresultat.OPPFYLT
            }
            delvilkårResultat.values.any { it == Vilkårsresultat.IKKE_TATT_STILLING_TIL } -> {
                Vilkårsresultat.IKKE_TATT_STILLING_TIL
            }
            else -> Vilkårsresultat.IKKE_OPPFYLT
        }
    }

    /**
     * Dette setter foreløpig resultat, men fortsetter å validere resterende svar slik att man fortsatt har ett gyldig svar
     */
    private fun validerDelvilkårOgReturerVilkårsresultat(vilkårsregler: Vilkårsregel,
                                                         vurdering: DelvilkårsvurderingDto): Vilkårsresultat {
        val vilkårType = vilkårsregler.vilkårType
        feilHvis(vurdering.svar.isEmpty()) { "Savner svar for vilkår=$vilkårType regelId=${vurdering.type}" }
        val antallSvar = vurdering.svar.size
        var foreløpigResultat: Vilkårsresultat? = null
        var nesteSvarRegelId: RegelId? = null

        vurdering.svar.forEachIndexed { index, svar ->
            feilHvis(nesteSvarRegelId == null && index != 0) {
                "Har ikke satt neste svar for type=$vilkårType svar=${svar.regelId}"
            }

            val regel = vilkårsregler.regel(svar.regelId)
            val erIkkeSisteSvaret = index != (antallSvar - 1)
            val svarId = svar.svar

            // Hvis man ikke har besvart ett spørsmål så validerer vi att det er det siste spørsmålet, ellers setter vi resultat
            if (svarId == null) {
                feilHvis(erIkkeSisteSvaret) {
                    "Mangler svar på ett spørsmål som ikke er siste besvarte spørsmålet vilkårType=$vilkårType"
                }
                return Vilkårsresultat.IKKE_TATT_STILLING_TIL
            }
            val svarMapping = regel.svarMapping(svarId)
            validerSavnerBegrunnelseHvisUtenBegrunnelse(vilkårType, svarMapping, svar)

            if (manglerPåkrevdBegrunnelse(svarMapping, svar)) {
                foreløpigResultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL
            }

            if (svarMapping is SluttRegel) {
                feilHvis(erIkkeSisteSvaret) {
                    "Finnes ikke noen flere regler, med finnes flere svar vilkårType=$vilkårType svarId=$svarId"
                }
                return foreløpigResultat ?: svarMapping.resultat.vilkårsresultat
            }
            nesteSvarRegelId = svarMapping.regelId
        }
        error("Noe gikk galt, skal ikke komme til sluttet her")
    }

    private fun validerAttAlleRotvilkårFinnesMed(vilkårsregler: Vilkårsregel, delvilkår: List<DelvilkårsvurderingDto>) {
        val delvilkårRegelIdn = delvilkår.map { it.type }
        val rotregler = vilkårsregler.rotregler
        if (!rotregler.containsAll(delvilkårRegelIdn)) {
            throw Feil("Delvilkårsvurderinger savner svar på rotregler - rotregler=$rotregler delvilkår=$delvilkårRegelIdn")
        }
        if(delvilkårRegelIdn.size != rotregler.size) {
            throw Feil("Feil i antall regler dto har ${delvilkårRegelIdn.size} mens vilkår har ${rotregler.size}")
        }
        delvilkår.forEach { validerAttFørsteSvaretErSvarPåFørsteSpørsmål(it) }
    }

    /**
     * Valider att begrunnelse i svaret savnes hvis [RegelNode.begrunnelseType]=[BegrunnelseType.UTEN]
     */
    private fun validerSavnerBegrunnelseHvisUtenBegrunnelse(vilkårType: VilkårType, svarMapping: RegelNode, svar: VilkårSvarDto) {
        if (svarMapping.begrunnelseType == BegrunnelseType.UTEN && svar.begrunnelse != null && svar.begrunnelse.isNotEmpty()) {
            throw Feil("Begrunnelse for vilkårType=$vilkårType svarId=${svar.svar} skal ikke ha begrunnelse")
        }
    }

    /**
     * Validerer att begrunnelse er ifylt hvis [RegelNode.begrunnelseType]=[BegrunnelseType.PÅKREVD]
     */
    private fun manglerPåkrevdBegrunnelse(regelNode: RegelNode, svar: VilkårSvarDto): Boolean =
            regelNode.begrunnelseType == BegrunnelseType.PÅKREVD && svar.begrunnelse?.trim().isNullOrEmpty()

    /**
     * Validerer att regelId på første svaret er det samme som vurderingen sin regelId slik att man sjekker att første svaret er
     */
    private fun validerAttFørsteSvaretErSvarPåFørsteSpørsmål(vurdering: DelvilkårsvurderingDto) {
        val svarRegelId = vurdering.svar.first().regelId
        if (vurdering.type == svarRegelId) {
            throw Feil("Første svaret sin regelId=$svarRegelId er ikke lik vurdering sin regelId=${vurdering.type}")
        }
    }

    private inline fun feilHvis(boolean: Boolean, lazyMessage: () -> String) {
        if (boolean) {
            throw Feil(lazyMessage())
        }
    }
}