package no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer

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

    fun felterMedEndringerString(): String {
        return listOf(
            "folkeregisterpersonstatus=${folkeregisterpersonstatus.harEndringer}",
            "fødselsdato=${fødselsdato.harEndringer}",
            "dødsdato=${dødsdato.harEndringer}",
            "statsborgerskap=${statsborgerskap.harEndringer}",
            "sivilstand=${sivilstand.harEndringer}",
            "adresse=${adresse.harEndringer}",
            "fullmakt=${fullmakt.harEndringer}",
            "barn=${barn.harEndringer}",
            "innflyttingTilNorge=${innflyttingTilNorge.harEndringer}",
            "utflyttingFraNorge=${utflyttingFraNorge.harEndringer}",
            "oppholdstillatelse=${oppholdstillatelse.harEndringer}",
            "vergemål=${vergemål.harEndringer}"
        ).joinToString(", ")
    }
}

data class Endring(val harEndringer: Boolean = false)
