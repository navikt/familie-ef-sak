package no.nav.familie.ef.sak.testutil

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
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Kontaktadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
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

object PdlTestdataHelper {

    fun lagKjønn(kjønnType: KjønnType = KjønnType.KVINNE) = listOf(Kjønn(kjønnType))

    fun lagNavn(fornavn: String = "Fornavn",
                mellomnavn: String? = "mellomnavn",
                etternavn: String = "Etternavn",
                historisk: Boolean = false): List<Navn> {
        return listOf(Navn(fornavn,
                           mellomnavn,
                           etternavn,
                           Metadata(historisk = historisk)))
    }

    fun pdlSøker(adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
                 bostedsadresse: List<Bostedsadresse> = emptyList(),
                 dødsfall: List<Dødsfall> = emptyList(),
                 forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
                 fødsel: List<Fødsel> = emptyList(),
                 folkeregisterpersonstatus: List<Folkeregisterpersonstatus> = emptyList(),
                 fullmakt: List<Fullmakt> = emptyList(),
                 kjønn: List<Kjønn> = emptyList(),
                 kontaktadresse: List<Kontaktadresse> = emptyList(),
                 navn: List<Navn> = emptyList(),
                 opphold: List<Opphold> = emptyList(),
                 oppholdsadresse: List<Oppholdsadresse> = emptyList(),
                 sivilstand: List<Sivilstand> = emptyList(),
                 statsborgerskap: List<Statsborgerskap> = emptyList(),
                 telefonnummer: List<Telefonnummer> = emptyList(),
                 tilrettelagtKommunikasjon: List<TilrettelagtKommunikasjon> = emptyList(),
                 innflyttingTilNorge: List<InnflyttingTilNorge> = emptyList(),
                 utflyttingFraNorge: List<UtflyttingFraNorge> = emptyList(),
                 vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt> = emptyList()) =
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

    fun pdlBarn(adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
                bostedsadresse: List<Bostedsadresse> = emptyList(),
                deltBosted: List<DeltBosted> = emptyList(),
                dødsfall: List<Dødsfall> = emptyList(),
                forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
                fødsel: List<Fødsel> = emptyList(),
                navn: List<Navn> = lagNavn()) =
            PdlBarn(adressebeskyttelse,
                    bostedsadresse,
                    deltBosted,
                    dødsfall,
                    forelderBarnRelasjon,
                    fødsel,
                    navn)
}