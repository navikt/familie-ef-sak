package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class TidligareVedtaksperioderRegel : Vilkårsregel(vilkårType = VilkårType.TIDLIGERE_VEDTAKSPERIODER,
                                                   regler = setOf(HAR_TIDLIGERE_MOTTATT_OVERGANSSTØNAD,
                                                                  LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD),
                                                   hovedregler = regelIder(HAR_TIDLIGERE_MOTTATT_OVERGANSSTØNAD,
                                                                           LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD)) {

    override fun initereDelvilkårsvurdering(metadata: HovedregelMetadata,
                                            resultat: Vilkårsresultat): List<Delvilkårsvurdering> {
        return listOf(Delvilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                          listOf(Vurdering(regelId = RegelId.HAR_TIDLIGERE_MOTTATT_OVERGANSSTØNAD,
                                                           svar = SvarId.NEI))),
                      Delvilkårsvurdering(resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                          listOf(Vurdering(regelId = RegelId.HAR_TIDLIGERE_ANDRE_STØNADER_SOM_HAR_BETYDNING))))
    }

    companion object {

        private val HAR_TIDLIGERE_MOTTATT_OVERGANSSTØNAD =
                RegelSteg(regelId = RegelId.HAR_TIDLIGERE_MOTTATT_OVERGANSSTØNAD,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                                       hvisNei = SluttSvarRegel.OPPFYLT))

        private val LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD =
                RegelSteg(regelId = RegelId.HAR_TIDLIGERE_ANDRE_STØNADER_SOM_HAR_BETYDNING,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                                       hvisNei = SluttSvarRegel.OPPFYLT))
    }
}