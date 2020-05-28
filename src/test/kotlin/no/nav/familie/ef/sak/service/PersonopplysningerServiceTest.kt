package no.nav.familie.ef.sak.service

import io.mockk.mockk
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonopplysningerServiceTest {

    private lateinit var personopplysningerService: PersonopplysningerService

    @BeforeEach
    internal fun setUp() {
        personopplysningerService = PersonopplysningerService(PersonService(mockk(), PdlClientConfig().pdlClient()))
    }

    @Test
    internal fun `mapper pdlsøker til PersonopplysningerDto`() {
        val søker = personopplysningerService.hentPdlSøker("11111122222")
        assertThat(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(søker))
                .isEqualToIgnoringWhitespace("""{
  "telefonnummer" : {
    "landkode" : "+47",
    "nummer" : "98999923"
  },
  "folkeregisterpersonstatus" : "BOSATT",
  "statsborgerskap" : [ {
    "land" : "NOR",
    "gyldigFraOgMed" : "2020-01-01",
    "gyldigTilOgMed" : "2021-01-01"
  } ],
  "sivilstand" : [ {
    "type" : "SKILT",
    "gyldigFraOgMed" : "2020-01-01",
    "relatertVedSivilstand" : "11111122222",
    "navn" : null
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
    "motpartsPersonident" : "11111122222",
    "navn" : null
  } ]
}""")
    }
}