package no.nav.familie.ef.sak.regler.vilkår

import no.nav.familie.ef.sak.regler.*
import no.nav.familie.ef.sak.repository.domain.Delvilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vurdering

class TidligareVedtaksperioderRegel : Vilkårsregel(vilkårType = VilkårType.TIDLIGERE_VEDTAKSPERIODER,
                                                   regler = setOf(HAR_TIDLIGERE_MOTTATT_OVERGANSSTØNAD,
                                                                  LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD),
                                                   hovedregler = regelIder(HAR_TIDLIGERE_MOTTATT_OVERGANSSTØNAD,
                                                                           LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD)) {

    override fun initereDelvilkårsvurdering(metadata: HovedregelMetadata): List<Delvilkårsvurdering> {
        return listOf(Delvilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                          listOf(Vurdering(regelId = RegelId.HAR_TIDLIGERE_MOTTATT_OVERGANSSTØNAD,
                                                           svar = SvarId.NEI))),
                      Delvilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                          listOf(Vurdering(regelId = RegelId.HAR_TIDLIGERE_ANDRE_STØNADER_SOM_HAR_BETYDNING,
                                                           svar = SvarId.NEI))))
    }

    companion object {

        private val HAR_TIDLIGERE_MOTTATT_OVERGANSSTØNAD =
                RegelSteg(regelId = RegelId.HAR_TIDLIGERE_MOTTATT_OVERGANSSTØNAD,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.IKKE_OPPFYLT,
                                                       hvisNei = SluttSvarRegel.OPPFYLT))

        private val LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD =
                RegelSteg(regelId = RegelId.HAR_TIDLIGERE_ANDRE_STØNADER_SOM_HAR_BETYDNING,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.IKKE_OPPFYLT,
                                                       hvisNei = SluttSvarRegel.OPPFYLT))
    }
}