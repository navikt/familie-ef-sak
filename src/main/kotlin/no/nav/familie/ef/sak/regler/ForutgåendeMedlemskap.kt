package no.nav.familie.ef.sak.regler

private enum class ForutgåendeMedlemskapRegel(override val id: String,
                                              override val beskrivelse: String) : RegelIdMedBeskrivelse {

    SØKER_MEDLEM_I_FOLKETRYGDEN("M1", "Har søker vært medlem i folketrygden i de siste 5 årene?"),
    UNNTAK("M2", "Er unntak fra hovedreglen oppfylt?")
}

enum class MedlemskapUnntakÅrsak(override val resultat: Resultat = Resultat.OPPFYLT) : Årsak {
    MEDLEM_MER_ENN_5_ÅR_AVBRUDD_MINDRE_ENN_10_ÅR,
    MEDLEM_MER_ENN_7_ÅR_AVBRUDD_MER_ENN_10ÅR,
    I_LANDET_FOR_GJENFORENING_ELLER_GIFTE_SEG,
    ANDRE_FORELDER_MEDLEM_SISTE_5_ÅR,
    ANDRE_FORELDER_MEDLEM_MINST_5_ÅR_AVBRUDD_MINDRE_ENN_10_ÅR,
    ANDRE_FORELDER_MEDLEM_MINST_7_ÅR_AVBRUDD_MER_ENN_10_ÅR,
    TOTALVURDERING_OPPFYLLER_FORSKRIFT,
    NEI(resultat = Resultat.IKKE_OPPFYLT)
}

class ForutgåendeMedlemskap : Vilkårsregel(vilkårType = VilkårType.MEDLEMSKAP,
                                           regler = setOf(søkerMedlemIFolketrygdenSiste5Åren, unntaksregel),
                                           root = søkerMedlemIFolketrygdenSiste5Åren.regelId) {

    companion object {

        val unntaksregel =
                RegelSteg(regelId = ForutgåendeMedlemskapRegel.UNNTAK,
                          hvisJaBegrunnelse = Begrunnelse.VALGFRI,
                          hvisNeiBegrunnelse = Begrunnelse.VALGFRI,
                          årsaker = MedlemskapUnntakÅrsak::class)

        val søkerMedlemIFolketrygdenSiste5Åren =
                RegelSteg(regelId = ForutgåendeMedlemskapRegel.SØKER_MEDLEM_I_FOLKETRYGDEN,
                          hvisJaBegrunnelse = Begrunnelse.VALGFRI,
                          hvisNei = unntaksregel.regelId)
    }

}
