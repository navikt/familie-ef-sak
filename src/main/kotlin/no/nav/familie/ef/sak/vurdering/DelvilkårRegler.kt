package no.nav.familie.ef.sak.vurdering

import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.repository.domain.DelvilkårMetadata
import no.nav.familie.ef.sak.repository.domain.DelvilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad


/**
 * Filtrerer bort delvikår som ikke skall vurderes iht data i søknaden
 */

fun utledDelvilkårResultat(delvilkårType: DelvilkårType,
                           søknad: SøknadsskjemaOvergangsstønad,
                           delvilkårMetadata: DelvilkårMetadata): Vilkårsresultat {
    return if (erDelvilkårAktueltForSøknaden(delvilkårType, søknad, delvilkårMetadata)) {
        Vilkårsresultat.IKKE_VURDERT
    } else {
        Vilkårsresultat.IKKE_AKTUELL
    }
}

fun validerDelvilkår(oppdatert: VilkårsvurderingDto,
                     eksisterende: Vilkårsvurdering) {
    val innkommendeDelvurderinger = oppdatert.delvilkårsvurderinger.map { it.type }.toSet()
    val lagredeDelvurderinger = eksisterende.delvilkårsvurdering.delvilkårsvurderinger.map { it.type }.toSet()

    require(innkommendeDelvurderinger.size == lagredeDelvurderinger.size) { "Nye og eksisterende delvilkårsvurderinger har ulike antall vurderinger" }
    require(innkommendeDelvurderinger.containsAll(lagredeDelvurderinger)) { "Nye delvilkårsvurderinger mangler noen eksisterende vurderinger" }
}


private fun erDelvilkårAktueltForSøknaden(delvilkårType: DelvilkårType,
                                          søknad: SøknadsskjemaOvergangsstønad,
                                          delvilkårMetadata: DelvilkårMetadata): Boolean {
    val sivilstandType = delvilkårMetadata.sivilstandstype

    return when (delvilkårType) {
        DelvilkårType.DOKUMENTERT_EKTESKAP -> måDokumentereEkteskap(sivilstandType, søknad)
        DelvilkårType.DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE -> måDokumentereSeparasjonEllerSkilsmisse(sivilstandType, søknad)
        DelvilkårType.SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON -> måDokumentereSamlivsbrudd(sivilstandType, søknad)
        DelvilkårType.SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING -> måVerifisereDatoerForSamlivsbrudd(sivilstandType)
        DelvilkårType.KRAV_SIVILSTAND -> måVerifisereKravTilSivilstand(sivilstandType)
        else -> true
    }
}

private fun måVerifisereDatoerForSamlivsbrudd(sivilstandType: Sivilstandstype) = sivilstandType.erSeparert()

private fun måDokumentereSamlivsbrudd(sivilstandType: Sivilstandstype,
                                      søknad: SøknadsskjemaOvergangsstønad) =
        sivilstandType.erGift() && søknad.sivilstand.søktOmSkilsmisseSeparasjon == true

private fun måDokumentereSeparasjonEllerSkilsmisse(sivilstandType: Sivilstandstype,
                                                   søknad: SøknadsskjemaOvergangsstønad) =
        sivilstandType.erUgiftEllerUoppgitt() && søknad.sivilstand.erUformeltSeparertEllerSkilt == true

private fun måDokumentereEkteskap(sivilstandType: Sivilstandstype,
                                  søknad: SøknadsskjemaOvergangsstønad) =
        sivilstandType.erUgiftEllerUoppgitt() && søknad.sivilstand.erUformeltGift == true

private fun måVerifisereKravTilSivilstand(sivilstandType: Sivilstandstype) = !sivilstandType.erEnkeEllerEnkemann()


