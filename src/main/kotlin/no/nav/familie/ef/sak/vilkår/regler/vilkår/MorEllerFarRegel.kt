package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.felles.util.norskFormat
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
import no.nav.familie.kontrakter.felles.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class MorEllerFarRegel : Vilkårsregel(
    vilkårType = VilkårType.MOR_ELLER_FAR,
    regler = setOf(OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN),
    hovedregler = regelIder(OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN),
) {

    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: UUID?,
    ): List<Delvilkårsvurdering> {
        if (resultat != Vilkårsresultat.IKKE_TATT_STILLING_TIL || barnId == null) {
            return super.initiereDelvilkårsvurdering(metadata, resultat, barnId)
        }

        return hovedregler.map {
            if (it == RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN && erMorEllerFarForAlleBarn(metadata)) {
                automatiskOppfyllErMorEllerFar()
            } else {
                Delvilkårsvurdering(resultat, vurderinger = listOf(Vurdering(it)))
            }
        }
    }

    fun erMorEllerFarForAlleBarn(metadata: HovedregelMetadata): Boolean {
        secureLogger.info("Beregner er mor eller far for alle barn. Metadata: " + objectMapper.writeValueAsString(metadata))
        return metadata.vilkårgrunnlagDto.barnMedSamvær.all { it.søknadsgrunnlag.fødselTermindato == null && it.registergrunnlag.fødselsnummer != null }
    }

    private fun automatiskOppfyllErMorEllerFar(): Delvilkårsvurdering {
        val beskrivelse = "Automatisk vurdert: Den ${LocalDate.now().norskFormat()} er det" +
            " automatisk vurdert at bruker søker stønad for egne/adopterte barn?."
        return Delvilkårsvurdering(
            resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
            listOf(
                Vurdering(
                    regelId = RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN,
                    svar = SvarId.JA,
                    begrunnelse = beskrivelse,
                ),
            ),
        )
    }

    companion object {

        private val OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN =
            RegelSteg(
                regelId = RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN,
                svarMapping = jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )
    }
}
