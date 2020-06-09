package no.nav.familie.ef.sak.integration.dto.pdl

fun List<Navn>.gjeldende(): Navn = this.maxBy { it.metadata.endringer.maxBy(MetadataEndringer::registrert)!!.registrert }!!

fun Navn.visningsnavn(): String {
    return if (mellomnavn == null) "$fornavn $etternavn"
    else "$fornavn $mellomnavn $etternavn"
}

fun Bostedsadresse.tilFormatertAdresse(): String? {
    val adresse = vegadresse?.tilFormatertAdresse() ?: ukjentBosted?.bostedskommune
    return join(coAdresse(coAdressenavn), adresse)
}

fun Oppholdsadresse.tilFormatertAdresse(): String? {
    val adresse = vegadresse?.tilFormatertAdresse() ?: utenlandskAdresse?.tilFormatertAdresse()
    return join(coAdresse(coAdressenavn), adresse)
}

fun Kontaktadresse.tilFormatertAdresse(): String? {
    val adresse = when (type) {
        KontaktadresseType.INNLAND -> when {
            vegadresse != null -> vegadresse.tilFormatertAdresse()
            postboksadresse != null -> postboksadresse.tilFormatertAdresse()
            else -> postadresseIFrittFormat?.tilFormatertAdresse()
        }
        KontaktadresseType.UTLAND -> when {
            utenlandskAdresse != null -> utenlandskAdresse.tilFormatertAdresse()
            else -> utenlandskAdresseIFrittFormat?.tilFormatertAdresse()
        }
    }
    return join(coAdresse(coAdressenavn), adresse)
}

private fun coAdresse(coAdressenavn: String?): String? = coAdressenavn?.let { "c/o $it" }

//må feltet "postboks" ha med "postboks" i strengen? "postboks ${postboks}" ?
private fun Postboksadresse.tilFormatertAdresse(): String? = join(postbokseier, postboks, space(postnummer, poststed(postnummer)))

private fun PostadresseIFrittFormat.tilFormatertAdresse(): String? =
        join(adresselinje1, adresselinje2, adresselinje3, space(postnummer, poststed(postnummer)))

// har ikke med bygningEtasjeLeilighet, postboksNummerNavn
private fun UtenlandskAdresse.tilFormatertAdresse(): String? =
        join(adressenavnNummer,
             space(postkode, bySted),
             regionDistriktOmraade,
             land(landkode))

private fun UtenlandskAdresseIFrittFormat.tilFormatertAdresse(): String? =
        join(adresselinje1,
             adresselinje2,
             adresselinje3,
             space(postkode, byEllerStedsnavn),
             land(landkode))

private fun Vegadresse.tilFormatertAdresse(): String? =
        join(space(adressenavn, husnummer, husbokstav),
             bruksenhetsnummer,
             space(postnummer, poststed(postnummer)))

private fun poststed(postnummer: String?): String? {
    return "Oslo" //TODO
}

private fun land(landkode: String?): String? {
    return "NOR"
}

private fun space(vararg args: String?): String? = join(*args, separator = " ")

private fun join(vararg args: String?, separator: String = ", "): String? {
    val filterNotNull = args.filterNotNull().filterNot(String::isEmpty)
    return if (filterNotNull.isEmpty()) {
        null
    } else filterNotNull.joinToString(separator)
}