package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vurdering.medlemskap

import io.mockk.mockk
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.kontrakter.ef.søknad.*
import no.nav.familie.kontrakter.felles.medlemskap.PeriodeInfo
import no.nav.familie.kontrakter.felles.medlemskap.PeriodeStatus
import java.time.LocalDate
import java.time.LocalDateTime

fun pdlPerson(vararg perioder: Pair<LocalDate, LocalDateTime?>) = object : PdlPerson {

    override val fødsel: List<Fødsel> = listOf(Fødsel(null, null, null, null, null, Metadata(false)))

    override val bostedsadresse: List<Bostedsadresse> = perioder.map {
        Bostedsadresse(it.first, null, Folkeregistermetadata(null, it.second), null, null, null, null, Metadata(false))
    }
}

fun pdlSøker(adressebeskyttelse: List<Adressebeskyttelse> = mockk(),
             bostedsadresse: List<Bostedsadresse> = mockk(),
             dødsfall: List<Dødsfall> = mockk(),
             forelderBarnRelasjon: List<ForelderBarnRelasjon> = mockk(),
             fødsel: List<Fødsel> = mockk(),
             folkeregisterpersonstatus: List<Folkeregisterpersonstatus> = mockk(),
             fullmakt: List<Fullmakt> = mockk(),
             kjønn: List<Kjønn> = mockk(),
             kontaktadresse: List<Kontaktadresse> = mockk(),
             navn: List<Navn> = mockk(),
             opphold: List<Opphold> = mockk(),
             oppholdsadresse: List<Oppholdsadresse> = mockk(),
             sivilstand: List<Sivilstand> = mockk(),
             statsborgerskap: List<Statsborgerskap> = mockk(),
             telefonnummer: List<Telefonnummer> = mockk(),
             tilrettelagtKommunikasjon: List<TilrettelagtKommunikasjon> = mockk(),
             innflyttingTilNorge: List<InnflyttingTilNorge> = mockk(),
             utflyttingFraNorge: List<UtflyttingFraNorge> = mockk(),
             vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt> = mockk()) =
        PdlSøker(adressebeskyttelse,
                 bostedsadresse,
                 dødsfall,
                 forelderBarnRelasjon,
                 fødsel,
                 folkeregisterpersonstatus,
                 fullmakt,
                 kjønn,
                 kontaktadresse,
                 navn,
                 opphold,
                 oppholdsadresse,
                 sivilstand,
                 statsborgerskap,
                 telefonnummer,
                 tilrettelagtKommunikasjon,
                 innflyttingTilNorge,
                 utflyttingFraNorge,
                 vergemaalEllerFremtidsfullmakt)

fun pdlBarn(adressebeskyttelse: List<Adressebeskyttelse> = mockk(),
            bostedsadresse: List<Bostedsadresse> = mockk(),
            deltBosted: List<DeltBosted> = mockk(),
            dødsfall: List<Dødsfall> = mockk(),
            forelderBarnRelasjon: List<ForelderBarnRelasjon> = mockk(),
            fødsel: List<Fødsel> = mockk(),
            navn: List<Navn> = mockk()) =
        PdlBarn(adressebeskyttelse,
                bostedsadresse,
                deltBosted,
                dødsfall,
                forelderBarnRelasjon,
                fødsel,
                navn)

fun avvistePerioder(vararg perioder: Pair<LocalDate, LocalDate>) = perioder.map {
    PeriodeInfo(PeriodeStatus.AVST, null, it.first, it.second, true, "", null)
}

fun gyldigePerioder(vararg perioder: Pair<LocalDate, LocalDate>) = perioder.map {
    PeriodeInfo(PeriodeStatus.GYLD, null, it.first, it.second, true, "", null)
}

fun uavklartePerioder(vararg perioder: Pair<LocalDate, LocalDate>) = perioder.map {
    PeriodeInfo(PeriodeStatus.UAVK, null, it.first, it.second, true, "", null)
}

fun søknad(personalia: Søknadsfelt<Personalia> = mockk(),
           innsendingsdetaljer: Søknadsfelt<Innsendingsdetaljer> = mockk(),
           sivilstandsdetaljer: Søknadsfelt<Sivilstandsdetaljer> = mockk(),
           medlemskapsdetaljer: Søknadsfelt<Medlemskapsdetaljer> = mockk(),
           bosituasjon: Søknadsfelt<Bosituasjon> = mockk(),
           sivilstandsplaner: Søknadsfelt<Sivilstandsplaner>? = mockk(),
           barn: Søknadsfelt<List<Barn>> = mockk(),
           aktivitet: Søknadsfelt<Aktivitet> = mockk(),
           situasjon: Søknadsfelt<Situasjon> = mockk(),
           stønadsstart: Søknadsfelt<Stønadsstart> = mockk()) =
        SøknadOvergangsstønad(personalia,
                              innsendingsdetaljer,
                              sivilstandsdetaljer,
                              medlemskapsdetaljer,
                              bosituasjon,
                              sivilstandsplaner,
                              barn,
                              aktivitet,
                              situasjon,
                              stønadsstart)

fun <T> søknadsfelt(verdi: T, svarId: T? = null): Søknadsfelt<T> = Søknadsfelt(label = "label", verdi = verdi, svarId = svarId)
