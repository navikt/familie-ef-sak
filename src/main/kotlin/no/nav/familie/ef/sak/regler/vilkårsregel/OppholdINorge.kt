package no.nav.familie.ef.sak.regler

private enum class OppholdINorgeRegel(override val id: String,
                                      override val beskrivelse: String) : RegelIdMedBeskrivelse {

    BOR_OG_OPPHOLDER_SEG_I_NORGE("O1", "Bor og oppholder bruker og barna seg i Norge?"),
    UNNTAK("O2", "Er unntak fra hovedregelen oppfylt?")
}

@Suppress("unused")
private enum class OppholdINorgeUnntakÅrsaker(override val mapping: JaNei = JaNei.JA) : Årsak {

    ARBEID_NORSK_ARBEIDSGIVER,
    UTENLANDSOPPHOLD_MINDRE_ENN_6_UKER,
    NEI(mapping = JaNei.NEI)
}

class OppholdINorge : Vilkårsregel(vilkårType = VilkårType.LOVLIG_OPPHOLD,
                                   regler = setOf(borEllerOppholderSegINorgeRegel, unntaksregel),
                                   root = regelIds(borEllerOppholderSegINorgeRegel)) {

    companion object {

        val unntaksregel =
                RegelSteg(regelId = OppholdINorgeRegel.UNNTAK,
                          hvisJaBegrunnelse = Begrunnelse.VALGFRI,
                          hvisNeiBegrunnelse = Begrunnelse.VALGFRI,
                          årsaker = MedlemskapUnntakÅrsak::class)

        val borEllerOppholderSegINorgeRegel =
                RegelSteg(regelId = OppholdINorgeRegel.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                          hvisNei = unntaksregel.regelId,
                          hvisJaBegrunnelse = Begrunnelse.VALGFRI)
    }
}
