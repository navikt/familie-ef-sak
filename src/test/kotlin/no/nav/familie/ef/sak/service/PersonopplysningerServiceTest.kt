package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.config.KodeverkServiceMock
import no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.PersonopplysningerMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.StatsborgerskapMapper
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonopplysningerServiceTest {

    private val kodeverkService = KodeverkServiceMock().kodeverkService()

    private lateinit var personopplysningerService: PersonopplysningerService
    private lateinit var personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient
    private lateinit var adresseMapper: AdresseMapper
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService
    private lateinit var grunnlagsdataService: GrunnlagsdataService
    private lateinit var søknadService: SøknadService

    @BeforeEach
    internal fun setUp() {
        personopplysningerIntegrasjonerClient = mockk()
        adresseMapper = AdresseMapper(kodeverkService)
        arbeidsfordelingService = mockk()
        søknadService = mockk()

        val pdlClient = PdlClientConfig().pdlClient()
        grunnlagsdataService = GrunnlagsdataService(pdlClient, mockk(), søknadService, personopplysningerIntegrasjonerClient)
        val personopplysningerMapper =
                PersonopplysningerMapper(adresseMapper,
                                         StatsborgerskapMapper(kodeverkService),
                                         arbeidsfordelingService,
                                         kodeverkService)
        val personService = PersonService(pdlClient)
        personopplysningerService = PersonopplysningerService(personService,
                                                              søknadService,
                                                              personopplysningerIntegrasjonerClient,
                                                              grunnlagsdataService,
                                                              personopplysningerMapper)
    }

    @Test
    internal fun `mapper grunnlagsdata til PersonopplysningerDto`() {
        every { personopplysningerIntegrasjonerClient.egenAnsatt(any()) } returns true
        every { personopplysningerIntegrasjonerClient.hentMedlemskapsinfo(any()) } returns Medlemskapsinfo("01010172272",
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