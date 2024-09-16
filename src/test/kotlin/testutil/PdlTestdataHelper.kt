package no.nav.familie.ef.sak.testutil

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.DeltBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Dødsfall
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisteridentifikator
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.FolkeregisteridentifikatorStatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.ForelderBarnRelasjon
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fullmakt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fødested
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fødselsdato
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Kjønn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Kontaktadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Opphold
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Oppholdsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonForelderBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstand
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Statsborgerskap
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UkjentBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate

object PdlTestdataHelper {
    val metadataGjeldende = Metadata(historisk = false)
    val metadataHistorisk = Metadata(historisk = true)

    fun lagKjønn(kjønnType: KjønnType = KjønnType.KVINNE) = Kjønn(kjønnType)

    fun lagNavn(
        fornavn: String = "Fornavn",
        mellomnavn: String? = "mellomnavn",
        etternavn: String = "Etternavn",
        historisk: Boolean = false,
    ): Navn =
        Navn(
            fornavn,
            mellomnavn,
            etternavn,
            Metadata(historisk = historisk),
        )

    fun pdlSøker(
        adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
        bostedsadresse: List<Bostedsadresse> = emptyList(),
        dødsfall: List<Dødsfall> = emptyList(),
        forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
        fødselsdato: List<Fødselsdato> = emptyList(),
        fødested: List<Fødested> = emptyList(),
        folkeregisterpersonstatus: List<Folkeregisterpersonstatus> = emptyList(),
        fullmakt: List<Fullmakt> = emptyList(),
        kjønn: Kjønn? = null,
        kontaktadresse: List<Kontaktadresse> = emptyList(),
        navn: List<Navn> = emptyList(),
        opphold: List<Opphold> = emptyList(),
        oppholdsadresse: List<Oppholdsadresse> = emptyList(),
        sivilstand: List<Sivilstand> = emptyList(),
        statsborgerskap: List<Statsborgerskap> = emptyList(),
        innflyttingTilNorge: List<InnflyttingTilNorge> = emptyList(),
        utflyttingFraNorge: List<UtflyttingFraNorge> = emptyList(),
        vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt> = emptyList(),
        folkeregisteridentifikator: List<Folkeregisteridentifikator> = emptyList(),
    ) =
        PdlSøker(
            adressebeskyttelse,
            bostedsadresse,
            dødsfall,
            forelderBarnRelasjon,
            folkeregisteridentifikator,
            fødselsdato,
            fødested,
            folkeregisterpersonstatus,
            fullmakt,
            listOfNotNull(kjønn),
            kontaktadresse,
            navn,
            opphold,
            oppholdsadresse,
            sivilstand,
            statsborgerskap,
            innflyttingTilNorge,
            utflyttingFraNorge,
            vergemaalEllerFremtidsfullmakt,
        )

    fun pdlBarn(
        adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
        bostedsadresse: List<Bostedsadresse> = emptyList(),
        deltBosted: List<DeltBosted> = emptyList(),
        dødsfall: List<Dødsfall> = emptyList(),
        forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
        fødsel: Fødsel? = null,
        navn: Navn = lagNavn(),
    ) =
        PdlPersonForelderBarn(
            adressebeskyttelse,
            bostedsadresse,
            deltBosted,
            dødsfall,
            forelderBarnRelasjon,
            listOfNotNull(fødselsdato(fødsel?.fødselsår, fødsel?.fødselsdato)),
            listOfNotNull(fødested(fødsel?.fødeland, fødsel?.fødested, fødsel?.fødekommune)),
            listOfNotNull(navn),
            emptyList(),
        )

    fun fødsel(
        år: Int = 2018,
        måned: Int = 1,
        dag: Int = 1,
    ): Fødsel =
        fødsel(LocalDate.of(år, måned, dag))

    fun fødselsdato(
        år: Int? = 2018,
        dato: LocalDate?,
    ): Fødselsdato = Fødselsdato(år, dato)

    fun fødested(
        fødeland: String?,
        fødested: String?,
        fødekommune: String?,
    ): Fødested = Fødested(fødeland, fødested, fødekommune)

    fun fødsel(fødselsdato: LocalDate) =
        Fødsel(
            fødselsår = fødselsdato.year,
            fødselsdato = fødselsdato,
            fødested = null,
            fødekommune = null,
            fødeland = null,
        )

    fun ukjentBostedsadresse(
        bostedskommune: String = "1234",
        historisk: Boolean = false,
    ) =
        Bostedsadresse(
            angittFlyttedato = null,
            gyldigFraOgMed = null,
            gyldigTilOgMed = null,
            coAdressenavn = null,
            utenlandskAdresse = null,
            vegadresse = null,
            ukjentBosted = UkjentBosted(bostedskommune),
            matrikkeladresse = null,
            metadata = Metadata(historisk),
        )

    fun folkeregisteridentifikator(
        ident: String,
        status: FolkeregisteridentifikatorStatus = FolkeregisteridentifikatorStatus.I_BRUK,
        gjeldende: Boolean = true,
    ) = Folkeregisteridentifikator(
        ident,
        status,
        if (gjeldende) metadataGjeldende else metadataHistorisk,
    )
}
