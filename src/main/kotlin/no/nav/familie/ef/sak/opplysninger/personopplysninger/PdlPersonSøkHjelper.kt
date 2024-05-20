package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.SearchRuleEquals
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.SearchRuleExists
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.SøkeKriterier

object PdlPersonSøkHjelper {
    fun lagPdlPersonSøkKriterier(bostedsadresse: Bostedsadresse): List<SøkeKriterier> {
        if (bostedsadresse.matrikkeladresse != null) {
            val matrikkeladresse = bostedsadresse.matrikkeladresse
            return listOfNotNull(
                matrikkeladresse.matrikkelId?.let {
                    lagSøkeKriterier(
                        søkefelt = "person.bostedsadresse.matrikkeladresse.matrikkelId",
                        søkeord = it.toString(),
                    )
                },
                matrikkeladresse.bruksenhetsnummer?.let {
                    lagSøkeKriterier(
                        søkefelt = "person.bostedsadresse.matrikkeladresse.bruksenhetsnummer",
                        søkeord = it,
                    )
                },
            )
        } else if (bostedsadresse.vegadresse != null) {
            val vegadresse = bostedsadresse.vegadresse
            return listOfNotNull(
                equalsEllerNotExists(
                    søkefelt = "person.bostedsadresse.vegadresse.adressenavn",
                    søkeord = vegadresse.adressenavn,
                ),
                equalsEllerNotExists(
                    søkefelt = "person.bostedsadresse.vegadresse.bruksenhetsnummer",
                    søkeord = vegadresse.bruksenhetsnummer,
                ),
                equalsEllerNotExists(
                    søkefelt = "person.bostedsadresse.vegadresse.husbokstav",
                    søkeord = vegadresse.husbokstav,
                ),
                equalsEllerNotExists(
                    søkefelt = "person.bostedsadresse.vegadresse.husnummer",
                    søkeord = vegadresse.husnummer,
                ),
                equalsEllerNotExists(
                    søkefelt = "person.bostedsadresse.vegadresse.postnummer",
                    søkeord = vegadresse.postnummer,
                ),
            )
        }
        return emptyList()
    }

    private fun equalsEllerNotExists(
        søkefelt: String,
        søkeord: String?,
    ): SøkeKriterier {
        return søkeord?.let { lagSøkeKriterier(søkefelt, it) }
            ?: lagSøkeKriterier(søkefelt, exists = false)
    }

    private fun lagSøkeKriterier(
        søkefelt: String,
        søkeord: String,
    ): SøkeKriterier {
        return SøkeKriterier(
            fieldName = søkefelt,
            searchRule = SearchRuleEquals(equals = søkeord),
        )
    }

    private fun lagSøkeKriterier(
        søkefelt: String,
        exists: Boolean,
    ): SøkeKriterier {
        return SøkeKriterier(
            fieldName = søkefelt,
            searchRule = SearchRuleExists(exists = exists),
        )
    }
}
