package no.nav.familie.ef.sak.vurdering

import no.nav.familie.ef.sak.api.dto.OppdaterVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.repository.domain.DelvilkårMetadata
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad


/**
 * Filtrerer bort delvikår som ikke skall vurderes iht data i søknaden
 */
fun utledDelvilkårResultat(regelId: RegelId,
                           søknad: SøknadsskjemaOvergangsstønad,
                           delvilkårMetadata: DelvilkårMetadata): Vilkårsresultat {
    return if (erDelvilkårAktueltForSøknaden(regelId, søknad, delvilkårMetadata)) {
        Vilkårsresultat.IKKE_TATT_STILLING_TIL
    } else {
        Vilkårsresultat.IKKE_TATT_STILLING_TIL
    }
}

fun validerDelvilkår(oppdatert: OppdaterVilkårsvurderingDto,
                     eksisterende: Vilkårsvurdering) {
    val innkommendeDelvurderinger = oppdatert.delvilkårsvurderinger.map { it.hovedregel() }.toSet()
    val lagredeDelvurderinger = eksisterende.delvilkårsvurdering.delvilkårsvurderinger.map { it.hovedregel }.toSet()

    require(innkommendeDelvurderinger.size == lagredeDelvurderinger.size) { "Nye og eksisterende delvilkårsvurderinger har ulike antall vurderinger" }
    require(innkommendeDelvurderinger.containsAll(lagredeDelvurderinger)) { "Nye delvilkårsvurderinger mangler noen eksisterende vurderinger" }
}

private fun erDelvilkårAktueltForSøknaden(regelId: RegelId,
                                          søknad: SøknadsskjemaOvergangsstønad,
                                          delvilkårMetadata: DelvilkårMetadata): Boolean {
    val sivilstandType = delvilkårMetadata.sivilstandstype

    return when (regelId) {
        RegelId.DOKUMENTERT_EKTESKAP -> måDokumentereEkteskap(sivilstandType, søknad)
        RegelId.DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE -> måDokumentereSeparasjonEllerSkilsmisse(sivilstandType, søknad)
        RegelId.SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON -> måDokumentereSamlivsbrudd(sivilstandType, søknad)
        RegelId.SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING -> måVerifisereDatoerForSamlivsbrudd(sivilstandType)
        RegelId.KRAV_SIVILSTAND -> måVerifisereKravTilSivilstand(sivilstandType)
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


