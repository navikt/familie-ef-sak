package no.nav.familie.ef.sak.regler

import no.nav.familie.kontrakter.felles.objectMapper

data class Specifications(val resultatregler: Map<RegelId, Resultat>,
                          val vilkårsregler: Map<VilkårType, Vilkårsregel>)

val vilkårsregler: Map<VilkårType, Vilkårsregel> =
        listOf(ForutgåendeMedlemskap(),
               OppholdINorge(),
               MorEllerFar(),
               Sivilstand(),
               Samliv(),
               Aleneomsorg())
                .map { it.vilkårType to it }.toMap()
val specifications = Specifications(ResultatRegel.values().map { it to it.resultat }.toMap(), vilkårsregler)

fun main() {
    println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(specifications))
}

/**
Borde man ha en referense til parent når man "bruker en specification" så att man får ett tre?

Eller borde det vara tillræckligt om frontend må sende inn en parent og att man kan ha flere av eks OPPFYLT?

Hur ska man kunna ha 2 flyt før manuell och maskinell?

Hvordan håndtere det når man har annet enn Ja / Nei ?

Aleneomsorg skal være per barn?

Sivilstand - regler skal opprettes hvis det trengs
 */