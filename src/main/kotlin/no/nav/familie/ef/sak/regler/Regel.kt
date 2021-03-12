package no.nav.familie.ef.sak.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.kontrakter.felles.objectMapper
import kotlin.reflect.KClass

interface RegelId {

    val id: String
}

interface RegelIdMedBeskrivelse : RegelId {

    val beskrivelse: String
}

enum class RegelType {
    REGEL_STEG,
    RESULTAT
}

enum class Begrunnelse {
    //UTEN,
    PÅKREVD,
    VALGFRI
}

enum class Resultat {
    OPPFYLT,
    IKKE_OPPFYLT
}

interface RegelFlyt {

    //val type: RegelType
}

interface Årsak {

    val resultat: Resultat
}

class RegelSteg(val regelId: RegelId,
                val hvisJa: RegelId = ResultatRegel.OPPFYLT,
                val hvisNei: RegelId = ResultatRegel.IKKE_OPPFYLT,
                val hvisJaBegrunnelse: Begrunnelse? = null,
                val hvisNeiBegrunnelse: Begrunnelse? = null,
                @JsonIgnore
                val årsaker: KClass<out Årsak>? = null) : RegelFlyt {

    @JsonProperty("årsaker")
    val årsakMap = årsaker?.let { årsak -> årsak.java.enumConstants.map { it to it.resultat }.toMap() }

    //override val type = RegelType.REGEL_STEG
}