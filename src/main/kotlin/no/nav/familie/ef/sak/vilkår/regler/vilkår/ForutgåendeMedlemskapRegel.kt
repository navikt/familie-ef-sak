package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.InnflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.UtflyttingDto
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
import java.time.LocalDate
import java.util.UUID

const val STATSBORGERSTAT_VERDI_NORGE = "norge"
const val FØDELAND_NORGE = "NOR"

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
        if (resultat != Vilkårsresultat.IKKE_TATT_STILLING_TIL) {
            return super.initiereDelvilkårsvurdering(metadata, resultat, barnId)
        }

        val registergrunnlag = metadata.vilkårgrunnlagDto.medlemskap.registergrunnlag
        val erBosatt = registergrunnlag.folkeregisterpersonstatus == Folkeregisterpersonstatus.BOSATT
        val harNorskStatsborgerskap =
            registergrunnlag.statsborgerskap.any {
                it.land.lowercase() == STATSBORGERSTAT_VERDI_NORGE
            }
        val erFødtINorge = metadata.vilkårgrunnlagDto.personalia.fødeland == FØDELAND_NORGE

        val harOppholdtSegINorgeBasertPåSøknadsgrunnlag = harOppholdtSegINorgeBasertPåSøknad(metadata)

        val harBoddINorgeSiste5år = harBoddINorgeSiste5år(registergrunnlag.innflytting, registergrunnlag.utflytting)

        if (erFødtINorge && harNorskStatsborgerskap && erBosatt && harBoddINorgeSiste5år && harOppholdtSegINorgeBasertPåSøknadsgrunnlag) {
            return listOf(
                automatiskVurdertDelvilkår(
                    RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN,
                    SvarId.JA,
                    "Bruker er født i Norge, har norsk statsborgerskap, er bosatt i Norge og har ikke oppholdt seg utenfor Norge de siste 5 årene basert på opplysninger fra folkeregister og søknad.",
                ),
            )
        }

        return super.initiereDelvilkårsvurdering(metadata, resultat, barnId)
    }

    private fun harOppholdtSegINorgeBasertPåSøknad(metadata: HovedregelMetadata): Boolean {
        val søknadsgrunnlag = metadata.vilkårgrunnlagDto.medlemskap.søknadsgrunnlag
        val harOppholdtSegINorgeBasertPåSøknadsgrunnlag =
            if (søknadsgrunnlag != null) {
                søknadsgrunnlag.bosattNorgeSisteÅrene && søknadsgrunnlag.oppholderDuDegINorge
            } else {
                true
            }
        return harOppholdtSegINorgeBasertPåSøknadsgrunnlag
    }

    private fun harBoddINorgeSiste5år(
        innflytting: List<InnflyttingDto>,
        utflytting: List<UtflyttingDto>,
    ): Boolean {
        val harInnflyttet = innflytting.isNotEmpty()
        val harUtflyttet = utflytting.isNotEmpty()
        if (!harInnflyttet && !harUtflyttet) {
            return true
        }

        val harInnfyttetMenManglerDato = innflytting.any { it.dato == null }
        val harUtflyttetMenManglerDato = utflytting.any { it.dato == null }
        if (harInnfyttetMenManglerDato || harUtflyttetMenManglerDato) {
            return false
        }

        val sisteInnflyttetDato = innflytting.mapNotNull { it.dato }.maxOrNull()
        val sisteUtflyttetDato = utflytting.mapNotNull { it.dato }.maxOrNull()

        val femÅrSidenNå = LocalDate.now().minusYears(5)

        val boddINorgeSisteFemÅr =
            if (sisteInnflyttetDato == null) {
                false
            } else if (sisteUtflyttetDato == null) {
                sisteInnflyttetDato < femÅrSidenNå
            } else {
                sisteInnflyttetDato < femÅrSidenNå && sisteUtflyttetDato < sisteInnflyttetDato
            }

        return boddINorgeSisteFemÅr
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
