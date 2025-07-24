package no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ef.sak.vilkår.dto.AvstandTilSøkerDto
import no.nav.familie.ef.sak.vilkår.dto.LangAvstandTilSøker.JA
import no.nav.familie.ef.sak.vilkår.dto.LangAvstandTilSøker.JA_UPRESIS
import no.nav.familie.ef.sak.vilkår.dto.LangAvstandTilSøker.UKJENT
import java.lang.Math.abs
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.sqrt

data class PdlResponse<T>(
    val data: T,
    val errors: List<PdlError>?,
    val extensions: PdlExtensions?,
) {
    fun harFeil(): Boolean = errors != null && errors.isNotEmpty()

    fun harAdvarsel(): Boolean = !extensions?.warnings.isNullOrEmpty()

    fun errorMessages(): String = errors?.joinToString { it -> it.message } ?: ""
}

data class PdlBolkResponse<T>(
    val data: PersonBolk<T>?,
    val errors: List<PdlError>?,
    val extensions: PdlExtensions?,
) {
    fun errorMessages(): String = errors?.joinToString { it -> it.message } ?: ""

    fun harAdvarsel(): Boolean = !extensions?.warnings.isNullOrEmpty()
}

data class PdlError(
    val message: String,
    val extensions: PdlErrorExtensions?,
)

data class PdlErrorExtensions(
    val code: String?,
) {
    fun notFound() = code == "not_found"
}

data class PdlExtensions(
    val warnings: List<PdlWarning>?,
)

data class PdlWarning(
    val details: Any?,
    val id: String?,
    val message: String?,
    val query: String?,
)

data class PdlSøkerData(
    val person: PdlSøker?,
)

data class PersonDataBolk<T>(
    val ident: String,
    val code: String,
    val person: T?,
)

data class PersonBolk<T>(
    val personBolk: List<PersonDataBolk<T>>,
)

interface PdlPerson {
    val fødselsdato: List<Fødselsdato>
    val fødested: List<Fødested>
    val bostedsadresse: List<Bostedsadresse>
}

data class PdlIdentBolkResponse(
    val data: IdentBolk?,
    val errors: List<PdlError>?,
) {
    fun errorMessages(): String = errors?.joinToString { it -> it.message } ?: ""
}

data class PdlIdenterBolk(
    val code: String,
    val ident: String,
    val identer: List<PdlIdent>?,
) {
    fun gjeldende(): PdlIdent = this.identer?.first { !it.historisk } ?: PdlIdent(ident, false)
}

data class IdentBolk(
    val hentIdenterBolk: List<PdlIdenterBolk>,
)

data class PdlIdent(
    val ident: String,
    val historisk: Boolean,
)

data class PdlIdenter(
    val identer: List<PdlIdent>,
) {
    fun gjeldende(): PdlIdent = this.identer.first { !it.historisk }
}

data class PdlHentIdenter(
    val hentIdenter: PdlIdenter?,
)

data class PdlPersonKort(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    val navn: List<Navn>,
    @JsonProperty("doedsfall") val dødsfall: List<Dødsfall>,
)

data class PdlSøkerKort(
    @JsonProperty("kjoenn") val kjønn: List<Kjønn>,
    val navn: List<Navn>,
)

data class PdlSøker(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    override val bostedsadresse: List<Bostedsadresse>,
    @JsonProperty("doedsfall") val dødsfall: List<Dødsfall>,
    val forelderBarnRelasjon: List<ForelderBarnRelasjon>,
    val folkeregisteridentifikator: List<Folkeregisteridentifikator>,
    @JsonProperty("foedselsdato") override val fødselsdato: List<Fødselsdato>,
    @JsonProperty("foedested") override val fødested: List<Fødested>,
    val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>,
    val fullmakt: List<Fullmakt>?,
    @JsonProperty("kjoenn") val kjønn: List<Kjønn>,
    val kontaktadresse: List<Kontaktadresse>,
    val navn: List<Navn>,
    val opphold: List<Opphold>,
    val oppholdsadresse: List<Oppholdsadresse>,
    val sivilstand: List<Sivilstand>,
    val statsborgerskap: List<Statsborgerskap>,
    val innflyttingTilNorge: List<InnflyttingTilNorge>,
    val utflyttingFraNorge: List<UtflyttingFraNorge>,
    val vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>,
) : PdlPerson {
    fun alleIdenter(): Set<String> = folkeregisteridentifikator.map { it.ident }.toSet()
}

