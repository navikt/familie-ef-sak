package no.nav.familie.ef.sak.testutil

import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.DeltBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Dødsfall
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.ForelderBarnRelasjon
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fullmakt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Kjønn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Kontaktadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Opphold
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Oppholdsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstand
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Statsborgerskap
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Telefonnummer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.TilrettelagtKommunikasjon
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.kontrakter.ef.søknad.Aktivitet
import no.nav.familie.kontrakter.ef.søknad.Barn
import no.nav.familie.kontrakter.ef.søknad.Bosituasjon
import no.nav.familie.kontrakter.ef.søknad.Innsendingsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Medlemskapsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Personalia
import no.nav.familie.kontrakter.ef.søknad.Situasjon
import no.nav.familie.kontrakter.ef.søknad.Sivilstandsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Sivilstandsplaner
import no.nav.familie.kontrakter.ef.søknad.Stønadsstart
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import java.util.UUID

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


fun søknadsBarnTilBehandlingBarn(barn: Set<SøknadBarn>, behandlingId: UUID = UUID.randomUUID()): List<BehandlingBarn> = barn.map {
    BehandlingBarn(behandlingId = behandlingId,
                   søknadBarnId = it.id,
                   personIdent = it.fødselsnummer,
                   navn = it.navn,
                   fødselTermindato = it.fødselTermindato)
}