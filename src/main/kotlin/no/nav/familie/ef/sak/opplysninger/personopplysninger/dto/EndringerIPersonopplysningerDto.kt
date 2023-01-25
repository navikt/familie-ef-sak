package no.nav.familie.ef.sak.opplysninger.personopplysninger.dto

import java.time.LocalDateTime

data class EndringerIPersonopplysningerDto(
    val sjekketTidspunkt: LocalDateTime,
    val endringer: Endringer
)

data class Endringer(
    val folkeregisterpersonstatus: Endring = Endring(),
    val fødselsdato: Endring = Endring(),
    val dødsdato: Endring = Endring(),
    val statsborgerskap: Endring = Endring(),
    val sivilstand: Endring = Endring(),
    val adresse: Endring = Endring(),
    val fullmakt: Endring = Endring(),
    val barn: Endring = Endring(),
    val innflyttingTilNorge: Endring = Endring(),
    val utflyttingFraNorge: Endring = Endring(),
    val oppholdstillatelse: Endring = Endring(),
    val vergemål: Endring = Endring()
) {
    val harEndringer = listOf(
        folkeregisterpersonstatus,
        fødselsdato,
        dødsdato,
        statsborgerskap,
        sivilstand,
        adresse,
        fullmakt,
        barn,
        innflyttingTilNorge,
        utflyttingFraNorge,
        oppholdstillatelse,
        vergemål,
    ).any { it.harEndringer }
}

data class Endring(val harEndringer: Boolean = false)

fun finnEndringer(tidligere: PersonopplysningerDto, nye: PersonopplysningerDto) =
    Endringer(
        folkeregisterpersonstatus = utledEndringer(tidligere.folkeregisterpersonstatus, nye.folkeregisterpersonstatus),
        fødselsdato = utledEndringer(tidligere.fødselsdato, nye.fødselsdato),
        dødsdato = utledEndringer(tidligere.dødsdato, nye.dødsdato),
        statsborgerskap = utledEndringer(tidligere.statsborgerskap, nye.statsborgerskap),
        sivilstand = utledEndringer(tidligere.sivilstand, nye.sivilstand),
        adresse = utledEndringer(tidligere.adresse, nye.adresse),
        fullmakt = utledEndringer(tidligere.fullmakt, nye.fullmakt),
        barn = utledEndringer(tidligere.barn, nye.barn), // TODO bedre diff på barn
        // andreForeldre = // TODO legge til andre forelder og adresse på disse?
        innflyttingTilNorge = utledEndringer(tidligere.innflyttingTilNorge, nye.innflyttingTilNorge),
        utflyttingFraNorge = utledEndringer(tidligere.utflyttingFraNorge, nye.utflyttingFraNorge),
        oppholdstillatelse = utledEndringer(tidligere.oppholdstillatelse, nye.oppholdstillatelse),
        vergemål = utledEndringer(tidligere.vergemål, nye.vergemål),
    )

private fun <T> utledEndringer(tidligere: T, nye: T) =
    Endring(tidligere != nye)
