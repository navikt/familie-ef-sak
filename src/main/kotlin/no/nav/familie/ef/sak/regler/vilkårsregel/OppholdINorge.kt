package no.nav.familie.ef.sak.regler.vilkårsregel

import no.nav.familie.ef.sak.regler.NesteRegel
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttRegel
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.regler.VilkårType
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiMapping
import no.nav.familie.ef.sak.regler.regelIds

class OppholdINorge :
        Vilkårsregel(vilkårType = VilkårType.LOVLIG_OPPHOLD,
                     regler = setOf(borEllerOppholderSegINorgeRegel, unntaksregel),
                     rotregler = regelIds(borEllerOppholderSegINorgeRegel)) {

    companion object {

        val unntaksregel =
                RegelSteg(regelId = RegelId.OPPHOLD_UNNTAK,
                          svarMapping = mapOf(
                                  SvarId.ARBEID_NORSK_ARBEIDSGIVER to SluttRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                  SvarId.UTENLANDSOPPHOLD_MINDRE_ENN_6_UKER to SluttRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                  SvarId.NEI to SluttRegel.IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE
                          ))

        val borEllerOppholderSegINorgeRegel =
                RegelSteg(regelId = RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                          svarMapping = jaNeiMapping(hvisJa = SluttRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                                     hvisNei = NesteRegel(unntaksregel.regelId)))
    }
}
