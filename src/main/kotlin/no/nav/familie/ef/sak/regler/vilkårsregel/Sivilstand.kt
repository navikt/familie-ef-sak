package no.nav.familie.ef.sak.regler.vilkårsregel

import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttRegel
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.regelIds
import no.nav.familie.ef.sak.repository.domain.VilkårType

// skalIkkeVurdere: [DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE, SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON]

class Sivilstand : Vilkårsregel(vilkårType = VilkårType.SIVILSTAND,
                                regler = setOf(DOKUMENTERT_EKTESKAP,
                                               DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                                               SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                               SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                               KRAV_SIVILSTAND,
                                               UNNTAK),
                                rotregler = regelIds()) { //TODO?

    companion object {

        private fun påkrevdBegrunnelse(regelId: RegelId) =
                RegelSteg(regelId = regelId,
                          svarMapping = mapOf(
                                  SvarId.JA to SluttRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                  SvarId.NEI to SluttRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
                          ))

        private val DOKUMENTERT_EKTESKAP = påkrevdBegrunnelse(RegelId.DOKUMENTERT_EKTESKAP)
        private val DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE = påkrevdBegrunnelse(RegelId.DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE)
        private val SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON = påkrevdBegrunnelse(RegelId.SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON)
        private val SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING = påkrevdBegrunnelse(RegelId.SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING)
        private val KRAV_SIVILSTAND = påkrevdBegrunnelse(RegelId.KRAV_SIVILSTAND)
        private val UNNTAK = RegelSteg(regelId = RegelId.SIVILSTAND_UNNTAK,
                                       svarMapping = mapOf(
                                               SvarId.GJENLEVENDE_IKKE_RETT_TIL_YTELSER to SluttRegel.OPPFYLT,
                                               SvarId.GJENLEVENDE_OVERTAR_OMSORG to SluttRegel.OPPFYLT,
                                               SvarId.NEI to SluttRegel.IKKE_OPPFYLT
                                       ))
    }
}
