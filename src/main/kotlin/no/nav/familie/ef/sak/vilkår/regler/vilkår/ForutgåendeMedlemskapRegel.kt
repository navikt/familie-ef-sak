package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.NesteRegel
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder
import java.util.UUID

class ForutgåendeMedlemskapRegel : Vilkårsregel(
    vilkårType = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
    regler = setOf(SØKER_MEDLEM_I_FOLKETRYGDEN, MEDLEMSKAP_UNNTAK),
    hovedregler = regelIder(SØKER_MEDLEM_I_FOLKETRYGDEN),
) {
    override fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: UUID?,
    ): List<Delvilkårsvurdering> {
        val personstatus = metadata.vilkårgrunnlagDto.medlemskap.registergrunnlag.folkeregisterpersonstatus
        val statsborgerskap =
            metadata.vilkårgrunnlagDto.medlemskap.registergrunnlag.statsborgerskap.firstOrNull { it.land.lowercase() == "norge" }
        val harNorskStatsborgerskap = statsborgerskap != null
        val fødeland = metadata.vilkårgrunnlagDto.personalia.fødeland

        val harInnflyttet = metadata.vilkårgrunnlagDto.medlemskap.registergrunnlag.innflytting.isNotEmpty()
        val harUtflyttet = metadata.vilkårgrunnlagDto.medlemskap.registergrunnlag.utflytting.isNotEmpty()

        if (fødeland == "NOR" && harNorskStatsborgerskap && personstatus == Folkeregisterpersonstatus.BOSATT && !harInnflyttet && !harUtflyttet) {
            return listOf(automatiskVurdertDelvilkår(RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN, SvarId.JA, "Placeholder"))
        }

        return super.initiereDelvilkårsvurdering(metadata, resultat, barnId)
    }

    companion object {
        private val unntakSvarMapping =
            setOf(
                SvarId.MEDLEM_MER_ENN_5_ÅR_AVBRUDD_MINDRE_ENN_10_ÅR,
                SvarId.MEDLEM_MER_ENN_7_ÅR_AVBRUDD_MER_ENN_10ÅR,
                SvarId.I_LANDET_FOR_GJENFORENING_ELLER_GIFTE_SEG,
                SvarId.ANDRE_FORELDER_MEDLEM_SISTE_5_ÅR,
                SvarId.ANDRE_FORELDER_MEDLEM_MINST_5_ÅR_AVBRUDD_MINDRE_ENN_10_ÅR,
                SvarId.ANDRE_FORELDER_MEDLEM_MINST_7_ÅR_AVBRUDD_MER_ENN_10_ÅR,
                SvarId.TOTALVURDERING_OPPFYLLER_FORSKRIFT,
                SvarId.MEDLEM_MER_ENN_5_ÅR_EØS,
                SvarId.MEDLEM_MER_ENN_5_ÅR_EØS_ANNEN_FORELDER_TRYGDEDEKKET_I_NORGE,
            ).associateWith { SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE } +
                mapOf(SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)
        private val MEDLEMSKAP_UNNTAK =
            RegelSteg(
                regelId = RegelId.MEDLEMSKAP_UNNTAK,
                svarMapping = unntakSvarMapping,
            )

        private val SØKER_MEDLEM_I_FOLKETRYGDEN =
            RegelSteg(
                regelId = RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                        hvisNei = NesteRegel(MEDLEMSKAP_UNNTAK.regelId),
                    ),
            )
    }
}
