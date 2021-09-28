package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder
import no.nav.familie.ef.sak.vilkår.VilkårType

class AleneomsorgRegel : Vilkårsregel(vilkårType = VilkårType.ALENEOMSORG,
                                      regler = setOf(SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                                                     NÆRE_BOFORHOLD,
                                                     MER_AV_DAGLIG_OMSORG),
                                      hovedregler = regelIder(SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                                                              NÆRE_BOFORHOLD,
                                                              MER_AV_DAGLIG_OMSORG)) {

    companion object {

        private val MER_AV_DAGLIG_OMSORG =
                RegelSteg(regelId = RegelId.MER_AV_DAGLIG_OMSORG,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                                       hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE))

        private val næreBoForholdMapping =
                setOf(SvarId.SAMME_HUS_OG_FÆRRE_ENN_4_BOENHETER,
                      SvarId.SAMME_HUS_OG_FLERE_ENN_4_BOENHETER_MEN_VURDERT_NÆRT,
                      SvarId.SELVSTENDIGE_BOLIGER_SAMME_GÅRDSTUN,
                      SvarId.SELVSTENDIGE_BOLIGER_SAMME_TOMT,
                      SvarId.NÆRMESTE_BOLIG_ELLER_REKKEHUS_I_SAMMEGATE,
                      SvarId.TILSTØTENDE_BOLIGER_ELLER_REKKEHUS_I_SAMMEGATE)
                        .map { it to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE }
                        .toMap() + mapOf(SvarId.NEI to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)
        private val NÆRE_BOFORHOLD =
                RegelSteg(regelId = RegelId.NÆRE_BOFORHOLD,
                          svarMapping = næreBoForholdMapping)

        private val SKRIFTLIG_AVTALE_OM_DELT_BOSTED =
                RegelSteg(regelId = RegelId.SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                          jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                         hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE))

    }
}
