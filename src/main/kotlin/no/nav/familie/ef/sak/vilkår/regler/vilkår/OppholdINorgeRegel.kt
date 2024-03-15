package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus.BOSATT
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
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import java.util.UUID

class OppholdINorgeRegel : Vilkårsregel(
    vilkårType = VilkårType.LOVLIG_OPPHOLD,
    regler = setOf(BOR_OG_OPPHOLDER_SEG_I_NORGE, OPPHOLD_UNNTAK),
    hovedregler = regelIder(BOR_OG_OPPHOLDER_SEG_I_NORGE),
) {
    override fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: UUID?,
    ): List<Delvilkårsvurdering> {
        if (resultat != Vilkårsresultat.IKKE_TATT_STILLING_TIL) {
            return super.initiereDelvilkårsvurdering(metadata, resultat, barnId)
        }

        if (oppfyllerVilkårForAutomatiskVurdering(metadata)) {
            return listOf(
                automatiskVurdertDelvilkår(
                    regelId = RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                    svarId = SvarId.JA,
                    begrunnelse = "Bruker og barn bor og oppholder seg i Norge og bruker er norsk statsborger",
                ),
            )
        }

        return super.initiereDelvilkårsvurdering(metadata, resultat, barnId)
    }

    fun harBarnaPersonstatusBosatt(metadata: HovedregelMetadata): Boolean {
        return metadata.vilkårgrunnlagDto.barnMedSamvær.all {
            it.registergrunnlag.folkeregisterpersonstatus == BOSATT || it.søknadsgrunnlag.fødselTermindato != null
        }
    }

    fun oppfyllerVilkårForAutomatiskVurdering(metadata: HovedregelMetadata): Boolean {
        val erDigitalSøknad = metadata.behandling.årsak == BehandlingÅrsak.SØKNAD
        val medlemskap = metadata.vilkårgrunnlagDto.medlemskap
        val harBrukerSvartOppholderSegINorgeISøknad = medlemskap.søknadsgrunnlag?.oppholderDuDegINorge == true && medlemskap.søknadsgrunnlag.bosattNorgeSisteÅrene
        val harSøkerPersonstatusBosatt = medlemskap.registergrunnlag.folkeregisterpersonstatus == BOSATT
        val harSøkerNorskStatsborger = medlemskap.registergrunnlag.statsborgerskap.any {
            it.land.lowercase() == STATSBORGERSTAT_VERDI_NORGE
        }
        val harBarnaPersonstatusBosatt = harBarnaPersonstatusBosatt(metadata)

        return erDigitalSøknad &&
            harSøkerNorskStatsborger &&
            harBrukerSvartOppholderSegINorgeISøknad &&
            harSøkerPersonstatusBosatt &&
            harBarnaPersonstatusBosatt
    }

    companion object {
        private val OPPHOLD_UNNTAK =
            RegelSteg(
                regelId = RegelId.OPPHOLD_UNNTAK,
                svarMapping =
                mapOf(
                    SvarId.ARBEID_NORSK_ARBEIDSGIVER to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    SvarId.UTENLANDSOPPHOLD_MINDRE_ENN_6_UKER to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    SvarId.OPPHOLDER_SEG_I_ANNET_EØS_LAND to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )

        private val BOR_OG_OPPHOLDER_SEG_I_NORGE =
            RegelSteg(
                regelId = RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                svarMapping =
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = NesteRegel(OPPHOLD_UNNTAK.regelId),
                ),
            )
    }
}
