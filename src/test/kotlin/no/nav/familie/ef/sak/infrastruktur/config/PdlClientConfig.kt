package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ef.sak.infrastruktur.exception.PdlNotFoundException
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlSaksbehandlerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.DeltBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Dødsfall
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisteridentifikator
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.FolkeregisteridentifikatorFraSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.FolkeregisteridentifikatorStatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregistermetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.ForelderBarnRelasjon
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fullmakt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fødested
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Kontaktadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KontaktadresseType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Koordinater
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.MotpartsRolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Opphold
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Oppholdstillatelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonForelderBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonFraSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkResultat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkTreff
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstand
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Statsborgerskap
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Vegadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.VergeEllerFullmektig
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødsel
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødselsdato
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.lagKjønn
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.lagNavn
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.metadataGjeldende
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.pdlBarn
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.pdlSøker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata as PldMetadata

@Configuration
@Profile("mock-pdl")
class PdlClientConfig {
    @Bean
    @Primary
    fun pdlSaksbehandlerClient(): PdlSaksbehandlerClient {
        val pdlSaksbehandlerClient = mockk<PdlSaksbehandlerClient>()
        val pdlPersonFraSøk =
            PdlPersonFraSøk(
                listOf(element = FolkeregisteridentifikatorFraSøk(FNR_PÅ_ADRESSE_SØK)),
                bostedsadresse(),
                listOf(lagNavn()),
            )
        every { pdlSaksbehandlerClient.søkPersonerMedSammeAdresse(any()) } returns
            PersonSøkResultat(listOf(PersonSøkTreff(pdlPersonFraSøk)), 1, 1, 1)
        return pdlSaksbehandlerClient
    }

    @Bean
    @Primary
    fun pdlClient(): PdlClient {
        val pdlClient: PdlClient = mockk()

        every { pdlClient.ping() } just runs

        every { pdlClient.hentPersonKortBolk(any()) } answers {
            firstArg<List<String>>().associate { it to lagPersonKort(it) }
        }

        every { pdlClient.hentSøker(any()) } returns opprettPdlSøker()

        every { pdlClient.hentPersonForelderBarnRelasjon(any()) } returns barn()

        every { pdlClient.hentAndreForeldre(any()) } returns mapOf(ANNEN_FORELDER_FNR to annenForelder())

        val personIdentAktør = slot<String>()
        every { pdlClient.hentAktørIder(capture(personIdentAktør)) } answers {
            if (personIdentAktør.captured == "19117313797") {
                throw PdlNotFoundException()
            } else {
                PdlIdenter(listOf(PdlIdent("12345678901232", false)))
            }
        }

        val personIdent = slot<String>()
        every { pdlClient.hentPersonidenter(capture(personIdent)) } answers {
            if (personIdent.captured == "19117313797") {
                throw PdlNotFoundException()
            } else {
                PdlIdenter(listOf(PdlIdent(firstArg(), false), PdlIdent("98765432109", true)))
            }
        }

        every { pdlClient.hentIdenterBolk(listOf("123", "456")) }
            .returns(
                mapOf(
                    "123" to PdlIdent("ny123", false),
                    "456" to PdlIdent("ny456", false),
                ),
            )

        every { pdlClient.hentIdenterBolk(listOf("456", "123")) }
            .returns(
                mapOf(
                    "123" to PdlIdent("ny123", false),
                    "456" to PdlIdent("ny456", false),
                ),
            )

        every { pdlClient.hentIdenterBolk(listOf("111", "222")) }
            .returns(
                mapOf(
                    "111" to PdlIdent("111", false),
                    "222" to PdlIdent("222", false),
                ),
            )

        every { pdlClient.hentIdenterBolk(listOf("222", "111")) }
            .returns(
                mapOf(
                    "111" to PdlIdent("222", false),
                    "222" to PdlIdent("111", false),
                ),
            )

        return pdlClient
    }

