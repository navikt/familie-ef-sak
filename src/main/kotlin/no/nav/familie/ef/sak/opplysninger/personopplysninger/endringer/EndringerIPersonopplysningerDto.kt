package no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer

import java.time.LocalDateTime

data class EndringerIPersonopplysningerDto(
    val sjekketTidspunkt: LocalDateTime,
    val endringer: Endringer
)

data class Endringer(
    val folkeregisterpersonstatus: Endring<EndringVerdi> = Endring(),
    val fødselsdato: Endring<EndringVerdi> = Endring(),
    val dødsdato: Endring<EndringVerdi> = Endring(),
    val statsborgerskap: Endring<Unit> = Endring(),
    val sivilstand: Endring<Unit> = Endring(),
    val adresse: Endring<Unit> = Endring(),
    val fullmakt: Endring<Unit> = Endring(),
    val barn: Endring<List<Personendring>> = Endring(),
    val annenForelder: Endring<List<Personendring>> = Endring(),
    val innflyttingTilNorge: Endring<Unit> = Endring(),
    val utflyttingFraNorge: Endring<Unit> = Endring(),
    val oppholdstillatelse: Endring<Unit> = Endring(),
    val vergemål: Endring<Unit> = Endring()
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
        annenForelder,
        innflyttingTilNorge,
        utflyttingFraNorge,
        oppholdstillatelse,
        vergemål
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

data class Endring<DETALJER>(
    val harEndringer: Boolean = false,
    val detaljer: DETALJER? = null
)

data class Personendring(
    val ident: String,
    val endringer: List<EndringFelt> = emptyList(),
    val ny: Boolean = false,
    val fjernet: Boolean = false
)

data class EndringFelt(
    val felt: String,
    val tidligere: String,
    val ny: String
)

data class EndringVerdi(
    val tidligere: String,
    val ny: String
)
