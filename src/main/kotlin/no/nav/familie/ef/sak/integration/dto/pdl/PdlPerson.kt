package no.nav.familie.ef.sak.integration.dto.pdl

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime

data class PdlResponse<T>(val data: T?,
                          val errors: List<PdlError>?) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}

data class PdlError(val message: String)

data class PdlSøker(val person: PdlSøkerData)
data class PdlAnnenForelder(val person: PdlAnnenForelderData)
data class PdlBarn(val person: PdlBarnData)

interface PdlPerson {
    val fødsel: List<Fødsel>
    val bostedsadresse: List<Bostedsadresse>
}

data class PdlSøkerData(val adressebeskyttelse: List<Adressebeskyttelse>,
                        override val bostedsadresse: List<Bostedsadresse>,
                        @JsonProperty("doedsfall") val dødsfall: List<Dødsfall>,
                        val familierelasjoner: List<Familierelasjon>,
                        @JsonProperty("foedsel") override val fødsel: List<Fødsel>,
                        val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>,
                        val navn: List<Navn>,
                        val opphold: List<Opphold>,
                        val oppholdsadresse: List<Oppholdsadresse>,
                        val sivilstand: List<Sivilstand>,
                        val statsborgerskap: List<Statsborgerskap>,
                        val telefonnummer: List<Telefonnummer>,
                        val tilrettelagtKommunikasjon: List<TilrettelagtKommunikasjon>,
                        val innflyttingTilNorge: List<InnflyttingTilNorge>,
                        val utflyttingFraNorge: List<UtflyttingFraNorge>) : PdlPerson

data class PdlBarnData(val adressebeskyttelse: List<Adressebeskyttelse>,
                       override val bostedsadresse: List<Bostedsadresse>,
                       val deltBosted: List<DeltBosted>,
                       @JsonProperty("doedsfall") val dødsfall: List<Dødsfall>,
                       val familierelasjoner: List<Familierelasjon>,
                       @JsonProperty("foedsel") override val fødsel: List<Fødsel>,
                       val navn: List<Navn>) : PdlPerson

data class PdlAnnenForelderData(val adressebeskyttelse: List<Adressebeskyttelse>,
                                override val bostedsadresse: List<Bostedsadresse>,
                                @JsonProperty("doedsfall") val dødsfall: List<Dødsfall>,
                                @JsonProperty("foedsel") override val fødsel: List<Fødsel>,
                                val navn: List<Navn>,
                                val opphold: List<Opphold>,
                                val oppholdsadresse: List<Oppholdsadresse>,
                                val statsborgerskap: List<Statsborgerskap>,
                                val innflyttingTilNorge: List<InnflyttingTilNorge>,
                                val utflyttingFraNorge: List<UtflyttingFraNorge>) : PdlPerson

data class DeltBosted(val startdatoForKontrakt: LocalDateTime,
                      val sluttdatoForKontrakt: LocalDateTime?,
                      val vegadresse: Vegadresse?,
                      val ukjentBosted: UkjentBosted?
)

data class Folkeregistermetadata(val gyldighetstidspunkt: LocalDateTime?,
                                 @JsonProperty("opphoerstidspunkt") val opphørstidspunkt: LocalDateTime?)

data class Bostedsadresse(val angittFlyttedato: LocalDate?,
                          val coAdressenavn: String?,
                          val folkeregistermetadata: Folkeregistermetadata,
                          val vegadresse: Vegadresse?,
                          val ukjentBosted: UkjentBosted?)

data class Oppholdsadresse(val oppholdsadressedato: LocalDate?,
                           val coAdressenavn: String?,
                           val utenlandskAdresse: InternasjonalAdresse?,
                           val vegadresse: Vegadresse?,
                           val oppholdAnnetSted: String?)

data class Vegadresse(val husnummer: String?,
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

data class Fødsel(@JsonProperty("foedselsaar") val fødselsår: Int?,
                  @JsonProperty("foedselsdato") val fødselsdato: LocalDate?,
                  @JsonProperty("foedeland") val fødeland: String?,
                  @JsonProperty("foedested") val fødested: String?,
                  @JsonProperty("foedekommune") val fødekommune: String?)

data class Dødsfall(@JsonProperty("doedsdato") val dødsdato: LocalDate?)

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

data class TilrettelagtKommunikasjon(@JsonProperty("talespraaktolk") val talespråktolk: Tolk?,
                                     @JsonProperty("tegnspraaktolk") val tegnspråktolk: Tolk?)

data class Tolk(@JsonProperty("spraak") val språk: String?)

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
