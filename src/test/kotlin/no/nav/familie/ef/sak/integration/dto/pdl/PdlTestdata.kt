package no.nav.familie.ef.sak.no.nav.familie.ef.sak.integration.dto.pdl

import no.nav.familie.ef.sak.integration.dto.pdl.*
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

    private val matrikkeladresse = Matrikkeladresse(1L)
    private val utenlandskAdresse = UtenlandskAdresse("", "", "", "", "", "", "")

    private val folkeregistermetadata = Folkeregistermetadata(LocalDateTime.now(), LocalDateTime.now())

    private val navn = listOf(Navn("", "", "", metadataGjeldende))

    val pdlSøkerKortBolk = PersonBolk(listOf(PersonDataBolk("11111122222", "ok",
                                                            PdlSøkerKort(listOf(Kjønn(KjønnType.KVINNE)), navn))))

    private val adressebeskyttelse = listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, metadataGjeldende))

    private val bostedsadresse = listOf(Bostedsadresse(LocalDate.now(),
                                                       "",
                                                       folkeregistermetadata,
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

    val pdlPersonKortBolk = PersonBolk(listOf(PersonDataBolk("11111122222", "ok", PdlPersonKort(navn))))
}
