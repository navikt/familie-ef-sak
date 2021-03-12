package no.nav.familie.ef.sak.regler

private enum class SivilstandRegel(override val id: String,
                                   override val beskrivelse: String) : RegelIdMedBeskrivelse {

    DOKUMENTERT_EKTESKAP("S1", "Foreligger det dokumentasjon på ekteskap?"),
    DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE("S2", "Foreligger det dokumentasjon på separasjon eller skilsmisse?"),
    SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON("S3", "Kan samlivsbrudd likestilles med formell separasjon?"),
    SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING("S4", "Er det samsvar mellom datoene for separasjon og fraflytting?"),
    KRAV_SIVILSTAND("S5", "Er krav til sivilstand oppfylt?"),
    UNNTAK("S_UNNTAK", "Er unntak fra hovedregelen oppfylt?")
}

enum class SivilstandUnntakÅrsaker(override val resultat: Resultat = Resultat.OPPFYLT) : Årsak {
    ARBEID_NORSK_ARBEIDSGIVER,
    UTENLANDSOPPHOLD_MINDRE_ENN_6_UKER,
    NEI(resultat = Resultat.IKKE_OPPFYLT)
}

//TODO noen vilkår er her skal være beroende på hva som finnes i søknaden/pdl ?
class Sivilstand : Vilkårsregel(vilkårType = VilkårType.LOVLIG_OPPHOLD,
                                regler = setOf(),
                                root = borEllerOppholderSegINorgeRegel.regelId) {

    companion object {

        val DOKUMENTERT_EKTESKAP =
                RegelSteg(regelId = SivilstandRegel.DOKUMENTERT_EKTESKAP,
                          hvisJa =,
                          hvisNei =)
        val DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE =
                RegelSteg(regelId = SivilstandRegel.DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                          hvisJa =,
                          hvisNei =)
        val SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON =
                RegelSteg(regelId = SivilstandRegel.SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                          hvisJa =,
                          hvisNei =)
        val SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING =
                RegelSteg(regelId = SivilstandRegel.SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                          hvisJa =,
                          hvisNei =)
        val KRAV_SIVILSTAND =
                RegelSteg(regelId = SivilstandRegel.KRAV_SIVILSTAND,
                          hvisJa =,
                          hvisNei =)
        val UNNTAK =
                RegelSteg(regelId = SivilstandRegel.UNNTAK,
                          hvisJa =,
                          hvisNei =)

        val unntaksregel =
                RegelSteg(regelId = SivilstandRegel.UNNTAK,
                          hvisJa = ResultatRegel.OPPFYLT_MED_OPTIONAL_BEGRUNNELSE,
                          hvisNei = ResultatRegel.OPPFYLT_MED_OPTIONAL_BEGRUNNELSE,
                          årsaker = MedlemskapUnntakÅrsak::class)

        val borEllerOppholderSegINorgeRegel =
                RegelSteg(regelId = SivilstandRegel.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                          hvisJa = ResultatRegel.OPPFYLT_MED_OPTIONAL_BEGRUNNELSE,
                          hvisNei = unntaksregel.regelId)
    }
}
