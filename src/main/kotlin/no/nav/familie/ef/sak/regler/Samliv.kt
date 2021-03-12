package no.nav.familie.ef.sak.regler

private enum class SamlivRegel(override val id: String,
                               override val beskrivelse: String) : RegelIdMedBeskrivelse {

    LEVER_IKKE_MED_ANNEN_FORELDER("SAMLIV_1",
                                  "Er vilkåret om å ikke leve sammen med den andre av barnets/barnas foreldre oppfylt?"),
    LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD("SAMLIV_2",
                                          "Er vilkåret om å ikke leve i et ekteskapslignende forhold i felles husholdning uten felles barn oppfylt?")
}

class Samliv : Vilkårsregel(vilkårType = VilkårType.SAMLIV,
                            regler = setOf(LEVER_IKKE_MED_ANNEN_FORELDER,
                                           LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD),
                            root = regelIds(LEVER_IKKE_MED_ANNEN_FORELDER,
                                            LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD)) {

    companion object {

        val LEVER_IKKE_MED_ANNEN_FORELDER =
                RegelSteg(regelId = SamlivRegel.LEVER_IKKE_MED_ANNEN_FORELDER,
                          hvisJaBegrunnelse = Begrunnelse.VALGFRI,
                          hvisNeiBegrunnelse = Begrunnelse.VALGFRI)

        val LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD =
                RegelSteg(regelId = SamlivRegel.LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD,
                          hvisJaBegrunnelse = Begrunnelse.PÅKREVD,
                          hvisNeiBegrunnelse = Begrunnelse.PÅKREVD)
    }
}
