package no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonopplysningerDto

object UtledEndringerUtil {
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
            vergemål = utledEndringer(tidligere.vergemål, nye.vergemål)
        )

    private fun <T> utledEndringer(tidligere: T, nye: T) =
        Endring(tidligere != nye)
}
