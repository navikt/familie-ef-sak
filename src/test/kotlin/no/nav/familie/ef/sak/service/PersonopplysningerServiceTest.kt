package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.familie.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.mapper.AdresseMapper
import no.nav.familie.ef.sak.mapper.PersonopplysningerMapper
import no.nav.familie.ef.sak.mapper.StatsborgerskapMapper
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.KodeverkServiceMock
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonopplysningerServiceTest {

    private val kodeverkService = KodeverkServiceMock().kodeverkService()

    private lateinit var personopplysningerService: PersonopplysningerService
    private lateinit var familieIntegrasjonerClient: FamilieIntegrasjonerClient
    private lateinit var adresseMapper: AdresseMapper
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService
    private lateinit var grunnlagsdataService: GrunnlagsdataService
    private lateinit var søknadService: SøknadService

    @BeforeEach
    internal fun setUp() {
        familieIntegrasjonerClient = mockk()
        adresseMapper = AdresseMapper(kodeverkService)
        arbeidsfordelingService = mockk()
        søknadService = mockk()

        val pdlClient = PdlClientConfig().pdlClient()
        grunnlagsdataService = GrunnlagsdataService(pdlClient, mockk(), søknadService, familieIntegrasjonerClient)
        val personopplysningerMapper =
                PersonopplysningerMapper(adresseMapper,
                                         StatsborgerskapMapper(kodeverkService),
                                         arbeidsfordelingService,
                                         kodeverkService)
        val personService = PersonService(pdlClient)
        personopplysningerService = PersonopplysningerService(personService,
                                                              søknadService,
                                                              familieIntegrasjonerClient,
                                                              grunnlagsdataService,
                                                              personopplysningerMapper)
    }

    @Test
    internal fun `mapper grunnlagsdata til PersonopplysningerDto`() {
        every { familieIntegrasjonerClient.egenAnsatt(any()) } returns true
        every { familieIntegrasjonerClient.hentMedlemskapsinfo(any()) } returns Medlemskapsinfo("01010172272",
                                                                                                emptyList(),
                                                                                                emptyList(),
                                                                                                emptyList())
        every { arbeidsfordelingService.hentNavEnhet(any()) } returns Arbeidsfordelingsenhet("1", "Enhet")
        val søker = personopplysningerService.hentPersonopplysninger("01010172272")
        assertThat(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(søker))
                .isEqualToIgnoringWhitespace("""{
  "personIdent" : "01010172272",
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
    "land" : "Norge",
    "gyldigFraOgMedDato" : "2020-01-01",
    "gyldigTilOgMedDato" : null
  }, {
    "land" : "Sverige",
    "gyldigFraOgMedDato" : "2017-01-01",
    "gyldigTilOgMedDato" : "2020-01-01"
  } ],
  "sivilstand" : [ {
    "type" : "GIFT",
    "gyldigFraOgMed" : "2020-01-01",
    "relatertVedSivilstand" : "11111122222",
    "navn" : "11111122222 mellomnavn Etternavn"
  } ],
  "adresse" : [ {
    "visningsadresse" : "c/o CONAVN, Charlies vei 13 b, 0575 Oslo",
    "type" : "BOSTEDADRESSE",
    "gyldigFraOgMed" : "2020-01-01",
    "gyldigTilOgMed" : "2199-01-01",
    "angittFlyttedato" : "2020-01-02"
  }, {
    "visningsadresse" : "c/o co, Charlies vei 13 b, 0575 Oslo",
    "type" : "KONTAKTADRESSE",
    "gyldigFraOgMed" : "2020-01-01",
    "gyldigTilOgMed" : "2021-01-01",
    "angittFlyttedato" : null
  } ],
  "fullmakt" : [ {
    "gyldigFraOgMed" : "2021-01-01",
    "gyldigTilOgMed" : "2020-01-01",
    "motpartsPersonident" : "11111133333",
    "navn" : "11111133333 mellomnavn Etternavn"
  } ],
  "egenAnsatt" : true,
  "navEnhet" : "1 - Enhet",
  "barn" : [ {
    "personIdent" : "01012067050",
    "navn" : "Barn Barnesen",
    "annenForelder" : {
      "personIdent" : "17097926735",
      "navn" : "Bob  Burger"
    },
    "adresse" : [ {
      "visningsadresse" : "c/o CONAVN, Charlies vei 13 b, 0575 Oslo",
      "type" : "BOSTEDADRESSE",
      "gyldigFraOgMed" : "2020-01-01",
      "gyldigTilOgMed" : "2199-01-01",
      "angittFlyttedato" : "2020-01-02"
    } ],
    "borHosSøker" : true,
    "fødselsdato" : "2018-01-01"
  }, {
    "personIdent" : "13071489536",
    "navn" : "Barn2 Barnesen",
    "annenForelder" : {
     "personIdent" : "17097926735",
      "navn" : "Bob  Burger"
    },
    "adresse" : [ {
      "visningsadresse" : "c/o CONAVN, Charlies vei 13 b, 0575 Oslo",
      "type" : "BOSTEDADRESSE",
      "gyldigFraOgMed" : "2020-01-01",
      "gyldigTilOgMed" : "2199-01-01",
      "angittFlyttedato" : "2020-01-02"
    } ],
    "borHosSøker" : true,
    "fødselsdato" : "2018-01-01"
  } ],
  "innflyttingTilNorge" : [ {
    "fraflyttingsland" : "Sverige",
    "dato" : null,
    "fraflyttingssted" : "Stockholm"
  } ],
  "utflyttingFraNorge" : [ {
    "tilflyttingsland" : "Sverige",
    "dato" : null,
    "tilflyttingssted" : "Stockholm"
  } ],
  "oppholdstillatelse" : [ {
    "oppholdstillatelse" : "PERMANENT",
    "fraDato" : "2020-01-01",
    "tilDato" : null
  } ],
  "lagtTilEtterFerdigstilling" : false
}""")
    }
}