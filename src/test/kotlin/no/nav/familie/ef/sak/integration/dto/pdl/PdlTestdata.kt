package no.nav.familie.ef.sak.no.nav.familie.ef.sak.integration.dto.pdl

import no.nav.familie.ef.sak.integration.dto.pdl.*
import java.time.LocalDate
import java.time.LocalDateTime

object PdlTestdata {

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
                                                    1))

    private val utenlandskAdresse = UtenlandskAdresse("", "", "", "", "", "", "")

    private val folkeregistermetadata = Folkeregistermetadata(LocalDateTime.now(), LocalDateTime.now())

    private val navn = listOf(Navn("", "", "", Metadata(listOf(MetadataEndringer(LocalDate.now())))))

    val pdlSøkerKortData = PdlSøkerKortData(PdlSøkerKort(listOf(Kjønn(KjønnType.KVINNE)),
                                                         navn))

    private val adressebeskyttelse = listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG))

    private val bostedsadresse = listOf(Bostedsadresse(LocalDate.now(),
                                                       "",
                                                       folkeregistermetadata,
                                                       vegadresse,
                                                       UkjentBosted("")))

    private val dødsfall = listOf(Dødsfall(LocalDate.now()))

    private val familierelasjon = listOf(Familierelasjon("", Familierelasjonsrolle.BARN, Familierelasjonsrolle.FAR))

    private val fødsel = listOf(Fødsel(1, LocalDate.now(), "", "", ""))

    private val opphold = listOf(Opphold(Oppholdstillatelse.MIDLERTIDIG, LocalDate.now(), LocalDate.now()))

    private val oppholdsadresse = listOf(Oppholdsadresse(LocalDate.now(), "",
                                                         utenlandskAdresse,
                                                         vegadresse, ""))

    private val statsborgerskap = listOf(Statsborgerskap("", LocalDate.now(), LocalDate.now()))

    private val innflyttingTilNorge = listOf(InnflyttingTilNorge("", ""))

    private val utflyttingFraNorge = listOf(UtflyttingFraNorge("", ""))

    val pdlSøkerData =
            PdlSøkerData(PdlSøker(adressebeskyttelse,
                                  bostedsadresse,
                                  dødsfall,
                                  familierelasjon,
                                  fødsel,
                                  listOf(Folkeregisterpersonstatus("", "")),
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
                                  listOf(Sivilstand(Sivilstandstype.GIFT, LocalDate.now(), "", "", "", "", "", "")),
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

    val pdlBarnData = PdlBarnData(PdlBarn(adressebeskyttelse,
                                          bostedsadresse,
                                          listOf(DeltBosted(LocalDateTime.now(),
                                                            LocalDateTime.now(),
                                                            vegadresse,
                                                            UkjentBosted(""))),
                                          dødsfall,
                                          familierelasjon,
                                          fødsel,
                                          navn))

    val pdlAnnenForelderData = PdlAnnenForelderData(PdlAnnenForelder(adressebeskyttelse,
                                                                     bostedsadresse,
                                                                     dødsfall,
                                                                     fødsel,
                                                                     navn,
                                                                     opphold,
                                                                     oppholdsadresse,
                                                                     statsborgerskap,
                                                                     innflyttingTilNorge,
                                                                     utflyttingFraNorge))
}
