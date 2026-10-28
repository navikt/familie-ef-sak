package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.config.KodeverkServiceMock
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.infrastruktur.exception.PdlRequestException
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.kontantstøtte.HentUtbetalingsinfoKontantstøtteDto
import no.nav.familie.ef.sak.kontantstøtte.KontantstøtteService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRegisterService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.TidligereVedtaksperioderService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.FalskIdentitetDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.egenansatt.EgenAnsattClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt.FullmaktResultat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt.FullmaktService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.InnflyttingUtflyttingMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.PersonopplysningerMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.StatsborgerskapMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.FalskIdentitet
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fullmakt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.MotpartsRolle
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.time.LocalDate
import java.util.function.Supplier

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
    private lateinit var kontantstøtteService: KontantstøtteService
    private lateinit var fullmaktService: FullmaktService

    private val tidligereVedtaksperioderService = mockk<TidligereVedtaksperioderService>(relaxed = true)
    private val pdlClient = PdlClientConfig().pdlClient()
    private val featureToggleService = mockk<FeatureToggleService>()

    @BeforeEach
    internal fun setUp() {
        personopplysningerIntegrasjonerClient = mockk(relaxed = true)
        behandlingService = mockk(relaxed = true)
        adresseMapper = AdresseMapper(kodeverkService)
        søknadService = mockk()
        egenAnsattClient = mockk()
        arbeidsforholdService = mockk(relaxed = true)
        kontantstøtteService = mockk()
        fullmaktService = mockk()
        every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns true

        val personService = PersonService(pdlClient, ConcurrentMapCacheManager())
        every { egenAnsattClient.egenAnsatt(any()) } returns true
        every { kontantstøtteService.hentUtbetalingsinfoKontantstøtte(any()) } returns
            HentUtbetalingsinfoKontantstøtteDto(
                finnesUtbetaling = false,
                emptyList(),
            )
        every { fullmaktService.hentFullmakt(any()) } returns FullmaktResultat(listOf(Fullmakt(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1), "11111133333", MotpartsRolle.FULLMEKTIG, listOf())))
        val grunnlagsdataRegisterService =
            GrunnlagsdataRegisterService(
                personService,
                tidligereVedtaksperioderService,
                arbeidsforholdService,
                kontantstøtteService,
                fullmaktService,
                mockk(relaxed = true),
                featureToggleService,
            )

        grunnlagsdataService =
            GrunnlagsdataService(
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
        personopplysningerService =
            PersonopplysningerService(
                personService,
                behandlingService,
                personopplysningerIntegrasjonerClient,
                grunnlagsdataService,
                personopplysningerMapper,
                egenAnsattClient,
                ConcurrentMapCacheManager(),
                featureToggleService,
            )
    }

    @Test
    internal fun `mapper grunnlagsdata til PersonopplysningerDto`() {
        val søker = personopplysningerService.hentPersonopplysningerFraRegister("01010172272")
        assertThat(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(søker))
            .isEqualToIgnoringWhitespace(readFile("/json/personopplysningerDto.json"))
    }

    @Test
    internal fun `skal cache egenAnsatt når man kaller med samme ident`() {
        personopplysningerService.hentPersonopplysningerFraRegister("1")
        personopplysningerService.hentPersonopplysningerFraRegister("1")
        verify(exactly = 1) { egenAnsattClient.egenAnsatt(any()) }

        personopplysningerService.hentPersonopplysningerFraRegister("2")
        verify(exactly = 2) { egenAnsattClient.egenAnsatt(any()) }
    }

    @Test
    internal fun `cachede personopplysninger skal følge toggle når den slås av og på`() {
        every { pdlClient.hentFalskIdentitet("1") } returns FalskIdentitet(erFalsk = true, rettIdentitetErUkjent = true)

        medCache { service ->
            assertThat(service.hentPersonopplysningerFraRegister("1").falskIdentitet).isNotNull()

            every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns false
            assertThat(service.hentPersonopplysningerFraRegister("1").falskIdentitet).isNull()

            every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns true
            assertThat(service.hentPersonopplysningerFraRegister("1").falskIdentitet).isNotNull()
        }
    }

    @Test
    internal fun `cachet nav-kontor skal følge toggle når den slås av og på`() {
        val navKontor = mockk<NavKontorEnhet>()
        every { personopplysningerIntegrasjonerClient.hentNavKontor("1") } returns navKontor
        every { pdlClient.hentFalskIdentitet("1") } returns FalskIdentitet(erFalsk = true, rettIdentitetErUkjent = true)

        medCache { service ->
            every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns false
            assertThat(service.hentNavKontor("1")).isEqualTo(navKontor)

            every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns true
            assertThat(service.hentNavKontor("1")).isNull()

            every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns false
            assertThat(service.hentNavKontor("1")).isEqualTo(navKontor)
        }
    }

    @Test
    internal fun `skal ikke hente falsk identitet fra PDL når toggle er av`() {
        every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns false

        assertThat(personopplysningerService.hentPersonopplysningerFraRegister("toggle-av").falskIdentitet).isNull()
        verify(exactly = 0) { pdlClient.hentFalskIdentitet(any()) }
    }

    @Test
    internal fun `skal ignorere cachet falsk identitet når toggle slås av`() {
        every { pdlClient.hentFalskIdentitet("1") } returns FalskIdentitet(erFalsk = true, rettIdentitetErUkjent = true)
        assertThat(personopplysningerService.hentFalskIdentitet("1")).isNotNull()

        every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns false

        assertThat(personopplysningerService.hentFalskIdentitet("1")).isNull()
        verify(exactly = 1) { pdlClient.hentFalskIdentitet("1") }
    }

    @Test
    internal fun `skal hente nav-kontor uten oppslag på falsk identitet når toggle er av`() {
        every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns false
        every { pdlClient.hentFalskIdentitet("1") } returns FalskIdentitet(erFalsk = true, rettIdentitetErUkjent = true)

        personopplysningerService.hentNavKontor("1")

        verify(exactly = 1) { personopplysningerIntegrasjonerClient.hentNavKontor("1") }
        verify(exactly = 0) { pdlClient.hentFalskIdentitet(any()) }
    }

    @Test
    internal fun `skal sette falskIdentitet når PDL har merket identen som falsk`() {
        every { pdlClient.hentFalskIdentitet(any()) } returns FalskIdentitet(erFalsk = true, rettIdentitetErUkjent = true)

        val personopplysninger = personopplysningerService.hentPersonopplysningerFraRegister("1")

        assertThat(personopplysninger.falskIdentitet).isEqualTo(FalskIdentitetDto(rettIdentitetErUkjent = true))
    }

    @Test
    internal fun `skal sette rettIdentitetErUkjent til false når PDL ikke har satt feltet`() {
        every { pdlClient.hentFalskIdentitet(any()) } returns FalskIdentitet(erFalsk = true, rettIdentitetErUkjent = null)

        val personopplysninger = personopplysningerService.hentPersonopplysningerFraRegister("1")

        assertThat(personopplysninger.falskIdentitet).isEqualTo(FalskIdentitetDto(rettIdentitetErUkjent = false))
    }

    @Test
    internal fun `skal ikke hente nav-kontor når identen er merket som falsk`() {
        every { pdlClient.hentFalskIdentitet(any()) } returns FalskIdentitet(erFalsk = true, rettIdentitetErUkjent = true)

        assertThat(personopplysningerService.hentNavKontor("1")).isNull()
        verify(exactly = 0) { personopplysningerIntegrasjonerClient.hentNavKontor(any()) }
    }

    @Test
    internal fun `skal hente nav-kontor når identen ikke er merket som falsk`() {
        every { pdlClient.hentFalskIdentitet(any()) } returns null

        personopplysningerService.hentNavKontor("1")

        verify(exactly = 1) { personopplysningerIntegrasjonerClient.hentNavKontor("1") }
    }

    @Test
    internal fun `skal ikke sette falskIdentitet når erFalsk er false`() {
        every { pdlClient.hentFalskIdentitet(any()) } returns FalskIdentitet(erFalsk = false, rettIdentitetErUkjent = null)

        val personopplysninger = personopplysningerService.hentPersonopplysningerFraRegister("1")

        assertThat(personopplysninger.falskIdentitet).isNull()
    }

    @Test
    internal fun `feil ved henting av falsk identitet skal ikke stoppe personopplysninger og skal ikke caches`() {
        every { pdlClient.hentFalskIdentitet(any()) } throws PdlRequestException("Feil fra PDL")

        assertThat(personopplysningerService.hentPersonopplysningerFraRegister("1").falskIdentitet).isNull()

        every { pdlClient.hentFalskIdentitet(any()) } returns FalskIdentitet(erFalsk = true, rettIdentitetErUkjent = false)

        assertThat(personopplysningerService.hentPersonopplysningerFraRegister("1").falskIdentitet)
            .isEqualTo(FalskIdentitetDto(rettIdentitetErUkjent = false))
    }

    private fun medCache(test: (PersonopplysningerService) -> Unit) {
        AnnotationConfigApplicationContext().use { context ->
            context.register(CacheConfig::class.java)
            context.beanFactory.registerSingleton("featureToggleService", featureToggleService)
            context.beanFactory.registerSingleton("shortCache", ConcurrentMapCacheManager())
            context.registerBean("personopplysningerService", PersonopplysningerService::class.java, Supplier { personopplysningerService })
            context.refresh()
            test(context.getBean(PersonopplysningerService::class.java))
        }
    }

    @TestConfiguration
    @EnableCaching
    class CacheConfig

    private fun readFile(filnavn: String): String = this::class.java.getResource(filnavn)!!.readText()
}
