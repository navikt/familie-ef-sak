package no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl

import java.time.LocalDate
import java.time.LocalDateTime

object PdlTestdata {
    private val metadataGjeldende = Metadata(false)

    val vegadresse =
        Vegadresse(
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            Koordinater(
                1.0f,
                1.0f,
                1.0f,
                1,
            ),
            1L,
        )

    private val matrikkeladresse = Matrikkeladresse(1L, "", "", "")
    private val utenlandskAdresse = UtenlandskAdresse("", "", "", "", "", "", "")

    private val folkeregistermetadata = Folkeregistermetadata(LocalDateTime.now(), LocalDateTime.now())

    private val navn = listOf(Navn("", "", "", metadataGjeldende))

    private val pdlSøkerKortBolk =
        PersonBolk(
            listOf(
                PersonDataBolk(
                    "11111122222",
                    "ok",
                    PdlSøkerKort(listOf(Kjønn(KjønnType.KVINNE)), navn),
                ),
            ),
        )

    private val adressebeskyttelse =
        listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, metadataGjeldende))

    private val bostedsadresse =
        listOf(
            Bostedsadresse(
                LocalDate.now().minusDays(10),
                LocalDate.now(),
                LocalDate.now(),
                "",
                utenlandskAdresse,
                vegadresse,
                UkjentBosted(""),
                matrikkeladresse,
                metadataGjeldende,
            ),
        )

    private val dødsfall = listOf(Dødsfall(LocalDate.now()))

    private val familierelasjon =
        listOf(ForelderBarnRelasjon("", Familierelasjonsrolle.BARN, Familierelasjonsrolle.FAR))

    private val fødselsdato = listOf(Fødselsdato(1995, LocalDate.now()))
    private val fødested = listOf(Fødested(null, null, null))

    private val opphold = listOf(Opphold(Oppholdstillatelse.MIDLERTIDIG, LocalDate.now(), LocalDate.now()))

    private val oppholdsadresse =
        listOf(
            Oppholdsadresse(
                LocalDate.now(),
                null,
                "",
                utenlandskAdresse,
                vegadresse,
                "",
                metadataGjeldende,
            ),
        )

    private val statsborgerskap = listOf(Statsborgerskap("", LocalDate.now(), LocalDate.now()))

    private val innflyttingTilNorge = listOf(InnflyttingTilNorge("", "", folkeregistermetadata))

    private val utflyttingFraNorge = listOf(UtflyttingFraNorge("", "", LocalDate.now(), folkeregistermetadata))

    val søkerIdentifikator = "1"

    val folkeregisteridentifikatorSøker =
        listOf(
            Folkeregisteridentifikator(
                søkerIdentifikator,
                FolkeregisteridentifikatorStatus.I_BRUK,
                metadataGjeldende,
            ),
        )

    val pdlSøkerData =
        PdlSøkerData(
            PdlSøker(
                adressebeskyttelse,
                bostedsadresse,
                dødsfall,
                familierelasjon,
                folkeregisteridentifikatorSøker,
                fødselsdato,
                fødested,
                listOf(Folkeregisterpersonstatus("", "", metadataGjeldende)),
                listOf(
                    Fullmakt(
                        LocalDate.now(),
                        LocalDate.now(),
                        "",
                        MotpartsRolle.FULLMAKTSGIVER,
                        listOf(""),
                    ),
                ),
                listOf(Kjønn(KjønnType.KVINNE)),
                listOf(
                    Kontaktadresse(
                        "",
                        LocalDate.now(),
                        LocalDate.now(),
                        PostadresseIFrittFormat("", "", "", ""),
                        Postboksadresse("", "", ""),
                        KontaktadresseType.INNLAND,
                        utenlandskAdresse,
                        UtenlandskAdresseIFrittFormat("", "", "", "", "", ""),
                        vegadresse,
                    ),
                ),
                navn,
                opphold,
                oppholdsadresse,
                listOf(
                    Sivilstand(
                        Sivilstandstype.GIFT,
                        LocalDate.now(),
                        "",
                        LocalDate.now(),
                        metadataGjeldende,
                    ),
                ),
                statsborgerskap,
                innflyttingTilNorge,
                utflyttingFraNorge,
                listOf(
                    VergemaalEllerFremtidsfullmakt(
                        "",
                        folkeregistermetadata,
                        "",
                        VergeEllerFullmektig(
                            "",
                            Personnavn("", "", ""),
                            "",
                            true,
                        ),
                    ),
                ),
            ),
        )

    val pdlPersonForelderBarnData =
        PersonBolk(
            listOf(
                PersonDataBolk(
                    "11111122222",
                    "ok",
                    PdlPersonForelderBarn(
                        adressebeskyttelse,
                        bostedsadresse,
                        listOf(
                            DeltBosted(
                                LocalDate.now(),
                                LocalDate.now(),
                                vegadresse,
                                UkjentBosted(""),
                                metadataGjeldende,
                            ),
                        ),
                        dødsfall,
                        familierelasjon,
                        fødselsdato,
                        fødested,
                        navn,
                        listOf(Folkeregisterpersonstatus("bosatt", "", metadataGjeldende)),
                    ),
                ),
            ),
        )

    val ennenForelderIdentifikator = "2"

    val folkeregisteridentifikatorAnnenForelder =
        listOf(
            Folkeregisteridentifikator(
                ennenForelderIdentifikator,
                FolkeregisteridentifikatorStatus.I_BRUK,
                metadataGjeldende,
            ),
        )

    val pdlAnnenForelderData =
        PersonBolk(
            listOf(
                PersonDataBolk(
                    "11111122222",
                    "ok",
                    PdlAnnenForelder(
                        adressebeskyttelse,
                        bostedsadresse,
                        dødsfall,
                        fødselsdato,
                        fødested,
                        folkeregisteridentifikatorAnnenForelder,
                        navn,
                    ),
                ),
            ),
        )

    val pdlPersonKortBolk =
        PersonBolk(
            listOf(
                PersonDataBolk(
                    "11111122222",
                    "ok",
                    PdlPersonKort(
                        adressebeskyttelse,
                        navn,
                        dødsfall,
                    ),
                ),
            ),
        )

    val pdlPersonSøk =
        PersonSøk(
            PersonSøkResultat(
                hits =
                    listOf(
                        PersonSøkTreff(
                            PdlPersonFraSøk(
                                listOf(FolkeregisteridentifikatorFraSøk("123456789")),
                                bostedsadresse,
                                navn,
                            ),
                        ),
                    ),
                totalHits = 1,
                pageNumber = 1,
                totalPages = 1,
            ),
        )
}
