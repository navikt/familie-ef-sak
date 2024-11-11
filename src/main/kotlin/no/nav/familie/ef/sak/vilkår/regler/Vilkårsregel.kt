package no.nav.familie.ef.sak.vilkår.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstand
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.dto.LangAvstandTilSøker
import no.nav.familie.ef.sak.vilkår.dto.VilkårGrunnlagDto
import java.time.LocalDate
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
    val vilkårgrunnlagDto: VilkårGrunnlagDto,
    val behandling: Behandling,
)

data class BarnForelderLangAvstandTilSøker(
    val barnId: UUID,
    val langAvstandTilSøker: LangAvstandTilSøker,
    val borAnnenForelderISammeHus: String?,
)

abstract class Vilkårsregel(
    val vilkårType: VilkårType,
    val regler: Map<RegelId, RegelSteg>,
    @JsonIgnore
    val hovedregler: Set<RegelId>,
) {
    open fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        barnId: UUID? = null,
    ): List<Delvilkårsvurdering> =
        gjeldendeHovedregler().map {
            Delvilkårsvurdering(
                resultat,
                vurderinger = listOf(Vurdering(it)),
            )
        }

    fun gjeldendeHovedregler() = hovedregler.filter { it.regelVersjon == RegelVersjon.GJELDENDE }

    constructor(vilkårType: VilkårType, regler: Set<RegelSteg>, hovedregler: Set<RegelId>) :
        this(vilkårType, regler.associateBy { it.regelId }, hovedregler)

    fun regel(regelId: RegelId): RegelSteg = regler[regelId] ?: throw Feil("Finner ikke regelId=$regelId for vilkårType=$vilkårType")

    protected fun automatiskVurdertDelvilkår(
        regelId: RegelId,
        svarId: SvarId,
        begrunnelse: String,
    ): Delvilkårsvurdering =
        Delvilkårsvurdering(
            resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
            listOf(
                Vurdering(
                    regelId = regelId,
                    svar = svarId,
                    begrunnelse = "Automatisk vurdert (${LocalDate.now().norskFormat()}): $begrunnelse",
                ),
            ),
        )

    protected fun ubesvartDelvilkårsvurdering(regelId: RegelId) =
        Delvilkårsvurdering(
            resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            vurderinger =
                listOf(
                    Vurdering(
                        regelId = regelId,
                    ),
                ),
        )
}
