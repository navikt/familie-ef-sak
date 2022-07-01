package no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl

data class PersonSøkResultat(
    val hits: List<PersonSøkTreff>,
    val totalHits: Int,
    val pageNumber: Int,
    val totalPages: Int
)

data class PersonSøkTreff(val person: PdlPersonFraSøk)

data class PdlPersonFraSøk(
    val folkeregisteridentifikator: List<FolkeregisteridentifikatorFraSøk>,
    val bostedsadresse: List<Bostedsadresse>,
    val navn: List<Navn>
)

data class FolkeregisteridentifikatorFraSøk(val identifikasjonsnummer: String)

data class PersonSøk(val sokPerson: PersonSøkResultat)
