package no.nav.familie.ef.sak.regler.validering

import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VilkårDto
import no.nav.familie.ef.sak.api.dto.VilkårSvarDto
import no.nav.familie.ef.sak.regler.BegrunnelseType
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.Resultat
import no.nav.familie.ef.sak.regler.SluttRegel
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.vilkårsregel.OppholdINorge
import no.nav.familie.ef.sak.repository.domain.VilkårType

fun valider(vilkårDto: DelvilkårsvurderingDto, vilkårsregel: Vilkårsregel): Resultat {
    if (vilkårDto.svar.isEmpty()) {
        error("Forventer att svaren har sjekkets")
    }

    val svarPåRegelId = vilkårDto.svar.associateBy { it.regelId }
    vilkårsregel.regler.map { it.key }

    // Todo valider att man har sjekket alle svar som sendes inn
    val erAlleRotreglerOppfylte = vilkårsregel.rotregler.all {
        var regelId = it
        while (true) {
            val svar = svarPåRegelId[regelId] ?: error("Har ikke svaret på $regelId")
            val regel = vilkårsregel.regler[regelId] ?: error("Finner ikke $regelId under regler til ${vilkårsregel.vilkårType}")
            val svarRegel = regel.svarMapping[svar.svar]
                            ?: error("Finner ikke svarMapping for ${svar.svar} for ${vilkårsregel.vilkårType}-${regelId}")
            if (svar.begrunnelse.isNullOrEmpty() && svarRegel.begrunnelseType !== BegrunnelseType.UTEN) {
                error("Forventer att man skal ha med begrunnelse på ${svar.regelId}")
            } else if (svar.begrunnelse?.isNotEmpty() == true && svarRegel.begrunnelseType == BegrunnelseType.UTEN) {
                error("Forventer att man ikke skal ha med begrunnelse på ${svar.regelId}")
            } else if (svarRegel.regelId === RegelId.SLUTT_NODE) {
                return@all (svarRegel as SluttRegel).resultat == Resultat.OPPFYLT
            } else {
                regelId = svarRegel.regelId
            }
        }
        @Suppress("UNREACHABLE_CODE") // while-loop feiler hvis man ikke har med return val i sluttet
        false
    }
    return if (erAlleRotreglerOppfylte) Resultat.OPPFYLT else Resultat.IKKE_OPPFYLT
}

fun main() {


}