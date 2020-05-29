package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.time.LocalDateTime

@Configuration
@Profile("mock-pdl")
class PdlClientConfig {

    private val startdato = LocalDate.of(2020, 1, 1)
    private val sluttdato = LocalDate.of(2021, 1, 1)

    @Bean
    @Primary
    fun pdlClient(): PdlClient {
        val pdlCLient: PdlClient = mockk()

        val folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("bosatt", "bosattEtterFolkeregisterloven"))
        val navn = listOf(Navn("Fornavn",
                               "mellomnavn",
                               "Etternavn",
                               Metadata(endringer = listOf(MetadataEndringer(LocalDate.now())))))
        val kjønn = listOf(Kjønn(KjønnType.KVINNE))
        val adressebeskyttelse = listOf(Adressebeskyttelse(gradering = AdressebeskyttelseGradering.UGRADERT))

        every { pdlCLient.hentSøkerKort(any()) } returns
                PdlSøkerKort(adressebeskyttelse = adressebeskyttelse,
                             dødsfall = emptyList(),
                             folkeregisterpersonstatus = folkeregisterpersonstatus,
                             kjønn = kjønn,
                             navn = navn)

        every { pdlCLient.hentSøker(any()) } returns
                PdlSøker(
                        adressebeskyttelse = adressebeskyttelse,
                        bostedsadresse = bostedsadresse(),
                        dødsfall = listOf(),
                        familierelasjoner = listOf(),
                        fødsel = listOf(),
                        folkeregisterpersonstatus = folkeregisterpersonstatus,
                        fullmakt = fullmakter(),
                        kjønn = kjønn,
                        kontaktadresse = kontaktadresse(),
                        navn = navn,
                        opphold = listOf(),
                        oppholdsadresse = listOf(),
                        sivilstand = sivilstand(),
                        statsborgerskap = statsborgerskap(),
                        telefonnummer = listOf(Telefonnummer(landskode = "+47", nummer = "98999923", prioritet = 1)),
                        tilrettelagtKommunikasjon = listOf(),
                        innflyttingTilNorge = listOf(),
                        utflyttingFraNorge = listOf(),
                        vergemaalEllerFremtidsfullmakt = listOf()
                )
        return pdlCLient
    }

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
                                   gyldigTilOgMed = sluttdato))

    private fun sivilstand(): List<Sivilstand> =
            listOf(Sivilstand(type = Sivilstandstype.SKILT,
                              gyldigFraOgMed = startdato,
                              myndighet = "Myndighet",
                              kommune = "0301",
                              sted = "Oslo",
                              utland = null,
                              relatertVedSivilstand = "11111122222",
                              bekreftelsesdato = "2020-01-01"))

    private fun fullmakter(): List<Fullmakt> =
            listOf(Fullmakt(gyldigTilOgMed = startdato,
                            gyldigFraOgMed = sluttdato,
                            motpartsPersonident = "11111122222",
                            motpartsRolle = MotpartsRolle.FULLMEKTIG,
                            omraader = listOf()))

    private fun bostedsadresse(): List<Bostedsadresse> =
            listOf(Bostedsadresse(angittFlyttedato = startdato,
                                  folkeregistermetadata = Folkeregistermetadata(gyldighetstidspunkt = LocalDateTime.now(),
                                                                                opphørstidspunkt = startdato.atStartOfDay()),
                                  coAdressenavn = "CONAVN",
                                  vegadresse = vegadresse(),
                                  ukjentBosted = null))

    private fun vegadresse(): Vegadresse =
            Vegadresse(husnummer = "13",
                       husbokstav = "b",
                       adressenavn = "Charlies vei",
                       kommunenummer = "0301",
                       postnummer = "0575",
                       bruksenhetsnummer = "",
                       tilleggsnavn = null,
                       koordinater = null)
}