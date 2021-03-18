package no.nav.familie.ef.sak.regler.validering

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VurderingDto
import no.nav.familie.ef.sak.api.dto.svarTilDomene
import no.nav.familie.ef.sak.regler.BegrunnelseType
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelNode
import no.nav.familie.ef.sak.regler.SluttRegel
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.Vilkårsregler.Companion.VILKÅRSREGLER
import no.nav.familie.ef.sak.repository.domain.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering

/**
 * @param vilkårType type vilkår
 * @param vilkår [Vilkårsresultat] for vilkåret
 * @param delvilkår [Vilkårsresultat] for hver hovedregel
 */
private data class ValideringResultat(val vilkårType: VilkårType,
                                      val vilkår: Vilkårsresultat,
                                      val delvilkår: Map<RegelId, Vilkårsresultat>) {

    fun resultatHovedregel(hovedregel: RegelId) =
            delvilkår[hovedregel] ?: throw Feil("Savner resultat for regelId=$hovedregel vilkårType=$vilkårType")
}

object OppdaterVilkår {

    /**
     * Oppdaterer [Vilkårsvurdering] med nye svar og resultat
     * Validerer att svaren er gyldige
     */
    fun lagNyOppdatertVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering,
                                       oppdatering: List<DelvilkårsvurderingDto>,
                                       vilkårsregler: Map<VilkårType, Vilkårsregel> = VILKÅRSREGLER.vilkårsregler): Vilkårsvurdering {
        val vilkårsregel = vilkårsregler[vilkårsvurdering.type] ?: error("Finner ikke vilkårsregler for ${vilkårsvurdering.type}")

        validerVurdering(vilkårsregel, oppdatering)

        val vilkårsresultat = utledResultat(vilkårsregel, oppdatering)
        validerAttResultatErOppfyltEllerIkkeOppfylt(vilkårsresultat)
        val oppdaterteDelvilkår = oppdaterDelvilkår(vilkårsvurdering, vilkårsresultat, oppdatering)
        return vilkårsvurdering.copy(resultat = vilkårsresultat.vilkår,
                                     delvilkårsvurdering = oppdaterteDelvilkår)
    }

    /**
     * Då vi ikke helt har støtte for [Vilkårsresultat.SKAL_IKKE_VURDERES] ennå så skal svaret være
     * [Vilkårsresultat.IKKE_OPPFYLT] eller [Vilkårsresultat.OPPFYLT]
     * Når man legger til funksjonalitet for SKAL_IKKE_VURDERES, hva skal resultatet være?
     */
    private fun validerAttResultatErOppfyltEllerIkkeOppfylt(vilkårsresultat: ValideringResultat) {
        if (!vilkårsresultat.vilkår.oppfyltEllerIkkeOppfylt()) {
            val message = "Støtter ikke mellomlagring ennå, må håndtere ${vilkårsresultat.vilkår}. " +
                          "Ett resultat på vurderingen må bli oppfylt eller ikke oppfylt"
            throw Feil(message = message, frontendFeilmelding = message)
        }
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
        val vurderingerPåType = oppdatering.associateBy { it.vurderinger.first().regelId }
        val delvilkårsvurderinger = vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger.map {
            if (it.resultat == Vilkårsresultat.IKKE_AKTUELL) {
                it
            } else {
                val hovedregel = it.hovedregel
                val resultat = vilkårsresultat.resultatHovedregel(hovedregel)
                val svar = vurderingerPåType[hovedregel] ?: throw Feil("Savner svar for hovedregel=$hovedregel")

                if (resultat.oppfyltEllerIkkeOppfylt()) {
                    it.copy(resultat = resultat,
                            vurderinger = svar.svarTilDomene())
                } else {
                    // TODO håndtering for [Vilkårsresultat.SKAL_IKKE_VURDERES] som burde beholde første svaret i det delvilkåret
                    throw Feil("Håndterer ikke oppdatering av resultat=$resultat ennå")
                }
            }
        }.toList()
        return vilkårsvurdering.delvilkårsvurdering.copy(delvilkårsvurderinger = delvilkårsvurderinger)
    }

    /**
     * @return [ValideringResultat] med resultat for vilkåret og delvilkår
     */
    private fun utledResultat(vilkårsregel: Vilkårsregel, delvilkår: List<DelvilkårsvurderingDto>): ValideringResultat {
        val delvilkårResultat = delvilkår.map { vurdering ->
            vurdering.hovedregel() to utledDelvilkårsresultat(vilkårsregel, vurdering)
        }.toMap()
        return ValideringResultat(vilkårType = vilkårsregel.vilkårType,
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

    private fun validerVurdering(vilkårsregel: Vilkårsregel,
                                 oppdatering: List<DelvilkårsvurderingDto>) {
        validerAttAlleDelvilkårHarMinimumEttSvar(vilkårsregel.vilkårType, oppdatering)
        validerAttAlleHovedreglerFinnesMed(vilkårsregel, oppdatering)

        oppdatering.forEach { delvilkårsvurderingDto ->
            validerDelvilkår(vilkårsregel, delvilkårsvurderingDto)
        }
    }

    /**
     * Kaster feil hvis
     *
     * * ett [Vurdering] savner svar OG ikke er det siste svaret, eks svaren [ja, null, nei]
     *
     * * har begrunnelse men er [BegrunnelseType.UTEN]
     *
     * * ett svar er av typen [SluttRegel] men att det finnes flere svar, eks [ja, nei, ja],
     *   hvor det andre svaret egentlige er type [SluttRegel]
     *
     */
    private fun validerDelvilkår(vilkårsregel: Vilkårsregel,
                                 delvilkårsvurderingDto: DelvilkårsvurderingDto) {
        val vilkårType = vilkårsregel.vilkårType
        delvilkårsvurderingDto.vurderinger.forEachIndexed { index, svar ->
            val (regelId: RegelId, svarId: SvarId?, _) = svar
            val regelMapping = vilkårsregel.regel(regelId)
            val erIkkeSisteSvaret = index != (delvilkårsvurderingDto.vurderinger.size - 1)

            if (svarId == null) {
                feilHvis(erIkkeSisteSvaret) {
                    "Mangler svar på ett spørsmål som ikke er siste besvarte spørsmålet vilkårType=$vilkårType regelId=$regelId"
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
    private fun utledDelvilkårsresultat(vilkårsregel: Vilkårsregel,
                                        vurdering: DelvilkårsvurderingDto): Vilkårsresultat {
        vurdering.vurderinger.forEach { svar ->
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
     * Når backend initierar [Delvilkårsvurdering] så legges ett første svar in med regelId(hovedregel) for hvert delvilkår
     */
    private fun validerAttAlleDelvilkårHarMinimumEttSvar(vilkårType: VilkårType, oppdatering: List<DelvilkårsvurderingDto>) {
        oppdatering.forEach { vurdering ->
            feilHvis(vurdering.vurderinger.isEmpty()) { "Savner svar for en av delvilkåren for vilkår=$vilkårType" }
        }
    }

    private fun validerAttAlleHovedreglerFinnesMed(vilkårsregel: Vilkårsregel, delvilkår: List<DelvilkårsvurderingDto>) {
        val delvilkårRegelIdn = delvilkår.map { it.hovedregel() }
        val hovedregler = vilkårsregel.hovedregler
        feilHvis(!hovedregler.containsAll(delvilkårRegelIdn)) {
            "Delvilkårsvurderinger savner svar på rotregler - rotregler=$hovedregler delvilkår=$delvilkårRegelIdn"
        }
        feilHvis(delvilkårRegelIdn.size != hovedregler.size) {
            "Feil i antall regler dto har ${delvilkårRegelIdn.size} mens vilkår har ${hovedregler.size}"
        }
    }

    /**
     * Valider att begrunnelse i svaret savnes hvis [RegelNode.begrunnelseType]=[BegrunnelseType.UTEN]
     */
    private fun validerSavnerBegrunnelseHvisUtenBegrunnelse(vilkårType: VilkårType,
                                                            svarMapping: RegelNode,
                                                            vurdering: VurderingDto) {
        if (svarMapping.begrunnelseType == BegrunnelseType.UTEN && vurdering.begrunnelse != null && vurdering.begrunnelse.isNotEmpty()) {
            throw Feil("Begrunnelse for vilkårType=$vilkårType regelId=${vurdering.regelId} svarId=${vurdering.svar} skal ikke ha begrunnelse")
        }
    }

    /**
     * Validerer att begrunnelse er ifylt hvis [RegelNode.begrunnelseType]=[BegrunnelseType.PÅKREVD]
     */
    private fun manglerPåkrevdBegrunnelse(regelNode: RegelNode, vurdering: VurderingDto): Boolean =
            regelNode.begrunnelseType == BegrunnelseType.PÅKREVD && vurdering.begrunnelse?.trim().isNullOrEmpty()

    /**
     * @return regelId for første svaret som er hovedregeln på delvilkåret
     */
    private fun DelvilkårsvurderingDto.hovedregel() = this.vurderinger.first().regelId

    private inline fun feilHvis(boolean: Boolean, lazyMessage: () -> String) {
        if (boolean) {
            throw Feil(lazyMessage())
        }
    }
}