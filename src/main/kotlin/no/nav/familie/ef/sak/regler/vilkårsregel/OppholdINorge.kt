package no.nav.familie.ef.sak.regler

private enum class OppholdINorgeRegel(override val id: String,
                                      override val beskrivelse: String) : RegelIdMedBeskrivelse {

    BOR_OG_OPPHOLDER_SEG_I_NORGE("O1", "Bor og oppholder bruker og barna seg i Norge?"),
    OPPHOLD_UNNTAK("O2", "Er unntak fra hovedregelen oppfylt?")
}

@Suppress("unused")
private enum class OppholdINorgeUnntakÅrsaker(override val regelNod: RegelNod) : SvarMedSvarsalternativ {

    ARBEID_NORSK_ARBEIDSGIVER(SluttRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE),
    UTENLANDSOPPHOLD_MINDRE_ENN_6_UKER(SluttRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE),
    NEI(SluttRegel.IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE)
}

class OppholdINorge : Vilkårsregel(vilkårType = VilkårType.LOVLIG_OPPHOLD,
                                   regler = setOf(borEllerOppholderSegINorgeRegel, unntaksregel),
                                   rotregler = regelIds(borEllerOppholderSegINorgeRegel)) {

    companion object {

        val unntaksregel =
                RegelSteg(regelId = OppholdINorgeRegel.OPPHOLD_UNNTAK,
                          svarMapping = OppholdINorgeUnntakÅrsaker::class)

        val borEllerOppholderSegINorgeRegel =
                RegelSteg(regelId = OppholdINorgeRegel.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                          svarMapping = defaultSvarMapping(hvisJa = SluttRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                                           hvisNei = NesteRegel(unntaksregel.regelId)
                          )
                )
    }
}
