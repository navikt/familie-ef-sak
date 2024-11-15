package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.NesteRegel
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AlderPåBarnRegelUtil.harFullførtFjerdetrinn
import java.time.LocalDate
import java.util.UUID

class AlderPåBarnRegel :
    Vilkårsregel(
        vilkårType = VilkårType.ALDER_PÅ_BARN,
        regler = setOf(HAR_ALDER_LAVERE_ENN_GRENSEVERDI, UNNTAK_ALDER),
        hovedregler = regelIder(HAR_ALDER_LAVERE_ENN_GRENSEVERDI),
    ) {
    override fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: UUID?,
    ): List<Delvilkårsvurdering> {
        if (resultat != Vilkårsresultat.IKKE_TATT_STILLING_TIL || barnId == null) { // barnId kan være null ved migreringer, da behandlingbarn ikke er opprettet enda
            return super.initiereDelvilkårsvurdering(metadata, resultat, barnId)
        }

        return gjeldendeHovedregler().map {
            if (it == RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI && !harFullførtFjerdetrinn(metadata, barnId)) {
                automatisktOppfyltHarAlderLavereEnnGrenseverdi()
            } else {
                Delvilkårsvurdering(resultat, vurderinger = listOf(Vurdering(it)))
            }
        }
    }

    private fun automatisktOppfyltHarAlderLavereEnnGrenseverdi(): Delvilkårsvurdering {
        val beskrivelse =
            "Automatisk vurdert: Ut ifra barnets alder er det ${LocalDate.now().norskFormat()}" +
                " automatisk vurdert at barnet ikke har fullført 4. skoleår."
        return Delvilkårsvurdering(
            resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
            listOf(
                Vurdering(
                    regelId = RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI,
                    svar = SvarId.NEI,
                    begrunnelse = beskrivelse,
                ),
            ),
        )
    }

    private fun harFullførtFjerdetrinn(
        metadata: HovedregelMetadata,
        barnId: UUID?,
    ): Boolean {
        val fødselsdato = metadata.finnFødselsdatoEllerTermindatoForBarn(barnId)
        return harFullførtFjerdetrinn(fødselsdato)
    }

    private fun HovedregelMetadata.finnFødselsdatoEllerTermindatoForBarn(barnId: UUID?): LocalDate =
        vilkårgrunnlagDto.finnFødselsdatoForBarn(barnId)
            ?: barn.firstOrNull { it.id == barnId }?.fødselTermindato
            ?: error("Kunne ikke finne hverken fødselsdato fra registerdata eller termindato for barnID=$barnId")

    private fun VilkårGrunnlagDto.finnFødselsdatoForBarn(barnId: UUID?): LocalDate? = barnMedSamvær.firstOrNull { it.barnId == barnId }?.registergrunnlag?.fødselsdato

    companion object {
        private val unntakAlderMapping =
            setOf(
                SvarId.TRENGER_MER_TILSYN_ENN_JEVNALDRENDE,
                SvarId.FORSØRGER_HAR_LANGVARIG_ELLER_UREGELMESSIG_ARBEIDSTID,
            ).associateWith {
                SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
            } + mapOf(SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)

        private val UNNTAK_ALDER =
            RegelSteg(
                regelId = RegelId.UNNTAK_ALDER,
                svarMapping = unntakAlderMapping,
            )

        private val HAR_ALDER_LAVERE_ENN_GRENSEVERDI =
            RegelSteg(
                regelId = RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(UNNTAK_ALDER.regelId),
                        hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    ),
            )
    }
}
