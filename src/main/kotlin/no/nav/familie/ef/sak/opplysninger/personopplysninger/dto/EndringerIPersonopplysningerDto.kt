package no.nav.familie.ef.sak.opplysninger.personopplysninger.dto

import no.nav.familie.ef.sak.vilkår.dto.StatsborgerskapDto
import java.time.LocalDate
import java.time.LocalDateTime

data class EndringerIPersonopplysningerDto(
    val sjekketTidspunkt: LocalDateTime,
    val endringer: Endringer
)

data class Endringer(
    val folkeregisterpersonstatus: Endring<Folkeregisterpersonstatus?>? = null,
    val fødselsdato: Endring<LocalDate?>? = null,
    val dødsdato: Endring<LocalDate?>? = null,
    val statsborgerskap: Endring<List<StatsborgerskapDto>>? = null,
    val sivilstand: Endring<List<SivilstandDto>>? = null,
    val adresse: Endring<List<AdresseDto>>? = null,
    val fullmakt: Endring<List<FullmaktDto>>? = null,
    val egenAnsatt: Endring<Boolean>? = null,
    val barn: Endring<List<BarnDto>>? = null,
    val innflyttingTilNorge: Endring<List<InnflyttingDto>>? = null,
    val utflyttingFraNorge: Endring<List<UtflyttingDto>>? = null,
    val oppholdstillatelse: Endring<List<OppholdstillatelseDto>>? = null,
    val vergemål: Endring<List<VergemålDto>>? = null
) {
    val harEndringer = listOfNotNull(
        folkeregisterpersonstatus,
        fødselsdato,
        dødsdato,
        statsborgerskap,
        sivilstand,
        adresse,
        fullmakt,
        egenAnsatt,
        barn,
        innflyttingTilNorge,
        utflyttingFraNorge,
        oppholdstillatelse,
        vergemål,
    ).isNotEmpty()
}

data class Endring<T>(val tidligere: T, val nye: T)

fun finnEndringer(tidligere: PersonopplysningerDto, nye: PersonopplysningerDto) =
    Endringer(
        folkeregisterpersonstatus = utledEndringer(tidligere.folkeregisterpersonstatus, nye.folkeregisterpersonstatus),
        fødselsdato = utledEndringer(tidligere.fødselsdato, nye.fødselsdato),
        dødsdato = utledEndringer(tidligere.dødsdato, nye.dødsdato),
        statsborgerskap = utledEndringer(tidligere.statsborgerskap, nye.statsborgerskap),
        sivilstand = utledEndringer(tidligere.sivilstand, nye.sivilstand),
        adresse = utledEndringer(tidligere.adresse, nye.adresse),
        fullmakt = utledEndringer(tidligere.fullmakt, nye.fullmakt),
        egenAnsatt = utledEndringer(tidligere.egenAnsatt, nye.egenAnsatt),
        barn = utledEndringer(tidligere.barn, nye.barn),
        // andreForeldre = //
        innflyttingTilNorge = utledEndringer(tidligere.innflyttingTilNorge, nye.innflyttingTilNorge),
        utflyttingFraNorge = utledEndringer(tidligere.utflyttingFraNorge, nye.utflyttingFraNorge),
        oppholdstillatelse = utledEndringer(tidligere.oppholdstillatelse, nye.oppholdstillatelse),
        vergemål = utledEndringer(tidligere.vergemål, nye.vergemål),
    )

fun <T> utledEndringer(tidligere: T, nye: T): Endring<T>? =
    if (tidligere != nye) {
        Endring(tidligere, nye)
    } else {
        null
    }