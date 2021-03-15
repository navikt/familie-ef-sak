package no.nav.familie.ef.sak.regler

abstract class Vilkårsregel(val vilkårType: VilkårType,
                            val regler: Map<RegelId, RegelSteg>,
                            val rotregler: Set<RegelId>) {
    constructor(vilkårType: VilkårType, regler: Set<RegelSteg>, rotregler: Set<RegelId>):
            this(vilkårType, regler.associateBy {it.regelId}, rotregler)
}

enum class VilkårType {
    MEDLEMSKAP,
    LOVLIG_OPPHOLD,
    MOR_ELLER_FAR,
    SIVILSTAND,
    SAMLIV,
    ALENEOMSORG,
    NYTT_BARN_SAMME_PARTNER
}