    companion object {
        private val startdato = LocalDate.of(2020, 1, 1)
        private val sluttdato = LocalDate.of(2021, 1, 1)
        const val BARN_FNR = "01012067050"
        const val BARN2_FNR = "14041385481"
        const val SØKER_FNR = "01010172272"
        const val ANNEN_FORELDER_FNR = "17097926735"
        const val FNR_PÅ_ADRESSE_SØK = "01012067050"

        fun lagPersonKort(it: String) =
            PdlPersonKort(
                listOf(
                    Adressebeskyttelse(
                        gradering = AdressebeskyttelseGradering.UGRADERT,
                        metadata = metadataGjeldende,
                    ),
                ),
                listOf(lagNavn(fornavn = it)),
                emptyList(),
            )

        val folkeregisteridentifikatorSøker =
            Folkeregisteridentifikator(
                SØKER_FNR,
                FolkeregisteridentifikatorStatus.I_BRUK,
                metadataGjeldende,
            )

        fun opprettPdlSøker() =
            pdlSøker(
                adressebeskyttelse =
                    listOf(
                        Adressebeskyttelse(
                            gradering = AdressebeskyttelseGradering.UGRADERT,
                            metadata = metadataGjeldende,
                        ),
                    ),
                bostedsadresse = bostedsadresse(),
                dødsfall = listOf(),
                forelderBarnRelasjon = forelderBarnRelasjoner(),
                folkeregisteridentifikator = listOf(folkeregisteridentifikatorSøker),
                fødselsdato = listOf(fødselsdato(2018, LocalDate.of(2018, 1, 1))),
                fødested = listOf(Fødested("Norge", "Oslo", "Oslo")),
                folkeregisterpersonstatus =
                    listOf(
                        Folkeregisterpersonstatus(
                            "bosatt",
                            "bosattEtterFolkeregisterloven",
                            metadataGjeldende,
                        ),
                    ),
                fullmakt = fullmakter(),
                kjønn = lagKjønn(KjønnType.KVINNE),
                kontaktadresse = kontaktadresse(),
                navn = listOf(lagNavn()),
                opphold = listOf(Opphold(Oppholdstillatelse.PERMANENT, startdato, null)),
                oppholdsadresse = listOf(),
                sivilstand = sivilstand(),
                statsborgerskap = statsborgerskap(),
                innflyttingTilNorge = listOf(InnflyttingTilNorge("SWE", "Stockholm", folkeregistermetadata)),
                utflyttingFraNorge =
                    listOf(
                        UtflyttingFraNorge(
                            tilflyttingsland = "SWE",
                            tilflyttingsstedIUtlandet = "Stockholm",
                            utflyttingsdato = LocalDate.of(2021, 1, 1),
                            folkeregistermetadata = folkeregistermetadata,
                        ),
                    ),
                vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt(),
            )

        private val folkeregistermetadata =
            Folkeregistermetadata(
                LocalDateTime.of(2010, Month.AUGUST, 30, 10, 10),
                LocalDateTime.of(2018, Month.JANUARY, 15, 12, 55),
            )

        private fun barn(): Map<String, PdlPersonForelderBarn> =
            mapOf(
                BARN_FNR to
                    pdlBarn(
                        bostedsadresse = bostedsadresse(),
                        deltBosted =
                            listOf(
                                DeltBosted(
                                    LocalDate.of(2023, 1, 1),
                                    LocalDate.of(2053, 1, 1),
                                    null,
                                    null,
                                    PldMetadata(false),
                                ),
                                DeltBosted(
                                    LocalDate.of(2020, 1, 1),
                                    LocalDate.of(2023, 3, 31),
                                    null,
                                    null,
                                    PldMetadata(true),
                                ),
                            ),
                        forelderBarnRelasjon = familierelasjonerBarn(),
                        fødsel = fødsel(),
                        navn = lagNavn("Barn", null, "Barnesen"),
                    ),
                BARN2_FNR to
                    pdlBarn(
                        bostedsadresse = bostedsadresse(),
                        forelderBarnRelasjon = familierelasjonerBarn(),
                        fødsel = fødsel(),
                        navn = lagNavn("Barn2", null, "Barnesen"),
                    ),
            )

        private fun annenForelder(): PdlAnnenForelder =
            PdlAnnenForelder(
                adressebeskyttelse = emptyList(),
                bostedsadresse = bostedsadresse(Koordinater(x = 598845f, y = 6643333f, z = null, kvalitet = null)),
                dødsfall = listOf(Dødsfall(LocalDate.of(2021, 9, 22))),
                fødselsdato = listOf(fødselsdato(1994, LocalDate.of(1994, 11, 11))),
                fødested = listOf(Fødested(null, null, null)),
                navn = listOf(Navn("Bob", "", "Burger", metadataGjeldende)),
                folkeregisteridentifikator =
                    listOf(
                        Folkeregisteridentifikator(
                            ANNEN_FORELDER_FNR,
                            FolkeregisteridentifikatorStatus.I_BRUK,
                            metadataGjeldende,
                        ),
                    ),
            )

        private fun forelderBarnRelasjoner(): List<ForelderBarnRelasjon> =
            listOf(
                ForelderBarnRelasjon(
                    relatertPersonsIdent = BARN_FNR,
                    relatertPersonsRolle = Familierelasjonsrolle.BARN,
                    minRolleForPerson = Familierelasjonsrolle.MOR,
                ),
                ForelderBarnRelasjon(
                    relatertPersonsIdent = BARN2_FNR,
                    relatertPersonsRolle = Familierelasjonsrolle.BARN,
                    minRolleForPerson = Familierelasjonsrolle.MOR,
                ),
            )

        private fun familierelasjonerBarn(): List<ForelderBarnRelasjon> =
            listOf(
                ForelderBarnRelasjon(
                    relatertPersonsIdent = SØKER_FNR,
                    relatertPersonsRolle = Familierelasjonsrolle.MOR,
                    minRolleForPerson = Familierelasjonsrolle.BARN,
                ),
                ForelderBarnRelasjon(
                    relatertPersonsIdent = ANNEN_FORELDER_FNR,
                    relatertPersonsRolle = Familierelasjonsrolle.FAR,
                    minRolleForPerson = Familierelasjonsrolle.BARN,
                ),
            )

        private fun kontaktadresse(): List<Kontaktadresse> =
            listOf(
                Kontaktadresse(
                    coAdressenavn = "co",
                    gyldigFraOgMed = startdato,
                    gyldigTilOgMed = sluttdato,
                    postadresseIFrittFormat = null,
                    postboksadresse = null,
                    type = KontaktadresseType.INNLAND,
                    utenlandskAdresse = null,
                    utenlandskAdresseIFrittFormat = null,
                    vegadresse = vegadresse(),
                ),
            )

        private fun statsborgerskap(): List<Statsborgerskap> =
            listOf(
                Statsborgerskap(
                    land = "NOR",
                    gyldigFraOgMed = startdato,
                    gyldigTilOgMed = null,
                ),
                Statsborgerskap(
                    land = "SWE",
                    gyldigFraOgMed = startdato.minusYears(3),
                    gyldigTilOgMed = startdato,
                ),
            )

        private fun sivilstand(): List<Sivilstand> =
            listOf(
                Sivilstand(
                    type = Sivilstandstype.GIFT,
                    gyldigFraOgMed = startdato,
                    relatertVedSivilstand = "11111122222",
                    bekreftelsesdato = LocalDate.of(2020, 1, 1),
                    metadata = metadataGjeldende,
                ),
            )

        private fun fullmakter(): List<Fullmakt> =
            listOf(
                Fullmakt(
                    gyldigTilOgMed = startdato,
                    gyldigFraOgMed = sluttdato,
                    motpartsPersonident = "11111133333",
                    motpartsRolle = MotpartsRolle.FULLMEKTIG,
                    omraader = listOf(),
                ),
            )

        val defaultKoordinater = Koordinater(x = 601372f, y = 6629367f, z = null, kvalitet = null)

        private fun bostedsadresse(koordinater: Koordinater = defaultKoordinater): List<Bostedsadresse> =
            listOf(
                Bostedsadresse(
                    angittFlyttedato = startdato.plusDays(1),
                    gyldigFraOgMed = startdato,
                    gyldigTilOgMed = LocalDate.of(2199, 1, 1),
                    utenlandskAdresse = null,
                    coAdressenavn = "CONAVN",
                    vegadresse = vegadresse(koordinater),
                    ukjentBosted = null,
                    matrikkeladresse = null,
                    metadata = metadataGjeldende,
                ),
            )

        private fun vegadresse(koordinater: Koordinater = defaultKoordinater): Vegadresse =
            Vegadresse(
                husnummer = "13",
                husbokstav = "b",
                adressenavn = "Charlies vei",
                kommunenummer = "0301",
                postnummer = "0575",
                bruksenhetsnummer = "",
                tilleggsnavn = null,
                koordinater = koordinater,
                matrikkelId = 0,
            )

        private fun vergemaalEllerFremtidsfullmakt(): List<VergemaalEllerFremtidsfullmakt> =
            listOf(
                VergemaalEllerFremtidsfullmakt(
                    embete = null,
                    folkeregistermetadata = null,
                    type = "voksen",
                    vergeEllerFullmektig =
                        VergeEllerFullmektig(
                            motpartsPersonident = ANNEN_FORELDER_FNR,
                            navn = null,
                            omfang = "personligeOgOekonomiskeInteresser",
                            omfangetErInnenPersonligOmraade = false,
                        ),
                ),
                VergemaalEllerFremtidsfullmakt(
                    embete = null,
                    folkeregistermetadata = null,
                    type = "stadfestetFremtidsfullmakt",
                    vergeEllerFullmektig =
                        VergeEllerFullmektig(
                            motpartsPersonident = ANNEN_FORELDER_FNR,
                            navn = null,
                            omfang = "personligeOgOekonomiskeInteresser",
                            omfangetErInnenPersonligOmraade = false,
                        ),
                ),
            )
    }
}
