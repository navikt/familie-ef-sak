package no.nav.familie.ef.sak.integration.dto.pdl

import java.time.LocalDate
import java.time.LocalDateTime


data class Identliste(
        val identer: List<IdentInformasjon>
)

data class IdentInformasjon(
        val ident: String,
        val gruppe: IdentGruppe,
        val historisk: Boolean
)

enum class IdentGruppe {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID
}

data class PdlHentPersonResponse(val data: PdlPerson?,
                                 val errors: List<PdlError>?) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}

data class PdlError (val message: String)


data class PdlPerson(val person: PdlPersonData?)


data class PdlPersonData(val adressebeskyttelse: List<Adressebeskyttelse>,
                         val bostedsadresse: List<Bostedsadresse>,
                         val deltBosted: List<DeltBosted>,
                         val doedsfall: List<Doedsfall>,
                         val familierelasjoner: List<Familierelasjon>,
                         val foedsel: List<Foedsel>,
                         val folkeregisteridentifikator: List<Folkeregisteridentifikator>,
                         val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>,
                         val navn: List<Navn>,
                         val opphold: List<Opphold>,
                         val oppholdsadresse: List<Oppholdsadresse>,
                         val sivilstand: List<Sivilstand>,
                         val statsborgerskap: List<Statsborgerskap>,
                         val telefonnummer: List<Telefonnummer>,
                         val tilrettelagtKommunikasjon: List<TilrettelagtKommunikasjon>,
                         val innflyttingTilNorge: List<InnflyttingTilNorge>,
                         val utflyttingFraNorge: List<UtflyttingFraNorge>)

data class DeltBosted(val startdatoForKontrakt: LocalDateTime,
                      val sluttdatoForKontrakt: LocalDateTime?,
                      val vegadresse: Vegadresse?,
                      val ukjentBosted: UkjentBosted?
)

data class Bostedsadresse(val angittFlyttedato: LocalDate?,
                          val coAdressenavn: String?,
                          val vegadresse: Vegadresse?,
                          val ukjentBosted: UkjentBosted?)

data class Oppholdsadresse(val oppholdsadressedato: LocalDate?,
                           val coAdressenavn: String?,
                           val utenlandskAdresse: InternasjonalAdresse?,
                           val vegadresse: Vegadresse?,
                           val oppholdAnnetSted: String?)

data class Vegadresse(val matrikkelId: Int?,
                      val husnummer: String?,
                      val husbokstav: String?,
                      val bruksenhetsnummer: String?,
                      val adressenavn: String?,
                      val kommunenummer: String?,
                      val tilleggsnavn: String?,
                      val postnummer: String?,
                      val koordinater: Koordinater?)

data class UkjentBosted(val bostedskommune: String?)

data class InternasjonalAdresse(val adressenavn: String?,
                                val bygningsinformasjon: String?,
                                val postboks: String?,
                                val postkode: String?,
                                val byEllerStedsnavn: String?,
                                val distriktEllerRegion: String?,
                                val landkode: String)

data class Koordinater(val x: Float?,
                       val y: Float?,
                       val z: Float?,
                       val kvalitet: Int?)

data class Adressebeskyttelse(val gradering: AdressebeskyttelseGradering)

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT
}

data class Foedsel(val foedselsaar: Int?,
                   val foedselsdato: LocalDate?,
                   val foedeland: String?,
                   val foedested: String?,
                   val foedekommune: String?)

data class Doedsfall(val doedsdato: LocalDate?)

data class Familierelasjon(val relatertPersonsIdent: String,
                           val relatertPersonsRolle: Familierelasjonsrolle,
                           val minRolleForPerson: Familierelasjonsrolle?)

enum class Familierelasjonsrolle {
    BARN,
    MOR,
    FAR,
    MEDMOR
}

data class Folkeregisterpersonstatus(val status: String,
                                     val forenkletStatus: String)

data class Navn(val fornavn: String,
                val mellomnavn: String?,
                val etternavn: String)

data class Telefonnummer(val landskode: String,
                         val nummer: String,
                         val prioritet: Int)

data class TilrettelagtKommunikasjon(val talespraaktolk: Tolk?,
                                     val tegnspraaktolk: Tolk?)

data class Tolk(val spraak: String?)

data class Folkeregisteridentifikator(val identifikasjonsnummer: String,
                                      val status: String,
                                      val type: String)

data class Statsborgerskap(val land: String,
                           val gyldigFraOgMed: LocalDate?,
                           val gyldigTilOgMed: LocalDate?)

data class Opphold(val type: Oppholdstillatelse,
                   val oppholdFra: LocalDate?,
                   val oppholdTil: LocalDate?)

enum class Oppholdstillatelse {
    MIDLERTIDIG,
    PERMANENT,
    OPPLYSNING_MANGLER
}

data class Sivilstand(val type: Sivilstandstype,
                      val gyldigFraOgMed: LocalDate?,
                      val myndighet: String?,
                      val kommune: String?,
                      val sted: String?,
                      val utland: String?,
                      val relatertVedSivilstand: String?,
                      val bekreftelsesdato: String?)

enum class Sivilstandstype {
    UOPPGITT,
    UGIFT,
    GIFT,
    ENKE_ELLER_ENKEMANN,
    SKILT,
    SEPARERT,
    REGISTRERT_PARTNER,
    SEPARERT_PARTNER,
    SKILT_PARTNER,
    GJENLEVENDE_PARTNER
}

data class InnflyttingTilNorge(val fraflyttingsland: String?,
                               val fraflyttingsstedIUtlandet: String?)

data class UtflyttingFraNorge(val tilflyttingsland: String?,
                              val tilflyttingsstedIUtlandet: String?)