data class PdlPersonForelderBarn(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    override val bostedsadresse: List<Bostedsadresse>,
    val deltBosted: List<DeltBosted>,
    @JsonProperty("doedsfall") val dødsfall: List<Dødsfall>,
    val forelderBarnRelasjon: List<ForelderBarnRelasjon>,
    @JsonProperty("foedselsdato") override val fødselsdato: List<Fødselsdato>,
    @JsonProperty("foedested") override val fødested: List<Fødested>,
    val navn: List<Navn>,
    val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>,
) : PdlPerson

data class PdlAnnenForelder(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    override val bostedsadresse: List<Bostedsadresse>,
    @JsonProperty("doedsfall") val dødsfall: List<Dødsfall>,
    @JsonProperty("foedselsdato") override val fødselsdato: List<Fødselsdato>,
    @JsonProperty("foedested") override val fødested: List<Fødested>,
    val folkeregisteridentifikator: List<Folkeregisteridentifikator>,
    val navn: List<Navn>,
) : PdlPerson

data class Metadata(
    val historisk: Boolean,
)

data class DeltBosted(
    val startdatoForKontrakt: LocalDate,
    val sluttdatoForKontrakt: LocalDate?,
    val vegadresse: Vegadresse?,
    val ukjentBosted: UkjentBosted?,
    val metadata: Metadata,
)

data class Folkeregistermetadata(
    val gyldighetstidspunkt: LocalDateTime?,
    @JsonProperty("opphoerstidspunkt") val opphørstidspunkt: LocalDateTime?,
)

data class Folkeregisteridentifikator(
    @JsonProperty("identifikasjonsnummer")
    val ident: String,
    val status: FolkeregisteridentifikatorStatus,
    val metadata: Metadata,
)

enum class FolkeregisteridentifikatorStatus {
    I_BRUK,
    OPPHOERT,
}

data class Bostedsadresse(
    val angittFlyttedato: LocalDate?,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val coAdressenavn: String?,
    val utenlandskAdresse: UtenlandskAdresse?,
    val vegadresse: Vegadresse?,
    val ukjentBosted: UkjentBosted?,
    val matrikkeladresse: Matrikkeladresse?,
    val metadata: Metadata,
) {
    val matrikkelId get() = matrikkeladresse?.matrikkelId ?: vegadresse?.matrikkelId

    val bruksenhetsnummer get() = matrikkeladresse?.bruksenhetsnummer ?: vegadresse?.bruksenhetsnummer
}

data class Oppholdsadresse(
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate? = null,
    val coAdressenavn: String?,
    val utenlandskAdresse: UtenlandskAdresse?,
    val vegadresse: Vegadresse?,
    val oppholdAnnetSted: String?,
    val metadata: Metadata,
)

data class Kontaktadresse(
    val coAdressenavn: String?,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val postadresseIFrittFormat: PostadresseIFrittFormat?,
    val postboksadresse: Postboksadresse?,
    val type: KontaktadresseType,
    val utenlandskAdresse: UtenlandskAdresse?,
    val utenlandskAdresseIFrittFormat: UtenlandskAdresseIFrittFormat?,
    val vegadresse: Vegadresse?,
)

@Suppress("unused")
enum class KontaktadresseType {
    @JsonProperty("Innland")
    INNLAND,

    @JsonProperty("Utland")
    UTLAND,
}

data class Postboksadresse(
    val postboks: String,
    val postbokseier: String?,
    val postnummer: String?,
)

data class PostadresseIFrittFormat(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
)

