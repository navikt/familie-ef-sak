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
                .isEqualToIgnoringWhitespace(readFile("/json/personopplysningerDto.json"))
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource(filnavn).readText()
    }
}