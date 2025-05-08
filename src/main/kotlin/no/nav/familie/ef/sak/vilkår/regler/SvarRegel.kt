package no.nav.familie.ef.sak.vilkår.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat

enum class BegrunnelseType {
    UTEN,
    PÅKREVD,
    VALGFRI,
}

/**
 * Brukes for å mappe ett svar til ett vilkårsresultat,
 * Vi er kun interessert i å mappe [Vilkårsresultat.OPPFYLT] og [Vilkårsresultat.IKKE_OPPFYLT] og ikke de andre verdiene
 */
enum class Resultat(
    val vilkårsresultat: Vilkårsresultat,
) {
    OPPFYLT(Vilkårsresultat.OPPFYLT),
    IKKE_OPPFYLT(Vilkårsresultat.IKKE_OPPFYLT),
}

/**
 * Regel for svaret
 */
interface SvarRegel {
    val regelId: RegelId
    val begrunnelseType: BegrunnelseType
}

/**
 * @param resultat trengs ikke for frontend, men for validering i backend
 * [regelId] er [RegelId.SLUTT_NODE] som betyr att den ikke har noen flere spørsmål for svaret
 */
class SluttSvarRegel private constructor(
    @JsonIgnore
    val resultat: Resultat,
    override val begrunnelseType: BegrunnelseType = BegrunnelseType.UTEN,
) : SvarRegel {
    override val regelId: RegelId = RegelId.SLUTT_NODE

    companion object {
        val OPPFYLT = SluttSvarRegel(resultat = Resultat.OPPFYLT)
        val OPPFYLT_MED_PÅKREVD_BEGRUNNELSE =
            SluttSvarRegel(
                resultat = Resultat.OPPFYLT,
                begrunnelseType = BegrunnelseType.PÅKREVD,
            )
        val OPPFYLT_MED_VALGFRI_BEGRUNNELSE =
            SluttSvarRegel(
                resultat = Resultat.OPPFYLT,
                begrunnelseType = BegrunnelseType.VALGFRI,
            )

        val IKKE_OPPFYLT = SluttSvarRegel(resultat = Resultat.IKKE_OPPFYLT)
        val IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE =
            SluttSvarRegel(
                resultat = Resultat.IKKE_OPPFYLT,
                begrunnelseType = BegrunnelseType.PÅKREVD,
            )
        val IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE =
            SluttSvarRegel(
                resultat = Resultat.IKKE_OPPFYLT,
                begrunnelseType = BegrunnelseType.VALGFRI,
            )
    }
}

/**
 * Peker videre til neste regel, som er ett spørsmål
 */
data class NesteRegel(
    override val regelId: RegelId,
    override val begrunnelseType: BegrunnelseType = BegrunnelseType.UTEN,
) : SvarRegel
