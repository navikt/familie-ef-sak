package no.nav.familie.ef.sak.regler.vilkårsregel

import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttRegel
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiMapping
import no.nav.familie.ef.sak.regler.regelIds
import no.nav.familie.ef.sak.repository.domain.VilkårType

class Aleneomsorg : Vilkårsregel(vilkårType = VilkårType.ALENEOMSORG,
                                 regler = setOf(SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                                                NÆRE_BOFORHOLD,
                                                MER_AV_DAGLIG_OMSORG),
                                 hovedregler = regelIds(SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                                                        NÆRE_BOFORHOLD,
                                                        MER_AV_DAGLIG_OMSORG)) {

    companion object {

        private val MER_AV_DAGLIG_OMSORG =
                RegelSteg(regelId = RegelId.MER_AV_DAGLIG_OMSORG,
                          svarMapping = jaNeiMapping())

        private val næreBoForholdMapping =
                setOf(SvarId.SAMME_HUS_OG_FÆRRE_ENN_4_BOENHETER,
                      SvarId.SAMME_HUS_OG_FLERE_ENN_4_BOENHETER_MEN_VURDERT_NÆRT,
                      SvarId.SELVSTENDIGE_BOLIGER_SAMME_GÅRDSTUN,
                      SvarId.SELVSTENDIGE_BOLIGER_SAMME_TOMT,
                      SvarId.NÆRMESTE_BOLIG_ELLER_REKKEHUS_I_SAMMEGATE,
                      SvarId.TILSTØTENDE_BOLIGER_ELLER_REKKEHUS_I_SAMMEGATE)
                        .map { it to SluttRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE }
                        .toMap()
        private val NÆRE_BOFORHOLD =
                RegelSteg(regelId = RegelId.NÆRE_BOFORHOLD,
                          svarMapping = næreBoForholdMapping)

        private val SKRIFTLIG_AVTALE_OM_DELT_BOSTED =
                RegelSteg(regelId = RegelId.SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                          jaNeiMapping(hvisJa = SluttRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                       hvisNei = SluttRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE))

    }
}
