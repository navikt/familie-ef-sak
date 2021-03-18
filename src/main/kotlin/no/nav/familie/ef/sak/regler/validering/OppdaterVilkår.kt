package no.nav.familie.ef.sak.regler.validering

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VilkårSvarDto
import no.nav.familie.ef.sak.api.dto.svarTilDomene
import no.nav.familie.ef.sak.regler.BegrunnelseType
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelNode
import no.nav.familie.ef.sak.regler.SluttRegel
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.repository.domain.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.repository.domain.VilkårSvar
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering

/**
 * TODO skal validere att siste spørsmål  for hvert delvilkår mangler svar eller er sluttnode, slik att man kan verifisere att man har lagt till neste spørsmål
 * TODO legge inn att det er greit for frontend å sende inn re
 */

/**
 * @param vilkårType type vilkår
 * @param vilkår [Vilkårsresultat] for vilkåret
 * @param delvilkår [Vilkårsresultat] for hvert delvilkår/rotRegelId
 */
private data class ValideringResultat(val vilkårType: VilkårType,
                                      val vilkår: Vilkårsresultat,
                                      val delvilkår: Map<RegelId, Vilkårsresultat>) {

    fun rotVilkårsresultat(rotRegelId: RegelId) =
            delvilkår[rotRegelId] ?: throw Feil("Savner resultat for regelId=$rotRegelId vilkårType=$vilkårType")
}

object OppdaterVilkår {

    fun validerOgOppdater(vilkårsvurdering: Vilkårsvurdering,
                          vilkårsregler: List<Vilkårsregel>,
                          oppdatering: List<DelvilkårsvurderingDto>): Vilkårsvurdering {
        val vilkårsregel = vilkårsregler.single { it.vilkårType === vilkårsvurdering.type }

        validerVurdering(vilkårsregel, oppdatering)

        val vilkårsresultat = utledVilkårResultat(vilkårsregel, oppdatering)
        val oppdaterteDelvilkår = oppdaterDelvilkår(vilkårsvurdering, vilkårsresultat, oppdatering)
        return vilkårsvurdering.copy(resultat = vilkårsresultat.vilkår,
                                     delvilkårsvurdering = oppdaterteDelvilkår)
    }

    /**
     * Oppdaterer delvilkår
     * Den beholder den opprinnelige rekkefølgen som finnes på delvilkåren i databasen, slik att frontend kan sende inn de i en annen rekkefølge
     *
     * @param vilkårsvurdering Vilkårsoppdatering fra databasen som skal oppdateres
     */
    private fun oppdaterDelvilkår(vilkårsvurdering: Vilkårsvurdering,
                                  vilkårsresultat: ValideringResultat,
                                  oppdatering: List<DelvilkårsvurderingDto>): DelvilkårsvurderingWrapper {
        val vurderingerPåType = oppdatering.associateBy { it.svar.first().regelId }
        val delvilkårsvurderinger = vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger.map {
            if (it.resultat == Vilkårsresultat.IKKE_AKTUELL) {
                it
            } else {
                val rotRegelId = it.rotRegelId
                val resultat = vilkårsresultat.rotVilkårsresultat(rotRegelId)
                val svar = vurderingerPåType[rotRegelId] ?: throw Feil("Savner svar for rotRegelId=$rotRegelId")

                //TODO burde man ha en endepunkte som settes att man ikke skal vurdere ett delvilkår?
                if (resultat == Vilkårsresultat.SKAL_IKKE_VURDERES) {
                    // Setter svar til initiellt verdi
                    it.copy(resultat = resultat,
                            svar = listOf(VilkårSvar(rotRegelId)))
                } else {
                    it.copy(resultat = resultat,
                            svar = svar.svarTilDomene())
                }
            }
        }.toList()
        return vilkårsvurdering.delvilkårsvurdering.copy(delvilkårsvurderinger = delvilkårsvurderinger)
    }

