package no.nav.familie.ef.sak.regler

import no.nav.familie.ef.sak.repository.domain.VilkårType

abstract class Vilkårsregel(val vilkårType: VilkårType,
                            val regler: Map<RegelId, RegelSteg>,
                            val rotregler: Set<RegelId>) {
    constructor(vilkårType: VilkårType, regler: Set<RegelSteg>, rotregler: Set<RegelId>):
            this(vilkårType, regler.associateBy {it.regelId}, rotregler)
}