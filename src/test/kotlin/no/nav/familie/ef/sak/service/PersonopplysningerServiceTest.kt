package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.config.KodeverkServiceMock
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRegisterService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.TidligereVedtaksperioderService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.egenansatt.EgenAnsattClient
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

    private lateinit var personopplysningerService: PersonopplysningerService
    private lateinit var personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient
    private lateinit var egenAnsattClient: EgenAnsattClient
    private lateinit var adresseMapper: AdresseMapper
    private lateinit var grunnlagsdataService: GrunnlagsdataService
    private lateinit var søknadService: SøknadService
    private lateinit var behandlingService: BehandlingService
    private lateinit var arbeidsforholdService: ArbeidsforholdService

    private val tidligereVedtaksperioderService = mockk<TidligereVedtaksperioderService>(relaxed = true)

    @BeforeEach
    internal fun setUp() {
        personopplysningerIntegrasjonerClient = mockk(relaxed = true)
        behandlingService = mockk(relaxed = true)
        adresseMapper = AdresseMapper(kodeverkService)
        søknadService = mockk()
        egenAnsattClient = mockk()
        arbeidsforholdService = mockk(relaxed = true)
        val personService = PersonService(PdlClientConfig().pdlClient(), ConcurrentMapCacheManager())
        every { egenAnsattClient.egenAnsatt(any()) } returns true

        val grunnlagsdataRegisterService = GrunnlagsdataRegisterService(
            personService,
            personopplysningerIntegrasjonerClient,
            tidligereVedtaksperioderService,
            arbeidsforholdService,
        )

        grunnlagsdataService = GrunnlagsdataService(
            mockk(),
            søknadService,
            grunnlagsdataRegisterService,
            behandlingService,
            mockk(),
            mockk(),
        )
        val personopplysningerMapper =
            PersonopplysningerMapper(
                adresseMapper,
                StatsborgerskapMapper(kodeverkService),
                InnflyttingUtflyttingMapper(kodeverkService),
            )
        personopplysningerService = PersonopplysningerService(
            personService,
            behandlingService,
            personopplysningerIntegrasjonerClient,
            grunnlagsdataService,
            personopplysningerMapper,
            egenAnsattClient,
            ConcurrentMapCacheManager(),
        )
    }

    @Test
    internal fun `mapper grunnlagsdata til PersonopplysningerDto`() {
        every { personopplysningerIntegrasjonerClient.hentMedlemskapsinfo(any()) } returns Medlemskapsinfo(
            "01010172272",
            emptyList(),
            emptyList(),
            emptyList(),
        )
        val søker = personopplysningerService.hentPersonopplysningerUtenVedtakshistorikk("01010172272")
        assertThat(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(søker))
            .isEqualToIgnoringWhitespace(readFile("/json/personopplysningerDto.json"))
    }

    @Test
    internal fun `skal cache egenAnsatt når man kaller med samme ident`() {
        personopplysningerService.hentPersonopplysningerUtenVedtakshistorikk("1")
        personopplysningerService.hentPersonopplysningerUtenVedtakshistorikk("1")
        verify(exactly = 1) { egenAnsattClient.egenAnsatt(any()) }

        personopplysningerService.hentPersonopplysningerUtenVedtakshistorikk("2")
        verify(exactly = 2) { egenAnsattClient.egenAnsatt(any()) }
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource(filnavn)!!.readText()
    }
}
