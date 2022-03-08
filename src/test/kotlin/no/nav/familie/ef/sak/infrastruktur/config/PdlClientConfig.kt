package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlSaksbehandlerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
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
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.MotpartsRolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Opphold
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Oppholdstillatelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonFraSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkResultat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkTreff
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstand
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Statsborgerskap
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Telefonnummer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Vegadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.VergeEllerFullmektig
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.lagKjønn
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.lagNavn
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

@Configuration
@Profile("mock-pdl")
class PdlClientConfig {

    @Bean
    @Primary
    fun pdlSaksbehandlerClient(): PdlSaksbehandlerClient {
        val pdlSaksbehandlerClient = mockk<PdlSaksbehandlerClient>()
        val pdlPersonFraSøk = PdlPersonFraSøk(listOf(element = Folkeregisteridentifikator(fnrPåAdresseSøk)),
                                              bostedsadresse(),
                                              lagNavn())
        every { pdlSaksbehandlerClient.søkPersonerMedSammeAdresse(any()) } returns
                PersonSøkResultat(listOf(PersonSøkTreff(pdlPersonFraSøk)), 1, 1, 1)
        return pdlSaksbehandlerClient
    }

    @Bean
    @Primary
    fun pdlClient(): PdlClient {
        val pdlClient: PdlClient = mockk()

        every { pdlClient.ping() } just runs

        every { pdlClient.hentPersonKortBolk(any()) } answers { firstArg<List<String>>().associate { it to lagPersonKort(it) } }

        every { pdlClient.hentSøker(any()) } returns opprettPdlSøker()

        every { pdlClient.hentBarn(any()) } returns barn()

        every { pdlClient.hentAndreForeldre(any()) } returns mapOf(annenForelderFnr to annenForelder())

        every { pdlClient.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent("12345678901232", false)))

        every { pdlClient.hentPersonidenter(any(), eq(true)) } answers
                { PdlIdenter(listOf(PdlIdent(firstArg(), false), PdlIdent("98765432109", true))) }

