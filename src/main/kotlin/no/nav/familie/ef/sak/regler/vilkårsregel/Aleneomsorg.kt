package no.nav.familie.ef.sak.regler

private enum class AleneomsorgRegel(override val id: String,
                                    override val beskrivelse: String) : RegelIdMedBeskrivelse {

    SKRIFTLIG_AVTALE_OM_DELT_BOSTED("A1", ""),
    NÆRE_BOFORHOLD("A2", ""),
    MER_AV_DAGLIG_OMSORG("A3", "")
}

@Suppress("unused")
private enum class NæreBofoorholdÅrsaker(override val regelNod: RegelNod = SluttRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)
    : SvarMedSvarsalternativ {

    SAMME_HUS_OG_FÆRRE_ENN_4_BOENHETER,
    SAMME_HUS_OG_FLERE_ENN_4_BOENHETER_MEN_VURDERT_NÆRT,
    SELVSTENDIGE_BOLIGER_SAMME_GÅRDSTUN,
    SELVSTENDIGE_BOLIGER_SAMME_TOMT,
    NÆRMESTE_BOLIG_ELLER_REKKEHUS_I_SAMMEGATE,
    TILSTØTENDE_BOLIGER_ELLER_REKKEHUS_I_SAMMEGATE,
}

class Aleneomsorg : Vilkårsregel(vilkårType = VilkårType.ALENEOMSORG,
                                 regler = setOf(SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                                                NÆRE_BOFORHOLD,
                                                MER_AV_DAGLIG_OMSORG),
                                 rotregler = regelIds(SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                                                      NÆRE_BOFORHOLD,
                                                      MER_AV_DAGLIG_OMSORG)) {

    companion object {

        val MER_AV_DAGLIG_OMSORG =
                RegelSteg(regelId = AleneomsorgRegel.MER_AV_DAGLIG_OMSORG,
                          svarMapping = defaultSvarMapping())

        val NÆRE_BOFORHOLD =
                RegelSteg(regelId = AleneomsorgRegel.NÆRE_BOFORHOLD,
                          svarMapping = NæreBofoorholdÅrsaker::class)

        val SKRIFTLIG_AVTALE_OM_DELT_BOSTED =
                RegelSteg(regelId = AleneomsorgRegel.SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                          defaultSvarMapping(hvisJa = SluttRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                             hvisNei = SluttRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE))

    }
}
