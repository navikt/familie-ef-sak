package no.nav.familie.ef.sak.vurdering

import no.nav.familie.ef.sak.api.dto.InngangsvilkårGrunnlagDto
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.repository.domain.DelvilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad


/**
 * Filtrerer bort delvikår som ikke skall vurderes iht data i søknaden
 */

fun utledDelvilkårResultat(delvilkårType: DelvilkårType,
                           søknad: SøknadsskjemaOvergangsstønad,
                           registerGrunnlag: InngangsvilkårGrunnlagDto): Vilkårsresultat {
    return if (erDelvilkårAktueltForSøknaden(delvilkårType, søknad, registerGrunnlag)) {
        Vilkårsresultat.IKKE_VURDERT
    } else {
        Vilkårsresultat.IKKE_AKTUELL
    }
}

fun validerDelvilkår(vurdering: VilkårsvurderingDto,
                     vilkårsvurdering: Vilkårsvurdering) {
    val innkommendeDelvurderinger = vurdering.delvilkårsvurderinger.map { it.type }.toSet()
    val lagredeDelvurderinger = vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger.map { it.type }.toSet()

    if (innkommendeDelvurderinger.size != lagredeDelvurderinger.size
        || !innkommendeDelvurderinger.containsAll(lagredeDelvurderinger)) {
        error("Delvilkårstyper motsvarer ikke de som finnes lagrede på vilkåret")
    }
}


private fun erDelvilkårAktueltForSøknaden(it: DelvilkårType,
                                          søknad: SøknadsskjemaOvergangsstønad,
                                          register: InngangsvilkårGrunnlagDto): Boolean {
    val sivilstandType = register.sivilstand.registergrunnlag.type

    return when (it) {
        DelvilkårType.DOKUMENTERT_EKTESKAP -> måDokumentereEkteskap(sivilstandType, søknad)
        DelvilkårType.DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE -> måDokumentereSeparasjonEllerSkilsmisse(sivilstandType,
                                                                                                        søknad)
        DelvilkårType.SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON -> måDokumentereSamlivsbrudd(sivilstandType, søknad)
        DelvilkårType.SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING -> samslivsbruddDatoerSamsvarer(sivilstandType)
        DelvilkårType.KRAV_SIVILSTAND -> kravTilSivilstand(sivilstandType)

        else -> true
    }
}

private fun samslivsbruddDatoerSamsvarer(sivilstandType: Sivilstandstype) =
        (sivilstandType == Sivilstandstype.SEPARERT || sivilstandType == Sivilstandstype.SEPARERT_PARTNER)

private fun måDokumentereSamlivsbrudd(sivilstandType: Sivilstandstype,
                                      søknad: SøknadsskjemaOvergangsstønad) =
        (sivilstandType == Sivilstandstype.GIFT || sivilstandType == Sivilstandstype.REGISTRERT_PARTNER) && søknad.sivilstand.søktOmSkilsmisseSeparasjon == true

private fun måDokumentereSeparasjonEllerSkilsmisse(sivilstandType: Sivilstandstype,
                                                   søknad: SøknadsskjemaOvergangsstønad) =
        (sivilstandType == Sivilstandstype.UGIFT || sivilstandType == Sivilstandstype.UOPPGITT) && søknad.sivilstand.erUformeltSeparertEllerSkilt == true

private fun måDokumentereEkteskap(sivilstandType: Sivilstandstype,
                                  søknad: SøknadsskjemaOvergangsstønad) =
        (sivilstandType == Sivilstandstype.UGIFT || sivilstandType == Sivilstandstype.UOPPGITT) && søknad.sivilstand.erUformeltGift == true

private fun kravTilSivilstand(sivilstandType: Sivilstandstype) = sivilstandType != Sivilstandstype.ENKE_ELLER_ENKEMANN && sivilstandType != Sivilstandstype.GJENLEVENDE_PARTNER