        return pdlClient
    }

    companion object {

        private val startdato = LocalDate.of(2020, 1, 1)
        private val sluttdato = LocalDate.of(2021, 1, 1)
        private const val barnFnr = "01012067050"
        private const val barn2Fnr = "14041385481"
        private const val søkerFnr = "01010172272"
        private const val annenForelderFnr = "17097926735"
        private const val fnrPåAdresseSøk = "01012067050"
        private val metadataGjeldende = Metadata(historisk = false)

        fun lagPersonKort(it: String) =
                PdlPersonKort(listOf(Adressebeskyttelse(gradering = AdressebeskyttelseGradering.UGRADERT,
                                                        metadata = metadataGjeldende)),
                              lagNavn(fornavn = it),
                              emptyList())

        fun opprettPdlSøker() =
                PdlSøker(adressebeskyttelse = listOf(Adressebeskyttelse(gradering = AdressebeskyttelseGradering.UGRADERT,
                                                                        metadata = metadataGjeldende)),
                         bostedsadresse = bostedsadresse(),
                         dødsfall = listOf(),
                         forelderBarnRelasjon = forelderBarnRelasjoner(),
                         fødsel = fødsel(),
                         folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("bosatt",
                                                                                      "bosattEtterFolkeregisterloven",
                                                                                      metadataGjeldende)),
                         fullmakt = fullmakter(),
                         kjønn = lagKjønn(KjønnType.KVINNE),
                         kontaktadresse = kontaktadresse(),
                         navn = lagNavn(),
                         opphold = listOf(Opphold(Oppholdstillatelse.PERMANENT, startdato, null)),
                         oppholdsadresse = listOf(),
                         sivilstand = sivilstand(),
                         statsborgerskap = statsborgerskap(),
                         telefonnummer = listOf(Telefonnummer(landskode = "+47", nummer = "98999923", prioritet = 1)),
                         tilrettelagtKommunikasjon = listOf(),
                         innflyttingTilNorge = listOf(InnflyttingTilNorge("SWE", "Stockholm", folkeregistermetadata)),
                         utflyttingFraNorge = listOf(UtflyttingFraNorge(tilflyttingsland = "SWE",
                                                                        tilflyttingsstedIUtlandet = "Stockholm",
                                                                        utflyttingsdato = LocalDate.of(2021, 1, 1),
                                                                        folkeregistermetadata = folkeregistermetadata)),
                         vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt()
                )

        private val folkeregistermetadata = Folkeregistermetadata(LocalDateTime.of(2010, Month.AUGUST, 30, 10, 10),
                                                                  LocalDateTime.of(2018, Month.JANUARY, 15, 12, 55))

        private fun barn(): Map<String, PdlBarn> =
                mapOf(barnFnr to PdlBarn(adressebeskyttelse = listOf(),
                                         bostedsadresse = bostedsadresse(),
                                         deltBosted = listOf(),
                                         dødsfall = listOf(),
                                         forelderBarnRelasjon = familierelasjonerBarn(),
                                         fødsel = fødsel(),
                                         navn = lagNavn("Barn", null, "Barnesen")),
                      barn2Fnr to PdlBarn(adressebeskyttelse = listOf(),
                                          bostedsadresse = bostedsadresse(),
                                          deltBosted = listOf(),
                                          dødsfall = listOf(),
                                          forelderBarnRelasjon = familierelasjonerBarn(),
                                          fødsel = fødsel(),
                                          navn = lagNavn("Barn2", null, "Barnesen")))

        private fun fødsel(år: Int = 2018, måned: Int = 1, dag: Int = 1): List<Fødsel> =
                listOf(Fødsel(fødselsår = år,
                              fødselsdato = LocalDate.of(år, måned, dag),
                              metadata = metadataGjeldende,
                              fødested = null,
                              fødekommune = null,
                              fødeland = null))

        private fun annenForelder(): PdlAnnenForelder =
                PdlAnnenForelder(
                        adressebeskyttelse = emptyList(),
                        bostedsadresse = bostedsadresse(),
                        dødsfall = listOf(Dødsfall(LocalDate.of(2021, 9, 22))),
                        fødsel = fødsel(1994, 11, 1),
                        navn = listOf(Navn("Bob", "", "Burger", metadataGjeldende)),
                )

        private fun forelderBarnRelasjoner(): List<ForelderBarnRelasjon> =
                listOf(ForelderBarnRelasjon(relatertPersonsIdent = barnFnr,
                                            relatertPersonsRolle = Familierelasjonsrolle.BARN,
                                            minRolleForPerson = Familierelasjonsrolle.MOR),
                       ForelderBarnRelasjon(relatertPersonsIdent = barn2Fnr,
                                            relatertPersonsRolle = Familierelasjonsrolle.BARN,
                                            minRolleForPerson = Familierelasjonsrolle.MOR))

        private fun familierelasjonerBarn(): List<ForelderBarnRelasjon> =
                listOf(ForelderBarnRelasjon(relatertPersonsIdent = søkerFnr,
                                            relatertPersonsRolle = Familierelasjonsrolle.MOR,
                                            minRolleForPerson = Familierelasjonsrolle.BARN),
                       ForelderBarnRelasjon(relatertPersonsIdent = annenForelderFnr,
                                            relatertPersonsRolle = Familierelasjonsrolle.FAR,
                                            minRolleForPerson = Familierelasjonsrolle.BARN))


        private fun kontaktadresse(): List<Kontaktadresse> =
                listOf(Kontaktadresse(coAdressenavn = "co",
                                      gyldigFraOgMed = startdato,
                                      gyldigTilOgMed = sluttdato,
                                      postadresseIFrittFormat = null,
                                      postboksadresse = null,
                                      type = KontaktadresseType.INNLAND,
                                      utenlandskAdresse = null,
                                      utenlandskAdresseIFrittFormat = null,
                                      vegadresse = vegadresse()))

        private fun statsborgerskap(): List<Statsborgerskap> =
                listOf(Statsborgerskap(land = "NOR",
                                       gyldigFraOgMed = startdato,
                                       gyldigTilOgMed = null),
                       Statsborgerskap(land = "SWE",
                                       gyldigFraOgMed = startdato.minusYears(3),
                                       gyldigTilOgMed = startdato))

        private fun sivilstand(): List<Sivilstand> =
                listOf(Sivilstand(type = Sivilstandstype.GIFT,
                                  gyldigFraOgMed = startdato,
                                  relatertVedSivilstand = "11111122222",
                                  bekreftelsesdato = LocalDate.of(2020, 1, 1),
                                  metadata = metadataGjeldende))

        private fun fullmakter(): List<Fullmakt> =
                listOf(Fullmakt(gyldigTilOgMed = startdato,
                                gyldigFraOgMed = sluttdato,
                                motpartsPersonident = "11111133333",
                                motpartsRolle = MotpartsRolle.FULLMEKTIG,
                                omraader = listOf()))

        private fun bostedsadresse(): List<Bostedsadresse> =
                listOf(Bostedsadresse(angittFlyttedato = startdato.plusDays(1),
                                      gyldigFraOgMed = startdato,
                                      gyldigTilOgMed = LocalDate.of(2199, 1, 1),
                                      utenlandskAdresse = null,
                                      coAdressenavn = "CONAVN",
                                      vegadresse = vegadresse(),
                                      ukjentBosted = null,
                                      matrikkeladresse = null,
                                      metadata = metadataGjeldende
                ))

        private fun vegadresse(): Vegadresse =
                Vegadresse(husnummer = "13",
                           husbokstav = "b",
                           adressenavn = "Charlies vei",
                           kommunenummer = "0301",
                           postnummer = "0575",
                           bruksenhetsnummer = "",
                           tilleggsnavn = null,
                           koordinater = null,
                           matrikkelId = 0)

        private fun vergemaalEllerFremtidsfullmakt(): List<VergemaalEllerFremtidsfullmakt> {
            return listOf(
                    VergemaalEllerFremtidsfullmakt(embete = null,
                                                   folkeregistermetadata = null,
                                                   type = "voksen",
                                                   vergeEllerFullmektig =
                                                   VergeEllerFullmektig(motpartsPersonident = annenForelderFnr,
                                                                        navn = null,
                                                                        omfang = "personligeOgOekonomiskeInteresser",
                                                                        omfangetErInnenPersonligOmraade = false)),
                    VergemaalEllerFremtidsfullmakt(embete = null,
                                                   folkeregistermetadata = null,
                                                   type = "stadfestetFremtidsfullmakt",
                                                   vergeEllerFullmektig =
                                                   VergeEllerFullmektig(motpartsPersonident = annenForelderFnr,
                                                                        navn = null,
                                                                        omfang = "personligeOgOekonomiskeInteresser",
                                                                        omfangetErInnenPersonligOmraade = false)))
        }
    }
}