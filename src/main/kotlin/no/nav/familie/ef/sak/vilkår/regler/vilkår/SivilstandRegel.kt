package no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstand
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.NesteRegel
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder

class SivilstandRegel : Vilkårsregel(
    vilkårType = VilkårType.SIVILSTAND,
    regler = setOf(
        KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE,
        KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE,
        SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
        SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
        UNNTAK
    ),
    hovedregler = regelIder(
        KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE,
        KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE,
        SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
        SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
        UNNTAK
    )
) {

    override fun initereDelvilkårsvurdering(metadata: HovedregelMetadata, resultat: Vilkårsresultat): List<Delvilkårsvurdering> {
        val (sivilstandSøknad: Sivilstand?, sivilstandstype: Sivilstandstype) = metadata

        val hovedregel: RegelId = when {
            sivilstandstype.erUgiftEllerUoppgitt() &&
                sivilstandSøknad != null &&
                (sivilstandSøknad.erUformeltGift == true || sivilstandSøknad.erUformeltSeparertEllerSkilt == true) ->
                KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE
            sivilstandstype.erUgiftEllerUoppgitt() -> KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE

            sivilstandstype.erGift() && sivilstandSøknad != null && sivilstandSøknad.søktOmSkilsmisseSeparasjon == true ->
                SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON
            sivilstandstype.erGift() -> KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE

            sivilstandstype.erSeparert() -> SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING
            sivilstandstype.erSkilt() -> KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE
            sivilstandstype.erEnkeEllerEnkemann() -> UNNTAK
            else -> throw Feil("Finner ikke matchende sivilstand for $sivilstandstype")
        }.regelId

        return hovedregler.map {
            val resultatForDelvilkår = if (it == hovedregel) resultat else Vilkårsresultat.IKKE_AKTUELL
            Delvilkårsvurdering(resultat = resultatForDelvilkår, listOf(Vurdering(it)))
        }
    }

    companion object {

        private fun påkrevdBegrunnelse(regelId: RegelId) =
            RegelSteg(
                regelId = regelId,
                svarMapping = jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
                )
            )

        private val KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE = påkrevdBegrunnelse(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE)
        private val KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE =
            RegelSteg(
                regelId = RegelId.KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE,
                svarMapping = jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
                )
            )

        private val SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON = påkrevdBegrunnelse(RegelId.SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON)

        private val SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING =
            RegelSteg(
                regelId = RegelId.SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                svarMapping = jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = NesteRegel(KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE.regelId)
                )
            )

        private val UNNTAK =
            RegelSteg(
                regelId = RegelId.SIVILSTAND_UNNTAK,
                svarMapping = mapOf(
                    SvarId.GJENLEVENDE_IKKE_RETT_TIL_YTELSER to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    SvarId.GJENLEVENDE_OVERTAR_OMSORG to SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
                )
            )
    }
}
