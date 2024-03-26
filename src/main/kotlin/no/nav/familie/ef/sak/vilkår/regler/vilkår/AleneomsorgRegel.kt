package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.dto.LangAvstandTilSøker
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import java.time.LocalDate
import java.util.UUID

class AleneomsorgRegel(
    hovedregler: Set<RegelId>? =
        null,
) : Vilkårsregel(
    vilkårType = VilkårType.ALENEOMSORG,
    regler =
    setOf(
        SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
        NÆRE_BOFORHOLD,
        MER_AV_DAGLIG_OMSORG,
    ),
    hovedregler =
    hovedregler ?: regelIder(
        SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
        NÆRE_BOFORHOLD,
        MER_AV_DAGLIG_OMSORG,
    ),
) {
    override fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: UUID?,
    ): List<Delvilkårsvurdering> {
        if (resultat != Vilkårsresultat.IKKE_TATT_STILLING_TIL) {
            return super.initiereDelvilkårsvurdering(metadata, resultat, barnId)
        }

        if (erDigitalSøknadOgDonorBarn(metadata, barnId)) {
            return listOf(automatiskVurderAleneomsorgNårAnnenForelderErDonor())
        }

        return hovedregler.map { hovedregel ->
            if (hovedregel == RegelId.NÆRE_BOFORHOLD && borLangtFraHverandre(metadata, barnId)) {
                opprettAutomatiskBeregnetNæreBoforholdDelvilkår()
            } else {
                Delvilkårsvurdering(resultat, vurderinger = listOf(Vurdering(hovedregel)))
            }
        }
    }

    private fun erDigitalSøknadOgDonorBarn(
        metadata: HovedregelMetadata,
        barnId: UUID?,
    ) = metadata.behandling.årsak == BehandlingÅrsak.SØKNAD &&
        metadata.vilkårgrunnlagDto.barnMedSamvær.find {
            it.barnId == barnId
        }?.søknadsgrunnlag?.ikkeOppgittAnnenForelderBegrunnelse?.lowercase() == "donor"

    private fun opprettAutomatiskBeregnetNæreBoforholdDelvilkår() =
        Delvilkårsvurdering(
            resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
            listOf(
                Vurdering(
                    regelId = RegelId.NÆRE_BOFORHOLD,
                    svar = SvarId.NEI,
                    begrunnelse = "Automatisk vurdert (${LocalDate.now().norskFormat()}): Det er beregnet at annen forelder bor mer enn 1 km unna bruker.",
                ),
            ),
        )

    private fun automatiskVurderAleneomsorgNårAnnenForelderErDonor(): Delvilkårsvurdering {
        val begrunnelseTekst = "Automatisk vurdert (${LocalDate.now().norskFormat()}): Bruker oppgir at annen forelder er donor."

        return Delvilkårsvurdering(
            resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
            listOf(
                Vurdering(
                    regelId = RegelId.SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                    svar = SvarId.NEI,
                    begrunnelse = begrunnelseTekst,
                ),
                Vurdering(
                    regelId = RegelId.NÆRE_BOFORHOLD,
                    svar = SvarId.NEI,
                    begrunnelse = begrunnelseTekst,
                ),
                Vurdering(
                    regelId = RegelId.MER_AV_DAGLIG_OMSORG,
                    svar = SvarId.JA,
                    begrunnelse = begrunnelseTekst,
                ),
            ),
        )
    }

    private fun borLangtFraHverandre(
        metadata: HovedregelMetadata,
        barnId: UUID?,
    ) = metadata.langAvstandTilSøker.firstOrNull { it.barnId == barnId }?.langAvstandTilSøker?.let {
        it == LangAvstandTilSøker.JA_UPRESIS || it == LangAvstandTilSøker.JA
    } ?: false

    companion object {
        private val MER_AV_DAGLIG_OMSORG =
            RegelSteg(
                regelId = RegelId.MER_AV_DAGLIG_OMSORG,
                svarMapping =
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )

        private val næreBoForholdMapping =
            setOf(
                SvarId.SAMME_HUS_OG_FÆRRE_ENN_4_BOENHETER,
                SvarId.SAMME_HUS_OG_FLERE_ENN_4_BOENHETER_MEN_VURDERT_NÆRT,
                SvarId.SELVSTENDIGE_BOLIGER_SAMME_GÅRDSTUN,
                SvarId.SELVSTENDIGE_BOLIGER_SAMME_TOMT,
                SvarId.NÆRMESTE_BOLIG_ELLER_REKKEHUS_I_SAMMEGATE,
                SvarId.TILSTØTENDE_BOLIGER_ELLER_REKKEHUS_I_SAMMEGATE,
            )
                .associateWith {
                    SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
                } + mapOf(SvarId.NEI to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)
        private val NÆRE_BOFORHOLD =
            RegelSteg(
                regelId = RegelId.NÆRE_BOFORHOLD,
                svarMapping = næreBoForholdMapping,
            )

        private val SKRIFTLIG_AVTALE_OM_DELT_BOSTED =
            RegelSteg(
                regelId = RegelId.SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                ),
            )
    }
}
