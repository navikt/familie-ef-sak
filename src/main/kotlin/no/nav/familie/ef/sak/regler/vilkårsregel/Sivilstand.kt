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

@Suppress("unused")
private enum class SivilstandUnntakÅrsaker(override val mapping: JaNei = JaNei.JA) : Årsak {

    ARBEID_NORSK_ARBEIDSGIVER,
    UTENLANDSOPPHOLD_MINDRE_ENN_6_UKER,
    NEI(mapping = JaNei.NEI)
}

//TODO noen vilkår er her skal være beroende på hva som finnes i søknaden/pdl ?
class Sivilstand : Vilkårsregel(vilkårType = VilkårType.SIVILSTAND,
                                regler = setOf(DOKUMENTERT_EKTESKAP,
                                               DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                                               SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                               SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                               KRAV_SIVILSTAND, UNNTAK),
                                root = regelIds()) {

    companion object {

        private fun påkrevdBegrunnelse(regelId: SivilstandRegel) =
                RegelSteg(regelId = regelId,
                          hvisJaBegrunnelse = Begrunnelse.PÅKREVD,
                          hvisNeiBegrunnelse = Begrunnelse.PÅKREVD)

        val DOKUMENTERT_EKTESKAP = påkrevdBegrunnelse(SivilstandRegel.DOKUMENTERT_EKTESKAP)
        val DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE = påkrevdBegrunnelse(SivilstandRegel.DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE)
        val SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON = påkrevdBegrunnelse(SivilstandRegel.SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON)
        val SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING = påkrevdBegrunnelse(SivilstandRegel.SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING)
        val KRAV_SIVILSTAND = påkrevdBegrunnelse(SivilstandRegel.KRAV_SIVILSTAND)
        val UNNTAK = RegelSteg(regelId = SivilstandRegel.UNNTAK,
                               årsaker = SivilstandUnntakÅrsaker::class)
    }
}
