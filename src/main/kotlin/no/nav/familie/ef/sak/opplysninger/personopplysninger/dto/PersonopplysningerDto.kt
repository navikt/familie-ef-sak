package no.nav.familie.ef.sak.opplysninger.personopplysninger.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.vilkår.dto.StatsborgerskapDto
import java.time.LocalDate
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisterpersonstatus as PdlFolkeregisterpersonstatus

/*
Målform Målform skal mastres videre i KRR. Konsumenter som vil ha målform, må derfor gå direkte mot KRR for å få ut opplysningene.
 I en overgangsfase vil opplysningene fortsatt være tilgjengelig fra TPS.
NAV-enhet
 */
data class PersonopplysningerDto(
    val personIdent: String,
    val navn: NavnDto,
    val kjønn: Kjønn,
    val adressebeskyttelse: Adressebeskyttelse?,
    val folkeregisterpersonstatus: Folkeregisterpersonstatus?,
    val fødselsdato: LocalDate?,
    val dødsdato: LocalDate?,
    val statsborgerskap: List<StatsborgerskapDto>,
    val sivilstand: List<SivilstandDto>,
    val adresse: List<AdresseDto>,
    val fullmakt: List<FullmaktDto>,
    val egenAnsatt: Boolean,
    val barn: List<BarnDto>,
    val innflyttingTilNorge: List<InnflyttingDto>,
    val utflyttingFraNorge: List<UtflyttingDto>,
    val oppholdstillatelse: List<OppholdstillatelseDto>,
    val vergemål: List<VergemålDto>,
)

data class DeltBostedDto(
    val startdatoForKontrakt: LocalDate,
    val sluttdatoForKontrakt: LocalDate?,
    val historisk: Boolean,
)

data class BarnDto(
    val personIdent: String,
    val navn: String,
    val annenForelder: AnnenForelderMinimumDto?,
    val adresse: List<AdresseDto>,
    val borHosSøker: Boolean,
    val deltBosted: List<DeltBostedDto>,
    val harDeltBostedNå: Boolean,
    val fødselsdato: LocalDate?,
    val dødsdato: LocalDate?,
)

data class BarnMinimumDto(
    val personIdent: String,
    val navn: String,
    val fødselsdato: LocalDate?,
)

data class AnnenForelderMinimumDto(
    val personIdent: String,
    val navn: String,
    val dødsdato: LocalDate?,
    val bostedsadresse: String?,
)

data class SivilstandDto(
    val type: Sivilstandstype,
    val gyldigFraOgMed: LocalDate?,
    val relatertVedSivilstand: String?,
    val navn: String?,
    val dødsdato: LocalDate?,
    val erGjeldende: Boolean,
)

@Suppress("unused") // Kopi fra PDL
enum class Sivilstandstype(
    val visningsnavn: String,
) {
    UOPPGITT("Uoppgitt"),
    UGIFT("Ugift"),
    GIFT("Gift"),
    ENKE_ELLER_ENKEMANN("Enke eller enkemann"),
    SKILT("Skilt"),
    SEPARERT("Separert"),
    REGISTRERT_PARTNER("Registrert partner"),
    SEPARERT_PARTNER("Separert partner"),
    SKILT_PARTNER("Skilt partner"),
    GJENLEVENDE_PARTNER("Gjenlevende partner"),
    ;

    fun erGift(): Boolean = this == REGISTRERT_PARTNER || this == GIFT

    fun erUgiftEllerUoppgitt(): Boolean = this == UGIFT || this == UOPPGITT

    fun erSeparert(): Boolean = this == SEPARERT_PARTNER || this == SEPARERT

    fun erEnkeEllerEnkemann(): Boolean = this == ENKE_ELLER_ENKEMANN || this == GJENLEVENDE_PARTNER

    fun erSkilt(): Boolean = this == SKILT || this == SKILT_PARTNER

    fun erUgiftEllerSkilt(): Boolean = this == UGIFT || this == SKILT
}

data class AdresseDto(
    val visningsadresse: String?,
    val type: AdresseType,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val angittFlyttedato: LocalDate? = null,
    val erGjeldende: Boolean = false,
)

enum class AdresseType(
    val rekkefølge: Int,
) {
    BOSTEDADRESSE(1),
    OPPHOLDSADRESSE(2),
    KONTAKTADRESSE(3),
    KONTAKTADRESSE_UTLAND(4),
}

data class FullmaktDto(
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate?,
    val motpartsPersonident: String,
    val navn: String?,
    val områder: List<String>,
)

@Suppress("unused") // Kopi fra PDL
enum class Adressebeskyttelse {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,
}

@Suppress("unused")
enum class Folkeregisterpersonstatus(
    private val pdlStatus: String,
    val visningsnavn: String,
) {
    BOSATT("bosatt", "Bosatt"),
    UTFLYTTET("utflyttet", "Utflyttet"),
    FORSVUNNET("forsvunnet", "Forsvunnet"),
    DØD("doed", "Død"),
    OPPHØRT("opphoert", "Opphørt"),
    FØDSELSREGISTRERT("foedselsregistrert", "Fødselsregistrert"),
    MIDLERTIDIG("midlertidig", "Midlertidig"),
    INAKTIV("inaktiv", "Inaktiv"),
    UKJENT("ukjent", "Ukjent"),
    ;

    companion object {
        private val map = values().associateBy(Folkeregisterpersonstatus::pdlStatus)

        fun fraPdl(status: PdlFolkeregisterpersonstatus) = map.getOrDefault(status.status, UKJENT)
    }
}

@Suppress("unused") // Kopi fra PDL
enum class Kjønn {
    KVINNE,
    MANN,
    UKJENT,
}

data class NavnDto(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val visningsnavn: String,
) {
    companion object {
        fun fraNavn(navn: Navn): NavnDto = NavnDto(navn.fornavn, navn.mellomnavn, navn.etternavn, navn.visningsnavn())
    }
}

data class VergemålDto(
    val embete: String?,
    val type: String?,
    val motpartsPersonident: String?,
    val navn: String?,
    val omfang: String?,
)
