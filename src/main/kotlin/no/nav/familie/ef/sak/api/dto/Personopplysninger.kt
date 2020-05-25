package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate

/*
Målform Målform skal mastres videre i KRR. Konsumenter som vil ha målform, må derfor gå direkte mot KRR for å få ut opplysningene. I en overgangsfase vil opplysningene fortsatt være tilgjengelig fra TPS.
NAV-enhet
 */
data class PersonopplysningerDto(
        val telefonnummer: TelefonnummerDto,
        val personstatus: Folkeregisterpersonstatus,
        val statsborgerskap: List<StatsborgerskapDto>,
        val sivilstand: List<SivilstandDto>,
        //val adresse: List<AdresseDto>,
        val fullmakt: List<FullmaktDto>
)

data class TelefonnummerDto(
        val landskode: String,
        val nummer: String
)

data class StatsborgerskapDto(
        val land: String,
        val gyldigFraOgMed: LocalDate?,
        val gyldigTilOgMed: LocalDate?
)

data class SivilstandDto(
        val type: Sivilstandstype,
        val gyldigFraOgMed: LocalDate?,
        val kommune: String?, //TODO finnes denne? Finner ikke den i online-doc
        val relatertVedSivilstand: String?
//Bekreftelsesdato ?
)

@Suppress("unused")
enum class Sivilstandstype { //TODO

    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT
}

data class AdresseDto(
    val visningsadresse: String,
    val type: AdresseType,
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate
)

enum class AdresseType {
    BOSTEDADRESSE,
    POSTADRESSE
}

data class FullmaktDto(
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
        val motpartsPersonident: String,
        val motpartsRolle: MotpartsRolle
)

enum class MotpartsRolle {
    FULLMAKTSGIVER,
    FULLMEKTIG
}

@Suppress("unused")
enum class Adressebeskyttelse {

    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT
}

@Suppress("unused")
enum class Folkeregisterpersonstatus(private val pdlKode: String) {

    BOSATT("bosatt"),
    UTFLYTTET("utflyttet"),
    FORSVUNNET("forsvunnet"),
    DØD("doed"),
    OPPHØRT("opphoert"),
    FØDSELSREGISTRERT("foedselsregistrert"),
    MIDLERTIDIG("midlertidig"),
    INAKTIV("inaktiv"),
    UKJENT("ukjent");

    companion object {
        private val map = values().associateBy(Folkeregisterpersonstatus::pdlKode)
        fun fraPDLKode(pdlKode: String) = map.getOrDefault(pdlKode, UKJENT)
    }
}

@Suppress("unused")
enum class Kjønn {

    KVINNE,
    MANN,
    UKJENT
}

data class NavnDto(val fornavn: String,
                   val mellomnavn: String?,
                   val etternavn: String) {

    @Suppress("unused") val visningsnavn: String = when (mellomnavn) {
        null -> "$fornavn $etternavn"
        else -> "$fornavn $mellomnavn $etternavn"
    }
}