package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.NesteRegel
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder
import no.nav.familie.kontrakter.felles.Fødselsnummer
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
        if (resultat != Vilkårsresultat.IKKE_TATT_STILLING_TIL) {
            return super.initiereDelvilkårsvurdering(metadata, resultat, barnId)
        }
        val harFullførtFjerdetrinn = harFullførtFjedetrinn(metadata, barnId)
        return listOf(
            Delvilkårsvurdering(
                resultat = if (harFullførtFjerdetrinn == SvarId.NEI) Vilkårsresultat.AUTOMATISK_OPPFYLT else Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                listOf(
                    Vurdering(
                        regelId = RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI,
                        svar = harFullførtFjerdetrinn,
                        begrunnelse = if (harFullførtFjerdetrinn == SvarId.NEI) {
                            "Automatisk vurdert: Ut ifra barnets alder er det ${
                                LocalDate.now()
                                    .norskFormat()
                            } automatisk vurdert at barnet ikke har fullført 4. skoleår."
                        } else {
                            null
                        },
                    ),
                ),
            ),
        )
    }

    private fun harFullførtFjedetrinn(
        metadata: HovedregelMetadata,
        barnId: UUID?,
    ): SvarId? {
        val fødselsdato = metadata.barn.firstOrNull { it.id == barnId }
            ?.personIdent
            ?.let { Fødselsnummer(it).fødselsdato }
        return if (fødselsdato == null || harFullførtFjerdetrinn(fødselsdato)) {
            null
        } else {
            SvarId.NEI
        }
    }

    companion object {

        private val unntakAlderMapping =
            setOf(
                SvarId.TRENGER_MER_TILSYN_ENN_JEVNALDRENDE,
                SvarId.FORSØRGER_HAR_LANGVARIG_ELLER_UREGELMESSIG_ARBEIDSTID,
            )
                .associateWith {
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
                svarMapping = jaNeiSvarRegel(
                    hvisJa = NesteRegel(UNNTAK_ALDER.regelId),
                    hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                ),
            )
    }

    fun harFullførtFjerdetrinn(fødselsdato: LocalDate, datoForBeregning: LocalDate = LocalDate.now()): Boolean {
        val alder = datoForBeregning.year - fødselsdato.year
        var skoletrinn = alder - 5 // Begynner på skolen i det året de fyller 6
        if (datoForBeregning.month.plus(1).value < 6) { // Legger til en sikkerhetsmargin på 1 mnd tilfelle de fyller år mens saken behandles
            skoletrinn--
        }
        return skoletrinn > 4
    }
}
