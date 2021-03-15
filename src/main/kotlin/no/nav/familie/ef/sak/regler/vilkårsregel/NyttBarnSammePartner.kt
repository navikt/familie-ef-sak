package no.nav.familie.ef.sak.regler

private enum class NyttBarnSammePartnerRegel(override val id: String,
                                             override val beskrivelse: String) : RegelIdMedBeskrivelse {

    HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER("N1", "Bor og oppholder bruker og barna seg i Norge?")
}

class NyttBarnSammePartner : Vilkårsregel(vilkårType = VilkårType.NYTT_BARN_SAMME_PARTNER,
                                          regler = setOf(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER),
                                          rotregler = regelIds(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER)) {

    companion object {

        val HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER =
                RegelSteg(regelId = NyttBarnSammePartnerRegel.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER,
                          svarMapping = jaNeiMapping())
    }
}
