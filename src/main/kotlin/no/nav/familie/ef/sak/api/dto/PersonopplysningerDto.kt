package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.integration.dto.pdl.Navn
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import java.time.LocalDate
import no.nav.familie.ef.sak.integration.dto.pdl.Folkeregisterpersonstatus as PdlFolkeregisterpersonstatus

/*
Målform Målform skal mastres videre i KRR. Konsumenter som vil ha målform, må derfor gå direkte mot KRR for å få ut opplysningene.
 I en overgangsfase vil opplysningene fortsatt være tilgjengelig fra TPS.
NAV-enhet
 */
data class PersonopplysningerDto(val personIdent: String,
                                 val navn: NavnDto,
                                 val kjønn: Kjønn,
                                 val adressebeskyttelse: Adressebeskyttelse?,
                                 val folkeregisterpersonstatus: Folkeregisterpersonstatus?,
                                 val dødsdato: LocalDate?,
                                 val telefonnummer: TelefonnummerDto?,
                                 val statsborgerskap: List<StatsborgerskapDto>,
                                 val sivilstand: List<SivilstandDto>,
                                 val adresse: List<AdresseDto>,
                                 val fullmakt: List<FullmaktDto>,
                                 val egenAnsatt: Boolean,
                                 val navEnhet: String,
                                 val barn: List<BarnDto>,
                                 val innflyttingTilNorge: List<InnflyttingDto>,
                                 val utflyttingFraNorge: List<UtflyttingDto>,
                                 val oppholdstillatelse: List<OppholdstillatelseDto>,
                                 val lagtTilEtterFerdigstilling: Boolean)

data class BarnDto(val personIdent: String,
                   val navn: String,
                   val annenForelder: AnnenForelderMinimumDto?,
                   val adresse: List<AdresseDto>,
                   val borHosSøker: Boolean,
                   val fødselsdato: LocalDate?)

data class AnnenForelderMinimumDto(val personIdent: String,
                                   val navn: String)

data class TelefonnummerDto(val landskode: String,
                            val nummer: String)

data class SivilstandDto(val type: Sivilstandstype,
                         val gyldigFraOgMed: String?,
                         val relatertVedSivilstand: String?,
                         val navn: String?,
                         val dødsdato: LocalDate?)

@Suppress("unused") //Kopi fra PDL
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
    GJENLEVENDE_PARTNER;

    fun erGift(): Boolean = this == REGISTRERT_PARTNER || this == GIFT;
    fun erUgiftEllerUoppgitt(): Boolean = this == UGIFT || this == UOPPGITT;
    fun erSeparert(): Boolean = this == SEPARERT_PARTNER || this == SEPARERT;
    fun erEnkeEllerEnkemann(): Boolean = this == ENKE_ELLER_ENKEMANN || this == GJENLEVENDE_PARTNER;
    fun erSkilt(): Boolean = this == SKILT || this == SKILT_PARTNER;

}

data class AdresseDto(val visningsadresse: String?,
                      val type: AdresseType,
                      val gyldigFraOgMed: LocalDate?,
                      val gyldigTilOgMed: LocalDate?,
                      val angittFlyttedato: LocalDate? = null)

enum class AdresseType {
    BOSTEDADRESSE,
    KONTAKTADRESSE,
    KONTAKTADRESSE_UTLAND,
    OPPHOLDSADRESSE
}

data class FullmaktDto(val gyldigFraOgMed: LocalDate,
                       val gyldigTilOgMed: LocalDate,
                       val motpartsPersonident: String,
                       val navn: String?)


@Suppress("unused") //Kopi fra PDL
enum class Adressebeskyttelse {

    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT
}

@Suppress("unused")
enum class Folkeregisterpersonstatus(private val pdlStatus: String) {

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

        private val map = values().associateBy(Folkeregisterpersonstatus::pdlStatus)
        fun fraPdl(status: PdlFolkeregisterpersonstatus) = map.getOrDefault(status.status, UKJENT)
    }
}

@Suppress("unused") //Kopi fra PDL
enum class Kjønn {

    KVINNE,
    MANN,
    UKJENT
}

data class NavnDto(val fornavn: String,
                   val mellomnavn: String?,
                   val etternavn: String,
                   val visningsnavn: String) {

    companion object {

        fun fraNavn(navn: Navn): NavnDto = NavnDto(navn.fornavn, navn.mellomnavn, navn.etternavn, navn.visningsnavn())
    }
}