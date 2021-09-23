package no.nav.familie.ef.sak.felles.integration.dto.pdl

import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.DeltBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Dødsfall
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisteridentifikator
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregistermetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.ForelderBarnRelasjon
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fullmakt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Kjønn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Kontaktadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KontaktadresseType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Koordinater
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Matrikkeladresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.MotpartsRolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Opphold
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Oppholdsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Oppholdstillatelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonFraSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøkerData
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøkerKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonBolk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonDataBolk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkResultat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkTreff
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Personnavn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PostadresseIFrittFormat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Postboksadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstand
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Statsborgerskap
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Telefonnummer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.TilrettelagtKommunikasjon
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Tolk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UkjentBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtenlandskAdresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtenlandskAdresseIFrittFormat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Vegadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.VergeEllerFullmektig
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate
import java.time.LocalDateTime

object PdlTestdata {

    private val metadataGjeldende = Metadata(false)

    private val vegadresse = Vegadresse("",
                                        "",
                                        "",
                                        "",
                                        "",
                                        "",
                                        "",
                                        Koordinater(1.0f,
                                                    1.0f,
                                                    1.0f,
                                                    1),
                                        1L)

    private val matrikkeladresse = Matrikkeladresse(1L, "", "", "")
    private val utenlandskAdresse = UtenlandskAdresse("", "", "", "", "", "", "")

    private val folkeregistermetadata = Folkeregistermetadata(LocalDateTime.now(), LocalDateTime.now())

    private val navn = listOf(Navn("", "", "", metadataGjeldende))

    val pdlSøkerKortBolk = PersonBolk(listOf(PersonDataBolk("11111122222", "ok",
                                                            PdlSøkerKort(listOf(Kjønn(KjønnType.KVINNE)), navn))))

    private val adressebeskyttelse = listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, metadataGjeldende))

    private val bostedsadresse = listOf(Bostedsadresse(LocalDate.now().minusDays(10),
                                                       LocalDate.now(),
                                                       LocalDate.now(),
                                                       "",
                                                       utenlandskAdresse,
                                                       vegadresse,
                                                       UkjentBosted(""),
                                                       matrikkeladresse,
                                                       metadataGjeldende))

    private val dødsfall = listOf(Dødsfall(LocalDate.now()))

    private val familierelasjon = listOf(ForelderBarnRelasjon("", Familierelasjonsrolle.BARN, Familierelasjonsrolle.FAR))

    private val fødsel = listOf(Fødsel(1, LocalDate.now(), "", "", "", metadataGjeldende))

    private val opphold = listOf(Opphold(Oppholdstillatelse.MIDLERTIDIG, LocalDate.now(), LocalDate.now()))

    private val oppholdsadresse = listOf(Oppholdsadresse(LocalDate.now(), null, "",
                                                         utenlandskAdresse,
                                                         vegadresse, "", metadataGjeldende))

    private val statsborgerskap = listOf(Statsborgerskap("", LocalDate.now(), LocalDate.now()))

    private val innflyttingTilNorge = listOf(InnflyttingTilNorge("", "", folkeregistermetadata))

    private val utflyttingFraNorge = listOf(UtflyttingFraNorge("", "", folkeregistermetadata))

    val pdlSøkerData =
            PdlSøkerData(PdlSøker(adressebeskyttelse,
                                  bostedsadresse,
                                  dødsfall,
                                  familierelasjon,
                                  fødsel,
                                  listOf(Folkeregisterpersonstatus("", "", metadataGjeldende)),
                                  listOf(Fullmakt(LocalDate.now(),
                                                  LocalDate.now(),
                                                  "",
                                                  MotpartsRolle.FULLMAKTSGIVER,
                                                  listOf(""))),
                                  listOf(Kjønn(KjønnType.KVINNE)),
                                  listOf(Kontaktadresse("",
                                                        LocalDate.now(),
                                                        LocalDate.now(),
                                                        PostadresseIFrittFormat("", "", "", ""),
                                                        Postboksadresse("", "", ""),
                                                        KontaktadresseType.INNLAND,
                                                        utenlandskAdresse,
                                                        UtenlandskAdresseIFrittFormat("", "", "", "", "", ""),
                                                        vegadresse)),
                                  navn,
                                  opphold,
                                  oppholdsadresse,
                                  listOf(Sivilstand(Sivilstandstype.GIFT, LocalDate.now(), "", "", metadataGjeldende)),
                                  statsborgerskap,
                                  listOf(Telefonnummer("", "", 1)),
                                  listOf(TilrettelagtKommunikasjon(Tolk(""), Tolk(""))),
                                  innflyttingTilNorge,
                                  utflyttingFraNorge,
                                  listOf(VergemaalEllerFremtidsfullmakt("",
                                                                        folkeregistermetadata,
                                                                        "",
                                                                        VergeEllerFullmektig("",
                                                                                             Personnavn("", "", ""),
                                                                                             "",
                                                                                             true)))))

    val pdlBarnData =
            PersonBolk(listOf(PersonDataBolk("11111122222", "ok", PdlBarn(adressebeskyttelse,
                                                                          bostedsadresse,
                                                                          listOf(DeltBosted(LocalDate.now(),
                                                                                            LocalDate.now(),
                                                                                            vegadresse,
                                                                                            UkjentBosted(""), metadataGjeldende)),
                                                                          dødsfall,
                                                                          familierelasjon,
                                                                          fødsel,
                                                                          navn))))

    val pdlAnnenForelderData =
            PersonBolk(listOf(PersonDataBolk("11111122222", "ok", PdlAnnenForelder(adressebeskyttelse,
                                                                                   bostedsadresse,
                                                                                   dødsfall,
                                                                                   fødsel,
                                                                                   navn,
                                                                                   opphold,
                                                                                   oppholdsadresse,
                                                                                   statsborgerskap,
                                                                                   innflyttingTilNorge,
                                                                                   utflyttingFraNorge))))

    val pdlPersonKortBolk = PersonBolk(listOf(PersonDataBolk("11111122222", "ok",
                                                             PdlPersonKort(navn,
                                                                           dødsfall))))

    val pdlPersonSøk = PersonSøk(
            PersonSøkResultat(hits = listOf(PersonSøkTreff(person = PdlPersonFraSøk(listOf(Folkeregisteridentifikator("123456789")),bostedsadresse, navn))),
                              totalHits = 1, pageNumber = 1, totalPages = 1))
}
