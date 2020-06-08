package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonopplysningerServiceTest {

    private lateinit var personopplysningerService: PersonopplysningerService
    private lateinit var familieIntegrasjonerClient: FamilieIntegrasjonerClient

    @BeforeEach
    internal fun setUp() {
        familieIntegrasjonerClient = mockk()
        personopplysningerService = PersonopplysningerService(PersonService(mockk(), PdlClientConfig().pdlClient()),
                                                              familieIntegrasjonerClient)
    }

    @Test
    internal fun `mapper pdlsøker til PersonopplysningerDto`() {
        every { familieIntegrasjonerClient.egenAnsatt(any()) } returns true
        val søker = personopplysningerService.hentPersonopplysninger("11111122222")
        assertThat(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(søker))
                .isEqualToIgnoringWhitespace("""{
  "personIdent" : "11111122222",
  "navn" : {
    "fornavn" : "Fornavn",
    "mellomnavn" : "mellomnavn",
    "etternavn" : "Etternavn",
    "visningsnavn" : "Fornavn mellomnavn Etternavn"
  },
  "kjønn" : "KVINNE",
  "adressebeskyttelse" : "UGRADERT",
  "folkeregisterpersonstatus" : "BOSATT",
  "dødsdato" : null,
  "telefonnummer" : {
    "landskode" : "+47",
    "nummer" : "98999923"
  },
  "statsborgerskap" : [ {
    "land" : "NOR",
    "gyldigFraOgMed" : "2020-01-01",
    "gyldigTilOgMed" : "2021-01-01"
  } ],
  "sivilstand" : [ {
    "type" : "SKILT",
    "gyldigFraOgMed" : "2020-01-01",
    "relatertVedSivilstand" : "11111122222",
    "navn" : "Foo mellomnavn Etternavn"
  } ],
  "adresse" : [ {
    "visningsadresse" : "c/o CONAVN, Charlies vei 13 b, 0575 Oslo",
    "type" : "BOSTEDADRESSE",
    "gyldigFraOgMed" : "2020-01-01",
    "gyldigTilOgMed" : "2020-01-01"
  }, {
    "visningsadresse" : "c/o co, Charlies vei 13 b, 0575 Oslo",
    "type" : "KONTAKTADRESSE",
    "gyldigFraOgMed" : "2020-01-01",
    "gyldigTilOgMed" : "2021-01-01"
  } ],
  "fullmakt" : [ {
    "gyldigFraOgMed" : "2021-01-01",
    "gyldigTilOgMed" : "2020-01-01",
    "motpartsPersonident" : "11111133333",
    "navn" : "Bar mellomnavn Etternavn"
  } ],
  "egenAnsatt" : true
}""")
    }
}