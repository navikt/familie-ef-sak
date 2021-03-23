package no.nav.familie.ef.sak.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad

/**
 * Brukes for å utlede hvilke delvilkår som må besvares
 */
data class HovedregelMetadata(val søknad: SøknadsskjemaOvergangsstønad,
                              val sivilstandstype: Sivilstandstype)

abstract class Vilkårsregel(val vilkårType: VilkårType,
                            val regler: Map<RegelId, RegelSteg>,
                            @JsonIgnore
                            val hovedregler: Set<RegelId>) {

    @Suppress("UNUSED_PARAMETER") open // kan overrideas
    fun hovedregler(metadata: HovedregelMetadata): Set<RegelId> = hovedregler

    constructor(vilkårType: VilkårType, regler: Set<RegelSteg>, hovedregler: Set<RegelId>) :
            this(vilkårType, regler.associateBy { it.regelId }, hovedregler)

    fun regel(regelId: RegelId): RegelSteg {
        return regler[regelId] ?: throw Feil("Finner ikke regelId=$regelId for vilkårType=$vilkårType")
    }
}