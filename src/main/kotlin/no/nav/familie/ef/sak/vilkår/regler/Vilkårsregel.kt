package no.nav.familie.ef.sak.vilkår.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vurdering

/**
 * Brukes for å utlede hvilke delvilkår som må besvares
 */
data class HovedregelMetadata(val søknad: SøknadsskjemaOvergangsstønad,
                              val sivilstandstype: Sivilstandstype)

abstract class Vilkårsregel(val vilkårType: VilkårType,
                            val regler: Map<RegelId, RegelSteg>,
                            @JsonIgnore
                            val hovedregler: Set<RegelId>) {

    open fun initereDelvilkårsvurdering(metadata: HovedregelMetadata,
                                        resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL): List<Delvilkårsvurdering> {
        return hovedregler.map {
            Delvilkårsvurdering(resultat,
                                vurderinger = listOf(Vurdering(it)))
        }
    }

    constructor(vilkårType: VilkårType, regler: Set<RegelSteg>, hovedregler: Set<RegelId>) :
            this(vilkårType, regler.associateBy { it.regelId }, hovedregler)

    fun regel(regelId: RegelId): RegelSteg {
        return regler[regelId] ?: throw Feil("Finner ikke regelId=$regelId for vilkårType=$vilkårType")
    }

}