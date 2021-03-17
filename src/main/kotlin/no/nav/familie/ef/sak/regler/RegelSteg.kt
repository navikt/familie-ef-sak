package no.nav.familie.ef.sak.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.api.Feil


data class RegelSteg(val regelId: RegelId,
                     val svarMapping: Map<SvarId, RegelNode>) {

    fun svarMapping(svarId: SvarId): RegelNode {
        return svarMapping[svarId] ?: throw Feil("Finner ikke svarId=$svarId for regelId=${regelId}")
    }
}

fun jaNeiMapping(hvisJa: RegelNode = SluttRegel.OPPFYLT,
                 hvisNei: RegelNode = SluttRegel.IKKE_OPPFYLT): Map<SvarId, RegelNode> =
        mapOf(SvarId.JA to hvisJa,
              SvarId.NEI to hvisNei)

interface RegelNode {

    val regelId: RegelId
    val begrunnelseType: BegrunnelseType
}

data class SluttRegel private constructor(@JsonIgnore // resultat trengs ikke for frontend, men for validering i backend
                                          val resultat: Resultat,
                                          override val begrunnelseType: BegrunnelseType = BegrunnelseType.UTEN) : RegelNode {

    override val regelId: RegelId = RegelId.SLUTT_NODE

    companion object {

        val OPPFYLT = SluttRegel(resultat = Resultat.OPPFYLT)
        val OPPFYLT_MED_PÅKREVD_BEGRUNNELSE = SluttRegel(resultat = Resultat.OPPFYLT,
                                                         begrunnelseType = BegrunnelseType.PÅKREVD)
        val OPPFYLT_MED_VALGFRI_BEGRUNNELSE = SluttRegel(resultat = Resultat.OPPFYLT,
                                                         begrunnelseType = BegrunnelseType.VALGFRI)

        val IKKE_OPPFYLT = SluttRegel(resultat = Resultat.IKKE_OPPFYLT)
        val IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE = SluttRegel(resultat = Resultat.IKKE_OPPFYLT,
                                                              begrunnelseType = BegrunnelseType.PÅKREVD)
        val IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE = SluttRegel(resultat = Resultat.IKKE_OPPFYLT,
                                                              begrunnelseType = BegrunnelseType.VALGFRI)
    }

}

data class NesteRegel(override val regelId: RegelId,
                      override val begrunnelseType: BegrunnelseType = BegrunnelseType.UTEN) : RegelNode