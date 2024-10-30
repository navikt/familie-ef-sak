package no.nav.familie.ef.sak.vilkår.regler.evalutation

import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.VurderingDto
import no.nav.familie.ef.sak.vilkår.regler.BegrunnelseType
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelVersjon
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.SvarRegel
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel

object RegelValidering {
    fun validerVurdering(
        vilkårsregel: Vilkårsregel,
        oppdatering: List<DelvilkårsvurderingDto>,
        tidligereDelvilkårsvurderinger: List<Delvilkårsvurdering>,
    ) {
        validerAlleDelvilkårErBesvartUtFraRegelverksversjon(vilkårsregel, oppdatering)
        validerAlleHovedreglerFinnesMed(vilkårsregel, oppdatering, tidligereDelvilkårsvurderinger)

        oppdatering.forEach { delvilkårsvurderingDto ->
            validerDelvilkår(vilkårsregel, delvilkårsvurderingDto)
        }
    }

    /**
     * Validerer att begrunnelse er ifylt hvis [SvarRegel.begrunnelseType]=[BegrunnelseType.PÅKREVD]
     */
    fun manglerPåkrevdBegrunnelse(
        svarRegel: SvarRegel,
        vurdering: VurderingDto,
    ): Boolean = svarRegel.begrunnelseType == BegrunnelseType.PÅKREVD && vurdering.begrunnelse?.trim().isNullOrEmpty()

    /**
     * Kaster feil hvis
     *
     * * ett [Vurdering] savner svar OG ikke er det siste svaret, eks svaren [ja, null, nei]
     *
     * * har begrunnelse men er [BegrunnelseType.UTEN]
     *
     * * ett svar er av typen [SluttSvarRegel] men att det finnes flere svar, eks [ja, nei, ja],
     *   hvor det andre svaret egentlige er type [SluttSvarRegel]
     *
     */
    private fun validerDelvilkår(
        vilkårsregel: Vilkårsregel,
        delvilkårsvurderingDto: DelvilkårsvurderingDto,
    ) {
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
                feilHvis(svarMapping is SluttSvarRegel && erIkkeSisteSvaret) {
                    "Finnes ikke noen flere regler, men finnes flere svar vilkårType=$vilkårType svarId=$svarId:"
                }
            }
        }
    }

    /**
     * Skal validere att man sender inn minimum ett svar for ett delvilkår
     * Når backend initierar [Delvilkårsvurdering] så legges ett første svar in med regelId(hovedregel) for hvert delvilkår
     */
    private fun validerAlleDelvilkårErBesvartUtFraRegelverksversjon(
        vilkårsregel: Vilkårsregel,
        oppdatering: List<DelvilkårsvurderingDto>,
    ) {
        oppdatering.forEach { vurdering ->
            val regelSteg = vilkårsregel.regler[vurdering.hovedregel()] ?: error("Fant ikke regelSteg for ${vurdering.hovedregel()}")
            feilHvis(vurdering.vurderinger.isEmpty() && regelSteg.versjon == RegelVersjon.GJELDENDE) { "Mangler svar for et delvilkår for vilkår=${vilkårsregel.vilkårType}" }
            feilHvis(vurdering.vurderinger.isNotEmpty() && regelSteg.versjon == RegelVersjon.HISTORISK) { "Kan ikke oppdatere et historisk delvilkår. Vilkår=${vilkårsregel.vilkårType}" }
        }
    }

    private fun validerAlleHovedreglerFinnesMed(
        vilkårsregel: Vilkårsregel,
        delvilkår: List<DelvilkårsvurderingDto>,
        tidligereDelvilkårsvurderinger: List<Delvilkårsvurdering>,
    ) {
        val aktuelleDelvilkår = aktuelleDelvilkår(tidligereDelvilkårsvurderinger)
        val delvilkårRegelIdn = delvilkår.map { it.hovedregel() }
        val aktuelleHvovedregler = vilkårsregel.hovedregler.filter { aktuelleDelvilkår.contains(it) }
        feilHvis(!aktuelleHvovedregler.containsAll(delvilkårRegelIdn)) {
            "Delvilkårsvurderinger savner svar på hovedregler - hovedregler=$aktuelleHvovedregler delvilkår=$delvilkårRegelIdn"
        }
        feilHvis(delvilkårRegelIdn.size != aktuelleHvovedregler.size) {
            "Feil i antall regler dto har ${delvilkårRegelIdn.size} " +
                "mens vilkår har ${aktuelleHvovedregler.size} aktuelle delvilkår"
        }
    }

    private fun aktuelleDelvilkår(tidligereDelvilkårsvurderinger: List<Delvilkårsvurdering>): Set<RegelId> =
        tidligereDelvilkårsvurderinger
            .filter { it.resultat != Vilkårsresultat.IKKE_AKTUELL }
            .map { it.hovedregel }
            .toSet()

    /**
     * Valider att begrunnelse i svaret savnes hvis [SvarRegel.begrunnelseType]=[BegrunnelseType.UTEN]
     */
    private fun validerSavnerBegrunnelseHvisUtenBegrunnelse(
        vilkårType: VilkårType,
        svarMapping: SvarRegel,
        vurdering: VurderingDto,
    ) {
        if (svarMapping.begrunnelseType == BegrunnelseType.UTEN && !vurdering.begrunnelse.isNullOrEmpty()) {
            throw Feil(
                "Begrunnelse for vilkårType=$vilkårType regelId=${vurdering.regelId} " +
                    "svarId=${vurdering.svar} skal ikke ha begrunnelse",
            )
        }
    }
}
