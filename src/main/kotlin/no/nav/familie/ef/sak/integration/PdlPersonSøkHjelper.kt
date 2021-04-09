package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.integration.dto.pdl.Bostedsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.SearchRule
import no.nav.familie.ef.sak.integration.dto.pdl.SøkeKriterier

object PdlPersonSøkHjelper {

     fun lagPdlPersonSøkCriteria(bostedsadresse: Bostedsadresse): List<SøkeKriterier> {
        if (bostedsadresse.matrikkeladresse?.matrikkelId != null) {
            return listOf(
                    lagSøkeKriterier(søkefelt = "person.bostedsadresse.matrikkeladresse.matrikkelId",
                                     søkeord = bostedsadresse.matrikkeladresse.matrikkelId.toString()))

        } else if (bostedsadresse.vegadresse != null) {
            val vegadresse = bostedsadresse.vegadresse
            return listOfNotNull(
                    vegadresse.adressenavn?.let {
                        lagSøkeKriterier(søkefelt = "person.bostedsadresse.vegadresse.adressenavn",
                                         søkeord = it)
                    },
                    vegadresse.bruksenhetsnummer?.let {
                        lagSøkeKriterier(søkefelt = "person.bostedsadresse.vegadresse.bruksenhetsnummer",
                                         søkeord = it)
                    },
                    vegadresse.husbokstav?.let {
                        lagSøkeKriterier(søkefelt = "person.bostedsadresse.vegadresse.husbokstav",
                                         søkeord = it)
                    },
                    vegadresse.husnummer?.let {
                        lagSøkeKriterier(søkefelt = "person.bostedsadresse.vegadresse.husnummer",
                                         søkeord = it)
                    },
                    vegadresse.postnummer?.let {
                        lagSøkeKriterier(søkefelt = "person.bostedsadresse.vegadresse.postnummer",
                                         søkeord = it)
                    }
            )
        }
        return emptyList()
    }

     fun lagSøkeKriterier(søkefelt: String, søkeord: String): SøkeKriterier {
        return SøkeKriterier(fieldName = søkefelt,
                             searchRule = SearchRule(equals = søkeord))
    }
}