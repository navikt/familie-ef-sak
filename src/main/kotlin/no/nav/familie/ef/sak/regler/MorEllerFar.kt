package no.nav.familie.ef.sak.regler

private enum class MorEllerFarRegel(override val id: String,
                                    override val beskrivelse: String) : RegelIdMedBeskrivelse {

    OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN("O1", "Har bruker omsorgen for egne/adopterte barn?"),
}

class MorEllerFar : Vilkårsregel(vilkårType = VilkårType.MOR_ELLER_FAR,
                                 regler = setOf(harBrukerOmsorgForBarn),
                                 root = regelIds(harBrukerOmsorgForBarn)) {

    companion object {

        val harBrukerOmsorgForBarn =
                RegelSteg(regelId = MorEllerFarRegel.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN,
                          hvisJa = ResultatRegel.OPPFYLT,
                          hvisNei = ResultatRegel.IKKE_OPPFYLT)
    }
}
