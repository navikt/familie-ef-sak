package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.infrastruktur.config.KodeverkServiceMock
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataHenterService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.InnflyttingUtflyttingMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.PersonopplysningerMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.StatsborgerskapMapper
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

internal class PersonopplysningerServiceTest {

    private val kodeverkService = KodeverkServiceMock().kodeverkService()
    private val pdlClient = PdlClientConfig().pdlClient()

    private lateinit var personopplysningerService: PersonopplysningerService
    private lateinit var personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient
    private lateinit var adresseMapper: AdresseMapper
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService
    private lateinit var grunnlagsdataService: GrunnlagsdataService
    private lateinit var søknadService: SøknadService

    @BeforeEach
    internal fun setUp() {
        personopplysningerIntegrasjonerClient = mockk(relaxed = true)
        adresseMapper = AdresseMapper(kodeverkService)
        arbeidsfordelingService = mockk(relaxed = true)
        søknadService = mockk()
        val grunnlagsdataHenterService = GrunnlagsdataHenterService(pdlClient, personopplysningerIntegrasjonerClient)

        grunnlagsdataService = GrunnlagsdataService(mockk(), søknadService, grunnlagsdataHenterService)
        val personopplysningerMapper =
                PersonopplysningerMapper(adresseMapper,
                                         StatsborgerskapMapper(kodeverkService),
                                         InnflyttingUtflyttingMapper(kodeverkService),
                                         arbeidsfordelingService)
        val personService = PersonService(pdlClient)
        personopplysningerService = PersonopplysningerService(personService,
                                                              søknadService,
                                                              personopplysningerIntegrasjonerClient,
                                                              grunnlagsdataService,
                                                              personopplysningerMapper,
                                                              ConcurrentMapCacheManager())
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

    @Test
    internal fun `skal cache egenAnsatt når man kaller med samme ident`() {
        personopplysningerService.hentPersonopplysninger("1")
        personopplysningerService.hentPersonopplysninger("1")
        verify(exactly = 1) { personopplysningerIntegrasjonerClient.egenAnsatt(any()) }

        personopplysningerService.hentPersonopplysninger("2")
        verify(exactly = 2) { personopplysningerIntegrasjonerClient.egenAnsatt(any()) }
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource(filnavn)!!.readText()
    }
}