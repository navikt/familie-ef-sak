package no.nav.familie.ef.sak.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.domain.VilkårType

abstract class Vilkårsregel(val vilkårType: VilkårType,
                            val regler: Map<RegelId, RegelSteg>,
                            @JsonIgnore
                            val rotregler: Set<RegelId>) {

    constructor(vilkårType: VilkårType, regler: Set<RegelSteg>, rotregler: Set<RegelId>) :
            this(vilkårType, regler.associateBy { it.regelId }, rotregler)

    fun regel(regelId: RegelId): RegelSteg {
        return regler[regelId] ?: throw Feil("Finner ikke regelId=${regelId} for vilkårType=$vilkårType")
    }
}