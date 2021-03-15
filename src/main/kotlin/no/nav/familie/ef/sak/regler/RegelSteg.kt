package no.nav.familie.ef.sak.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlin.reflect.KClass

interface RegelNod {

    val regelId: RegelId
    val begrunnelse: Begrunnelse
}

data class SluttRegel private constructor(@JsonIgnore
                                          val resultat: Resultat,
                                          override val begrunnelse: Begrunnelse = Begrunnelse.UTEN) : RegelNod {

    override val regelId: RegelId = SluttNod.SLUTTNOD

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
                      override val begrunnelse: Begrunnelse = Begrunnelse.UTEN) : RegelNod


data class RegelSteg(val regelId: RegelId,
                     val svarMapping: Map<Svar, RegelNod>) {

    constructor(regelId: RegelId, svarMapping: KClass<out SvarMedSvarsalternativ>) :
            this(regelId, svarMapping.java.enumConstants.map { it to it.regelNod }.toMap())
}