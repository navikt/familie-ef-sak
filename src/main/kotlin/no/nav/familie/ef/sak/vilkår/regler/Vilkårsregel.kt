package no.nav.familie.ef.sak.vilkår.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstand
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.dto.LangAvstandTilSøker
import java.util.UUID

/**
 * Brukes for å utlede hvilke delvilkår som må besvares
 */
data class HovedregelMetadata(
    val sivilstandSøknad: Sivilstand?,
    val sivilstandstype: Sivilstandstype,
    val erMigrering: Boolean = false,
    val barn: List<BehandlingBarn>,
    val søktOmBarnetilsyn: List<UUID>,
    val langAvstandTilSøker: List<BarnForelderLangAvstandTilSøker> = listOf(),
    val harBrukerEllerAnnenForelderTidligereVedtak: Boolean = false
)

data class BarnForelderLangAvstandTilSøker(
    val barnId: UUID,
    val langAvstandTilSøker: LangAvstandTilSøker
)

abstract class Vilkårsregel(
    val vilkårType: VilkårType,
    val regler: Map<RegelId, RegelSteg>,
    @JsonIgnore
    val hovedregler: Set<RegelId>
) {

    open fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        barnId: UUID? = null
    ): List<Delvilkårsvurdering> {
        return hovedregler.map {
            Delvilkårsvurdering(
                resultat,
                vurderinger = listOf(Vurdering(it))
            )
        }
    }

    constructor(vilkårType: VilkårType, regler: Set<RegelSteg>, hovedregler: Set<RegelId>) :
        this(vilkårType, regler.associateBy { it.regelId }, hovedregler)

    fun regel(regelId: RegelId): RegelSteg {
        return regler[regelId] ?: throw Feil("Finner ikke regelId=$regelId for vilkårType=$vilkårType")
    }

    protected fun ubesvartDelvilkårsvurdering(regelId: RegelId) = Delvilkårsvurdering(
        resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        vurderinger = listOf(
            Vurdering(
                regelId = regelId
            )
        )
    )
}
