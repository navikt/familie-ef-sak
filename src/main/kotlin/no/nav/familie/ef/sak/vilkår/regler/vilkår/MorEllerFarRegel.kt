package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import java.util.UUID

class MorEllerFarRegel :
    Vilkårsregel(
        vilkårType = VilkårType.MOR_ELLER_FAR,
        regler = setOf(OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN),
        hovedregler = regelIder(OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN),
    ) {
    override fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: UUID?,
    ): List<Delvilkårsvurdering> {
        if (resultat != Vilkårsresultat.IKKE_TATT_STILLING_TIL) {
            return super.initiereDelvilkårsvurdering(metadata, resultat, barnId)
        }

        return gjeldendeHovedregler().map {
            if (it == RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN && erMorEllerFarForAlleBarn(metadata)) {
                automatiskVurdertDelvilkår(RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN, SvarId.JA, "Bruker søker stønad for egne/adopterte barn.")
            } else {
                Delvilkårsvurdering(resultat, vurderinger = listOf(Vurdering(it)))
            }
        }
    }

    fun erMorEllerFarForAlleBarn(metadata: HovedregelMetadata): Boolean =
        metadata.behandling.årsak == BehandlingÅrsak.SØKNAD &&
            metadata.vilkårgrunnlagDto.barnMedSamvær.all { it.registergrunnlag.fødselsnummer != null }

    companion object {
        private val OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN =
            RegelSteg(
                regelId = RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
    }
}
