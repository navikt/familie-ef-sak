package no.nav.familie.ef.sak.regler

abstract class Vilkårsregel(val vilkårType: VilkårType,
                            val regler: Set<RegelSteg>,
                            val root: Set<RegelId>)

enum class VilkårType {
    MEDLEMSKAP,
    LOVLIG_OPPHOLD,
    MOR_ELLER_FAR
}