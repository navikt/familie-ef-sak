package no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.domain.OpplysningerOmAdresse
import no.nav.familie.ef.sak.vilkår.dto.OpplysningerOmAdresseDto

object OpplysningerOmAdresseMapper {

    fun tilDto(opplysningerOmAdresse: OpplysningerOmAdresse?) = OpplysningerOmAdresseDto(
        opplysningerOmAdresse?.adresse,
        opplysningerOmAdresse?.søkerBorPåRegistrertAdresse,
        opplysningerOmAdresse?.harMeldtFlytteendring
    )
}