    private fun utledVilkårResultat(vilkårsregel: Vilkårsregel, delvilkår: List<DelvilkårsvurderingDto>): ValideringResultat {
        val delvilkårResultat = delvilkår.map { vurdering ->
            vurdering.rootRegelId() to validerDelvilkårOgReturerVilkårsresultat(vilkårsregel, vurdering)
        }.toMap()
        return ValideringResultat(vilkårType = vilkårsregel.vilkårType,
                                  vilkår = utledVilkårResultat(delvilkårResultat),
                                  delvilkår = delvilkårResultat)
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

    private fun validerVurdering(vilkårsregel: Vilkårsregel,
                                 oppdatering: List<DelvilkårsvurderingDto>) {
        validerAttAlleDelvilkårHarMinimumEttSvar(vilkårsregel.vilkårType, oppdatering)
        validerAttAlleRotvilkårFinnesMed(vilkårsregel, oppdatering)

        oppdatering.forEach { delvilkårsvurderingDto ->
            validerDelviljår(vilkårsregel, delvilkårsvurderingDto)
        }
    }

    private fun validerDelviljår(vilkårsregel: Vilkårsregel,
                                 delvilkårsvurderingDto: DelvilkårsvurderingDto) {
        val vilkårType = vilkårsregel.vilkårType
        delvilkårsvurderingDto.svar.forEachIndexed { index, svar ->
            val (regelId: RegelId, svarId: SvarId?, _) = svar
            val regelMapping = vilkårsregel.regel(regelId)
            val erIkkeSisteSvaret = index != (delvilkårsvurderingDto.svar.size - 1)

            if (svarId == null) {
                feilHvis(erIkkeSisteSvaret) {
                    throw Feil("Mangler svar på ett spørsmål som ikke er siste besvarte spørsmålet vilkårType=$vilkårType regelId=$regelId")
                }
            } else {
                val svarMapping = regelMapping.svarMapping(svarId)
                validerSavnerBegrunnelseHvisUtenBegrunnelse(vilkårType, svarMapping, svar)
                feilHvis(svarMapping is SluttRegel && erIkkeSisteSvaret) {
                    "Finnes ikke noen flere regler, men finnes flere svar vilkårType=$vilkårType svarId=$svarId"
                }
            }
        }
    }

    /**
     * Dette setter foreløpig resultat, men fortsetter å validere resterende svar slik att man fortsatt har ett gyldig svar
     */
    private fun validerDelvilkårOgReturerVilkårsresultat(vilkårsregel: Vilkårsregel,
                                                         vurdering: DelvilkårsvurderingDto): Vilkårsresultat {
        vurdering.svar.forEach { svar ->
            val regel = vilkårsregel.regel(svar.regelId)
            val svarId = svar.svar ?: return Vilkårsresultat.IKKE_TATT_STILLING_TIL
            val svarMapping = regel.svarMapping(svarId)

            if (manglerPåkrevdBegrunnelse(svarMapping, svar)) {
                return Vilkårsresultat.IKKE_TATT_STILLING_TIL
            }

            if (svarMapping is SluttRegel) {
                return svarMapping.resultat.vilkårsresultat
            }
        }
        error("Noe gikk galt, skal ikke komme til sluttet her")
    }

    /**
     * Skal validere att man sender inn minimum ett svar for ett delvilkår
     * Når backend initierar [Delvilkårsvurdering] så legges ett første svar in med regelId rotspørsmålet
     */
    private fun validerAttAlleDelvilkårHarMinimumEttSvar(vilkårType: VilkårType, oppdatering: List<DelvilkårsvurderingDto>) {
        oppdatering.forEach { vurdering ->
            feilHvis(vurdering.svar.isEmpty()) { "Savner svar for en av delvilkåren for vilkår=$vilkårType" }
        }
    }

    private fun validerAttAlleRotvilkårFinnesMed(vilkårsregel: Vilkårsregel, delvilkår: List<DelvilkårsvurderingDto>) {
        val delvilkårRegelIdn = delvilkår.map { it.rootRegelId() }
        val regelDelvilkår = vilkårsregel.rotregler
        if (!regelDelvilkår.containsAll(delvilkårRegelIdn)) {
            throw Feil("Delvilkårsvurderinger savner svar på rotregler - rotregler=$regelDelvilkår delvilkår=$delvilkårRegelIdn")
        }
        if (delvilkårRegelIdn.size != regelDelvilkår.size) {
            throw Feil("Feil i antall regler dto har ${delvilkårRegelIdn.size} mens vilkår har ${regelDelvilkår.size}")
        }
    }

    /**
     * Valider att begrunnelse i svaret savnes hvis [RegelNode.begrunnelseType]=[BegrunnelseType.UTEN]
     */
    private fun validerSavnerBegrunnelseHvisUtenBegrunnelse(vilkårType: VilkårType, svarMapping: RegelNode, svar: VilkårSvarDto) {
        if (svarMapping.begrunnelseType == BegrunnelseType.UTEN && svar.begrunnelse != null && svar.begrunnelse.isNotEmpty()) {
            throw Feil("Begrunnelse for vilkårType=$vilkårType regelId=${svar.regelId} svarId=${svar.svar} skal ikke ha begrunnelse")
        }
    }

    /**
     * Validerer att begrunnelse er ifylt hvis [RegelNode.begrunnelseType]=[BegrunnelseType.PÅKREVD]
     */
    private fun manglerPåkrevdBegrunnelse(regelNode: RegelNode, svar: VilkårSvarDto): Boolean =
            regelNode.begrunnelseType == BegrunnelseType.PÅKREVD && svar.begrunnelse?.trim().isNullOrEmpty()

    private fun DelvilkårsvurderingDto.rootRegelId() = this.svar.first().regelId

    private inline fun feilHvis(boolean: Boolean, lazyMessage: () -> String) {
        if (boolean) {
            throw Feil(lazyMessage())
        }
    }
}