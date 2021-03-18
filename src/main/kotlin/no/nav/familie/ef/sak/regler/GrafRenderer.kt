package no.nav.familie.ef.sak.regler

import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.kontrakter.felles.objectMapper

/**
 * Brukes kun til å rendere grafdata for d3
 */
data class Fråga(val name: RegelId,
                 val children: List<Svar>) {
    val type = "fråga"
}

data class Svar(val name: SvarId,
                val begrunnelseType: BegrunnelseType,
                val children: List<Fråga>,
                val resultat: Vilkårsresultat? = null) {

    val type = "svar"
}


fun mapSvar(regler: Map<RegelId, RegelSteg>, svarMapping: Map<SvarId, RegelNode>): List<Svar> {
    return svarMapping.map {
        try {
            val value = it.value
            if (value is SluttRegel) {
                Svar(it.key, value.begrunnelseType, emptyList(), value.resultat.vilkårsresultat)
            } else {
                Svar(it.key, value.begrunnelseType, listOf(mapFråga(regler, value.regelId)))
            }
        } catch (e: Exception) {
            throw e
        }
    }
}

fun mapFråga(regler: Map<RegelId, RegelSteg>, regelId: RegelId): Fråga {
    val svarMapping = regler[regelId]!!.svarMapping
    return Fråga(regelId, mapSvar(regler, svarMapping))
}

fun main() {
    val objectMapper = objectMapper
            .writerWithDefaultPrettyPrinter()
    //println(objectMapper.writeValueAsString(Vilkårsregler.VILKÅRSREGLER))
    val map1 = Vilkårsregler.VILKÅRSREGLER.vilkårsregler.map {
        val regler = it.value.regler
        mapOf("name" to it.key,
              "children" to it.value.hovedregler.map { mapFråga(regler, it) })
    }
    println(objectMapper.writeValueAsString(mapOf("name" to "vilkår",
                                                  "children" to map1.toList())
    ))
}