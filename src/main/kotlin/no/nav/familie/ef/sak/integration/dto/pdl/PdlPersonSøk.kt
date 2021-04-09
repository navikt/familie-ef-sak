package no.nav.familie.ef.sak.integration.dto.pdl

data class PersonSøkResultat(
        val hits: List<PersonSøkTreff>,
        val totalHits: Int,
        val pageNumber: Int,
        val totalPages: Int
)

data class PersonSøkTreff(val person: PdlPersonFraSøk)

data class PdlPersonFraSøk(
        val bostedsadresse: List<Bostedsadresse>,
        val navn: List<Navn>
)

data class PersonSøk(val sokPerson: PersonSøkResultat)