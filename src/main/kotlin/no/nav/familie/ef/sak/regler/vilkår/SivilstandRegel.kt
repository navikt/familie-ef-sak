package no.nav.familie.ef.sak.regler.vilkår

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.regler.HovedregelMetadata
import no.nav.familie.ef.sak.regler.NesteRegel
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttSvarRegel
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.regler.regelIder
import no.nav.familie.ef.sak.repository.domain.Delvilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vurdering
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad

class SivilstandRegel : Vilkårsregel(vilkårType = VilkårType.SIVILSTAND,
                                     regler = setOf(KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE,
                                                    KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE,
                                                    SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                                    SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                                    UNNTAK),
                                     hovedregler = regelIder(KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE,
                                                             KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE,
                                                             SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                                             SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                                             UNNTAK)) {

    override fun initereDelvilkårsvurdering(metadata: HovedregelMetadata): List<Delvilkårsvurdering> {
        val (søknad: SøknadsskjemaOvergangsstønad, sivilstandstype: Sivilstandstype) = metadata

        val hovedregel: RegelId = when {
            sivilstandstype.erUgiftEllerUoppgitt() && (søknad.sivilstand.erUformeltGift == true || søknad.sivilstand.erUformeltSeparertEllerSkilt == true) -> KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE
            sivilstandstype.erUgiftEllerUoppgitt() -> KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE

            sivilstandstype.erGift() && søknad.sivilstand.søktOmSkilsmisseSeparasjon == true -> SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON
            sivilstandstype.erGift() -> KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE

            sivilstandstype.erSeparert() -> SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING
            sivilstandstype.erSkilt() -> KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE
            sivilstandstype.erEnkeEllerEnkemann() -> UNNTAK
            else -> throw Feil("Finner ikke matchende sivilstand for $sivilstandstype")
        }.regelId

        return hovedregler.map {
            val resultat = if (it == hovedregel) Vilkårsresultat.IKKE_TATT_STILLING_TIL else Vilkårsresultat.IKKE_AKTUELL
            Delvilkårsvurdering(resultat = resultat, listOf(Vurdering(it)))
        }
    }

    companion object {

        private fun påkrevdBegrunnelse(regelId: RegelId) =
                RegelSteg(regelId = regelId,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                                       hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE))

        private val KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE = påkrevdBegrunnelse(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE)
        private val KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE =
                RegelSteg(regelId = RegelId.KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                                                       hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE))

        private val SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON = påkrevdBegrunnelse(RegelId.SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON)

        private val SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING =
                RegelSteg(regelId = RegelId.SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                          svarMapping = jaNeiSvarRegel(hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                                       hvisNei = NesteRegel(KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE.regelId)))

        private val UNNTAK = RegelSteg(regelId = RegelId.SIVILSTAND_UNNTAK,
                                       svarMapping = mapOf(
                                               SvarId.GJENLEVENDE_IKKE_RETT_TIL_YTELSER to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                               SvarId.GJENLEVENDE_OVERTAR_OMSORG to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                                               SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
                                       ))
    }
}
