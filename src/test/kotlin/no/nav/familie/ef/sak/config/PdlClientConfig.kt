package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.*
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

    private val startdato = LocalDate.of(2020, 1, 1)
    private val sluttdato = LocalDate.of(2021, 1, 1)
    private val barnFnr = "01012067050"
    private val barn2Fnr = "13071489536"
    private val søkerFnr = "01010172272"
    private val annenForelderFnr = "17097926735"

    val metadataGjeldende = Metadata(historisk = false)

    @Bean
    @Primary
    fun pdlClient(): PdlClient {
        val pdlClient: PdlClient = mockk()

        every { pdlClient.ping() } just runs

        every { pdlClient.hentSøkerKortBolk(any()) } answers {
            (firstArg() as List<String>).map { it to PdlSøkerKort(lagKjønn(), lagNavn(fornavn = it)) }.toMap()
        }

        every { pdlClient.hentPersonKortBolk(any()) } answers {
            (firstArg() as List<String>).map { it to PdlPersonKort(lagNavn(fornavn = it)) }.toMap()
        }

        every { pdlClient.hentSøker(any()) } returns
                PdlSøker(adressebeskyttelse = listOf(Adressebeskyttelse(gradering = AdressebeskyttelseGradering.UGRADERT,
                                                                        metadata = metadataGjeldende)),
                         bostedsadresse = bostedsadresse(),
                         dødsfall = listOf(),
                         forelderBarnRelasjon = forelderBarnRelasjoner(),
                         fødsel = listOf(),
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
                         utflyttingFraNorge = listOf(UtflyttingFraNorge("SWE", "Stockholm", folkeregistermetadata)),
                         vergemaalEllerFremtidsfullmakt = listOf()
                )
        every { pdlClient.hentSøkerAsMap(any()) } returns mapOf()

        every { pdlClient.hentBarn(any()) } returns barn()

        every { pdlClient.hentAndreForeldre(any()) } returns mapOf(annenForelderFnr to annenForelder())

        every { pdlClient.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent("12345678901232", false)))

        every { pdlClient.hentPersonidenter(any(), eq(true)) } answers
                { PdlIdenter(listOf(PdlIdent(firstArg(), false), PdlIdent("98765432109", true))) }

        return pdlClient
    }

    private val folkeregistermetadata = Folkeregistermetadata(LocalDateTime.of(2010, Month.AUGUST, 30, 10, 10),
                                                              LocalDateTime.of(2018, Month.JANUARY, 15, 12, 55))


    private fun lagKjønn(kjønnType: KjønnType = KjønnType.KVINNE) = listOf(Kjønn(kjønnType))


    private fun lagNavn(fornavn: String = "Fornavn",
                        mellomnavn: String? = "mellomnavn",
                        etternavn: String = "Etternavn"): List<Navn> {
        return listOf(Navn(fornavn,
                           mellomnavn,
                           etternavn,
                           metadataGjeldende))
    }

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
                            dødsfall = emptyList(),
                            fødsel = fødsel(1994, 11, 1),
                            navn = listOf(Navn("Bob", "", "Burger", metadataGjeldende)),
                            opphold = emptyList(),
                            oppholdsadresse = emptyList(),
                            statsborgerskap = statsborgerskap(),
                            innflyttingTilNorge = emptyList(),
                            utflyttingFraNorge = emptyList(),
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
                              bekreftelsesdato = "2020-01-01",
                              metadata = metadataGjeldende))

    private fun fullmakter(): List<Fullmakt> =
            listOf(Fullmakt(gyldigTilOgMed = startdato,
                            gyldigFraOgMed = sluttdato,
                            motpartsPersonident = "11111133333",
                            motpartsRolle = MotpartsRolle.FULLMEKTIG,
                            omraader = listOf()))

    private fun bostedsadresse(): List<Bostedsadresse> =
            listOf(Bostedsadresse(angittFlyttedato = startdato,
                                  folkeregistermetadata = Folkeregistermetadata(gyldighetstidspunkt = LocalDateTime.now(),
                                                                                opphørstidspunkt = startdato.atStartOfDay()),
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
}