data class Vegadresse(
    val husnummer: String?,
    val husbokstav: String?,
    val bruksenhetsnummer: String?,
    val adressenavn: String?,
    val kommunenummer: String?,
    val tilleggsnavn: String?,
    val postnummer: String?,
    val koordinater: Koordinater?,
    val matrikkelId: Long?,
) {
    companion object {
        /**
         * Norge er delt i tre UTM-soner (32, 33 og 35) - men PDL returnerer ikke hvilken sone et koordinat tilhører
         * Pga dette vil det være vanskelig å sammenligne x-verdi på tvers av soner. Grensen mellom 32 og 33 ligger på ca 7 200 000
         * og alt sør for dette kan avstandsberegnes med både x- og y-koordinater. Nord for dette gjør vi ren avstandsberegning på
         * y-koordinater inntil vi evt får riktig UTM-sone i datagrunnlaget.
         */
        const val UTM_GRENSE = 7_200_000
        const val MINIMUM_AVSTAND_FOR_AUTOMATISK_BEREGNING_I_METER = 200
    }

    fun fjerneBoforhold(annenVegadresse: Vegadresse?): AvstandTilSøkerDto {
        if (this.koordinater == null || annenVegadresse?.koordinater == null) {
            return AvstandTilSøkerDto(avstand = null, langAvstandTilSøker = UKJENT)
        }

        val koordinater1 = this.koordinater
        val koordinater2 = annenVegadresse.koordinater

        if (koordinater1.x == null || koordinater1.y == null || koordinater2.x == null || koordinater2.y == null) {
            return AvstandTilSøkerDto(avstand = null, langAvstandTilSøker = UKJENT)
        }

        if (koordinater1.y > UTM_GRENSE || koordinater2.y > UTM_GRENSE) {
            val distanse = abs(koordinater2.y - koordinater1.y)
            return AvstandTilSøkerDto(
                avstand = distanse.toLong(),
                langAvstandTilSøker = if (distanse > MINIMUM_AVSTAND_FOR_AUTOMATISK_BEREGNING_I_METER) JA_UPRESIS else UKJENT,
            )
        }
        val distanse = beregnAvstandIMeter(koordinater1.x, koordinater1.y, koordinater2.x, koordinater2.y)
        return AvstandTilSøkerDto(
            avstand = distanse.toLong(),
            langAvstandTilSøker = if (distanse > MINIMUM_AVSTAND_FOR_AUTOMATISK_BEREGNING_I_METER) JA else UKJENT,
        )
    }

    private fun beregnAvstandIMeter(
        xKoordinat1: Float,
        yKoordinat1: Float,
        xKoordinat2: Float,
        yKoordinat2: Float,
    ): Float =
        sqrt(
            (xKoordinat1 - xKoordinat2) *
                (xKoordinat1 - xKoordinat2) +
                (yKoordinat1 - yKoordinat2) *
                (yKoordinat1 - yKoordinat2),
        )

    fun erSammeVegadresse(other: Vegadresse): Boolean = påkrevdeFelterErLike(this, other) && likeadresserNullOgTomIgnorert(other, this)

    private fun likeadresserNullOgTomIgnorert(
        other: Vegadresse,
        vegadresse: Vegadresse,
    ) = other.tilVegadresseDto() == vegadresse.tilVegadresseDto()

    private fun påkrevdeFelterErLike(
        vegadresse: Vegadresse,
        other: Vegadresse,
    ): Boolean {
        // påkrevd likhet på noen felt for å sammenligne to vegadresser
        val sammeAdressenavn = vegadresse.adressenavn != null && vegadresse.adressenavn == other.adressenavn
        val sammeHusnummer = vegadresse.husnummer != null && vegadresse.husnummer == other.husnummer
        val sammePostnummer = vegadresse.postnummer != null && vegadresse.postnummer == other.postnummer
        return sammePostnummer && sammeHusnummer && sammeAdressenavn
    }

    private fun Vegadresse.tilVegadresseDto() =
        VegadresseDto(
            adressenavn = adressenavn ?: "",
            husnummer = husnummer ?: "",
            postnummer = postnummer ?: "",
            husbokstav = husbokstav ?: "",
            bruksenhetsnummer = bruksenhetsnummer ?: "",
            kommunenummer = kommunenummer ?: "",
            tilleggsnavn = tilleggsnavn ?: "",
            koordinater = koordinater,
            matrikkelId = matrikkelId,
        )

    private data class VegadresseDto(
        val adressenavn: String,
        val postnummer: String,
        val husnummer: String,
        val husbokstav: String,
        val bruksenhetsnummer: String,
        val kommunenummer: String,
        val tilleggsnavn: String,
        val koordinater: Koordinater?,
        val matrikkelId: Long?,
    )
}

data class UkjentBosted(
    val bostedskommune: String?,
)

data class Koordinater(
    val x: Float?,
    val y: Float?,
    val z: Float?,
    val kvalitet: Int?,
)

