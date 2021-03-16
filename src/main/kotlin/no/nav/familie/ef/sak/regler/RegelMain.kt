package no.nav.familie.ef.sak.regler

import no.nav.familie.ef.sak.regler.vilkårsregel.Aleneomsorg
import no.nav.familie.ef.sak.regler.vilkårsregel.ForutgåendeMedlemskap
import no.nav.familie.ef.sak.regler.vilkårsregel.MorEllerFar
import no.nav.familie.ef.sak.regler.vilkårsregel.NyttBarnSammePartner
import no.nav.familie.ef.sak.regler.vilkårsregel.OppholdINorge
import no.nav.familie.ef.sak.regler.vilkårsregel.Samliv
import no.nav.familie.ef.sak.regler.vilkårsregel.Sivilstand
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.kontrakter.felles.objectMapper

data class Specification(val vilkårsregler: Map<VilkårType, Vilkårsregel>)

val vilkårsregler: Map<VilkårType, Vilkårsregel> =
        listOf(
                ForutgåendeMedlemskap(),
                OppholdINorge(),
                MorEllerFar(),
                Sivilstand(),
                Samliv(),
                Aleneomsorg(),
                NyttBarnSammePartner()
        )
                .map { it.vilkårType to it }.toMap()
val specifications = Specification(vilkårsregler)

fun main() {
    val objectMapper = objectMapper
            //.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .writerWithDefaultPrettyPrinter()
    println(objectMapper.writeValueAsString(specifications))
}

/**
Borde man ha en referense til parent når man "bruker en specification" så att man får ett tre?

Eller borde det vara tillræckligt om frontend må sende inn en parent og att man kan ha flere av eks OPPFYLT?

Hur ska man kunna ha 2 flyt før manuell och maskinell?

Hvordan håndtere det når man har annet enn Ja / Nei ?

Aleneomsorg skal være per barn?

Sivilstand - regler skal opprettes hvis det trengs

Hvordan håndtere att delvilkår er obligatoriske/ikke obligatoriske?
Eks aleneomsorg dær man har flera delvilkår dær de inte ær obligatoriske
 */