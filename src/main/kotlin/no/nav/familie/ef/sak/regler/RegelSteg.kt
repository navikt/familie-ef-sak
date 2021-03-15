package no.nav.familie.ef.sak.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.reflect.KClass

class RegelSteg(val regelId: RegelId,
                val hvisJa: RegelId = ResultatRegel.OPPFYLT,
                val hvisNei: RegelId = ResultatRegel.IKKE_OPPFYLT,
                val hvisJaBegrunnelse: Begrunnelse? = null,
                val hvisNeiBegrunnelse: Begrunnelse? = null,
                @JsonIgnore
                val årsaker: KClass<out Årsak>? = null) : RegelFlyt {

    @JsonProperty("årsaker")
    val årsakMap = årsaker?.let { årsak -> årsak.java.enumConstants.map { it to it.mapping }.toMap() }

    //override val type = RegelType.REGEL_STEG
}