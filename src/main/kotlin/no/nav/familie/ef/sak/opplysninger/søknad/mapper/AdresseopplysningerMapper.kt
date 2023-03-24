package no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.domain.Adresseopplysninger
import no.nav.familie.ef.sak.vilkår.dto.AdresseopplysningerDto

object AdresseopplysningerMapper {

    fun tilDto(adresseopplysninger: Adresseopplysninger?): AdresseopplysningerDto? =
        adresseopplysninger?.søkerBorPåRegistrertAdresse?.let {
            AdresseopplysningerDto(
                søkerBorPåRegistrertAdresse = adresseopplysninger.søkerBorPåRegistrertAdresse,
                adresse = adresseopplysninger.adresse,
                harMeldtAdresseendring = adresseopplysninger.harMeldtAdresseendring,
            )
        }
}