data class Adressebeskyttelse(
    val gradering: AdressebeskyttelseGradering,
    val metadata: Metadata,
) {
    fun erStrengtFortrolig(): Boolean =
        this.gradering == AdressebeskyttelseGradering.STRENGT_FORTROLIG ||
            this.gradering == AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
}

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,
}

data class Fødselsdato(
    @JsonProperty("foedselsaar") val fødselsår: Int?,
    @JsonProperty("foedselsdato") val fødselsdato: LocalDate?,
) {
    fun erUnder18År() =
        this.fødselsdato?.let { LocalDate.now() < it.plusYears(18) }
            ?: this.fødselsår?.let { LocalDate.now() < LocalDate.of(it, 1, 1).plusYears(18) }
            ?: true
}

data class Fødested(
    @JsonProperty("foedeland") val fødeland: String?,
    @JsonProperty("foedested") val fødested: String?,
    @JsonProperty("foedekommune") val fødekommune: String?,
)

data class Dødsfall(
    @JsonProperty("doedsdato") val dødsdato: LocalDate?,
)

data class ForelderBarnRelasjon(
    val relatertPersonsIdent: String?,
    val relatertPersonsRolle: Familierelasjonsrolle,
    val minRolleForPerson: Familierelasjonsrolle?,
)

enum class Familierelasjonsrolle {
    BARN,
    MOR,
    FAR,
    MEDMOR,
}

data class Folkeregisterpersonstatus(
    val status: String,
    val forenkletStatus: String,
    val metadata: Metadata,
)

data class Fullmakt(
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate?,
    val motpartsPersonident: String,
    val motpartsRolle: MotpartsRolle,
    val omraader: List<String>,
)

enum class MotpartsRolle {
    FULLMAKTSGIVER,
    FULLMEKTIG,
}

data class Kjønn(
    @JsonProperty("kjoenn") val kjønn: KjønnType,
)

enum class KjønnType {
    KVINNE,
    MANN,
    UKJENT,
}

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val metadata: Metadata,
)

data class Personnavn(
    val etternavn: String,
    val fornavn: String,
    val mellomnavn: String?,
)

data class Tolk(
    @JsonProperty("spraak") val språk: String?,
)

data class Statsborgerskap(
    val land: String,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
)

data class Opphold(
    val type: Oppholdstillatelse,
    val oppholdFra: LocalDate?,
    val oppholdTil: LocalDate?,
)

enum class Oppholdstillatelse {
    MIDLERTIDIG,
    PERMANENT,
    OPPLYSNING_MANGLER,
}

data class Sivilstand(
    val type: Sivilstandstype,
    val gyldigFraOgMed: LocalDate?,
    val relatertVedSivilstand: String?,
    val bekreftelsesdato: LocalDate?,
    val metadata: Metadata,
)

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
    GJENLEVENDE_PARTNER,
}

data class InnflyttingTilNorge(
    val fraflyttingsland: String?,
    val fraflyttingsstedIUtlandet: String?,
    val folkeregistermetadata: Folkeregistermetadata,
)

data class UtflyttingFraNorge(
    val tilflyttingsland: String?,
    val tilflyttingsstedIUtlandet: String?,
    val utflyttingsdato: LocalDate?,
    val folkeregistermetadata: Folkeregistermetadata,
)

data class UtenlandskAdresse(
    val adressenavnNummer: String?,
    val bySted: String?,
    val bygningEtasjeLeilighet: String?,
    val landkode: String,
    val postboksNummerNavn: String?,
    val postkode: String?,
    val regionDistriktOmraade: String?,
)

data class Matrikkeladresse(
    val matrikkelId: Long?,
    val bruksenhetsnummer: String?,
    val tilleggsnavn: String?,
    val postnummer: String?,
)

data class UtenlandskAdresseIFrittFormat(
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val byEllerStedsnavn: String?,
    val landkode: String,
    val postkode: String?,
)

data class VergeEllerFullmektig(
    val motpartsPersonident: String?,
    val navn: Personnavn?,
    val omfang: String?,
    val omfangetErInnenPersonligOmraade: Boolean,
)

data class VergemaalEllerFremtidsfullmakt(
    val embete: String?,
    val folkeregistermetadata: Folkeregistermetadata?,
    val type: String?,
    val vergeEllerFullmektig: VergeEllerFullmektig,
)
