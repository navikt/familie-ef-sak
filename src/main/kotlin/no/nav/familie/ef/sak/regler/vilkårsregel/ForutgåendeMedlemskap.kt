package no.nav.familie.ef.sak.regler

private enum class ForutgåendeMedlemskapRegel(override val id: String,
                                              override val beskrivelse: String) : RegelIdMedBeskrivelse {

    SØKER_MEDLEM_I_FOLKETRYGDEN("F1", "Har søker vært medlem i folketrygden i de siste 5 årene?"),
    UNNTAK("F2", "Er unntak fra hovedreglen oppfylt?")
}

@Suppress("unused")
enum class MedlemskapUnntakÅrsak(override val regelNod: RegelNod = SluttRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE)
    : SvarMedSvarsalternativ {

    MEDLEM_MER_ENN_5_ÅR_AVBRUDD_MINDRE_ENN_10_ÅR,
    MEDLEM_MER_ENN_7_ÅR_AVBRUDD_MER_ENN_10ÅR,
    I_LANDET_FOR_GJENFORENING_ELLER_GIFTE_SEG,
    ANDRE_FORELDER_MEDLEM_SISTE_5_ÅR,
    ANDRE_FORELDER_MEDLEM_MINST_5_ÅR_AVBRUDD_MINDRE_ENN_10_ÅR,
    ANDRE_FORELDER_MEDLEM_MINST_7_ÅR_AVBRUDD_MER_ENN_10_ÅR,
    TOTALVURDERING_OPPFYLLER_FORSKRIFT,
    NEI(SluttRegel.IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE)
}

class ForutgåendeMedlemskap
    : Vilkårsregel(vilkårType = VilkårType.MEDLEMSKAP,
                   regler = setOf(søkerMedlemIFolketrygdenSiste5Åren, unntaksregel),
                   rotregler = regelIds(søkerMedlemIFolketrygdenSiste5Åren)) {

    companion object {

        val unntaksregel =
                RegelSteg(regelId = ForutgåendeMedlemskapRegel.UNNTAK,
                          svarMapping = MedlemskapUnntakÅrsak::class)

        val søkerMedlemIFolketrygdenSiste5Åren =
                RegelSteg(regelId = ForutgåendeMedlemskapRegel.SØKER_MEDLEM_I_FOLKETRYGDEN,
                          svarMapping = defaultSvarMapping(hvisJa = SluttRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                                           hvisNei = NesteRegel(unntaksregel.regelId))
                )
    }

}