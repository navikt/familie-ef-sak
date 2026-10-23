package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.config.KodeverkServiceMock
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.infrastruktur.exception.PdlRequestException
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import java.time.LocalDate

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

    private fun readFile(filnavn: String): String = this::class.java.getResource(filnavn)!!.readText()
}
