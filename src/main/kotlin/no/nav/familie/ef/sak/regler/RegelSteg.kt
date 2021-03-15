package no.nav.familie.ef.sak.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlin.reflect.KClass


data class RegelSteg(val regelId: RegelId,
                     val svarMapping: Map<Svar, RegelNode>) {

    constructor(regelId: RegelId, svarMapping: KClass<out SvarMedSvarsalternativ>) :
            this(regelId, svarMapping.java.enumConstants.map { it to it.regelNode }.toMap())
}

interface RegelNode {

    val regelId: RegelId
    val begrunnelse: Begrunnelse
}

data class SluttRegel private constructor(@JsonIgnore // resultat trengs ikke for frontend, men for validering i backend
                                          val resultat: Resultat,
                                          override val begrunnelse: Begrunnelse = Begrunnelse.UTEN) : RegelNode {

    override val regelId: RegelId = SluttNode.SLUTT_NODE

    companion object {

        val OPPFYLT = SluttRegel(resultat = Resultat.OPPFYLT)
        val OPPFYLT_MED_PÅKREVD_BEGRUNNELSE = SluttRegel(resultat = Resultat.OPPFYLT,
                                                         begrunnelse = Begrunnelse.PÅKREVD)
        val OPPFYLT_MED_VALGFRI_BEGRUNNELSE = SluttRegel(resultat = Resultat.OPPFYLT,
                                                         begrunnelse = Begrunnelse.VALGFRI)

        val IKKE_OPPFYLT = SluttRegel(resultat = Resultat.IKKE_OPPFYLT)
        val IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE = SluttRegel(resultat = Resultat.IKKE_OPPFYLT,
                                                              begrunnelse = Begrunnelse.PÅKREVD)
        val IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE = SluttRegel(resultat = Resultat.IKKE_OPPFYLT,
                                                              begrunnelse = Begrunnelse.VALGFRI)
    }

}

data class NesteRegel(override val regelId: RegelId,
                      override val begrunnelse: Begrunnelse = Begrunnelse.UTEN) : RegelNode