package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig.Companion.ANNEN_FORELDER_FNR
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.kontantstøtte.KontantstøtteService
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRegisterService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRepository
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.TidligereVedtaksperioderService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt.FullmaktService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fullmakt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.MotpartsRolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstand
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.folkeregisteridentifikator
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

internal class GrunnlagsdataServiceTest {
    private val featureToggleService = mockk<FeatureToggleService>()
    private val grunnlagsdataRepository = mockk<GrunnlagsdataRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilordnetRessursService = mockk<TilordnetRessursService>()
    private val pdlClient = PdlClientConfig().pdlClient()
    private val personService = PersonService(pdlClient, ConcurrentMapCacheManager())
    private val søknadService = mockk<SøknadService>()
    private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    private val tidligereVedtaksperioderService = mockk<TidligereVedtaksperioderService>(relaxed = true)
    private val arbeidsforholdService = mockk<ArbeidsforholdService>(relaxed = true)
    private val kontantstøtteService = mockk<KontantstøtteService>(relaxed = true)
    private val fullmaktService = mockk<FullmaktService>(relaxed = true)
    private val grunnlagsdataRegisterService =
        GrunnlagsdataRegisterService(
            personService,
            personopplysningerIntegrasjonerClient,
            tidligereVedtaksperioderService,
            arbeidsforholdService,
            kontantstøtteService,
            fullmaktService,
        )

    private val søknad =
        SøknadsskjemaMapper.tilDomene(
            TestsøknadBuilder
                .Builder()
                .setBarn(
                    listOf(
                        TestsøknadBuilder
                            .Builder()
                            .defaultBarn("Navn1 navnesen", fødselTermindato = LocalDate.now().plusMonths(4)),
                        TestsøknadBuilder
                            .Builder()
                            .defaultBarn("Navn2 navnesen", fødselTermindato = LocalDate.now().plusMonths(6)),
                    ),
                ).build()
                .søknadOvergangsstønad,
        )

    private val service =
        GrunnlagsdataService(
            grunnlagsdataRepository = grunnlagsdataRepository,
            søknadService = søknadService,
            grunnlagsdataRegisterService = grunnlagsdataRegisterService,
            behandlingService = behandlingService,
            mockk(),
            tilordnetRessursService,
        )

    @BeforeEach
    internal fun setUp() {
        every { søknadService.hentOvergangsstønad(any()) } returns søknad
        every { personopplysningerIntegrasjonerClient.hentMedlemskapsinfo(any()) } returns
            Medlemskapsinfo("", emptyList(), emptyList(), emptyList())
        every { fullmaktService.hentFullmakt(any()) } returns
            listOf(
                Fullmakt(
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2021, 1, 1),
                    "11111133333",
                    MotpartsRolle.FULLMEKTIG,
                    listOf(),
                ),
            )
    }

    @Test
    internal fun `skal kaste feil hvis behandlingen savner grunnlagsdata`() {
        val behandling =
            behandling(
                fagsak(),
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
            )
        val behandlingId = behandling.id

        every { featureToggleService.isEnabled(any()) } returns true
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns null
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        assertThat(catchThrowable { service.hentGrunnlagsdata(behandlingId) })

        verify(exactly = 0) { personService.hentSøker(any()) }
    }

    @Test
    internal fun `skal hente navn til relatertVedSivilstand fra sivilstand når personen har sivilstand`() {
        val sivilstand = Sivilstand(Sivilstandstype.GIFT, null, "11111122222", null, Metadata(false))
        val pdlSøker =
            PdlClientConfig.opprettPdlSøker().copy(
                sivilstand = listOf(sivilstand),
                vergemaalEllerFremtidsfullmakt = emptyList(),
            )
        val fullmakt = pdlSøker.fullmakt?.map { it.motpartsPersonident } ?: emptyList()
        every { personService.hentSøker(any()) } returns pdlSøker

        service.hentFraRegisterForPersonOgAndreForeldre("1", emptyList())

        verify(exactly = 1) { pdlClient.hentPersonKortBolk(listOf(sivilstand.relatertVedSivilstand!!) + fullmakt) }
    }

    @Test
    internal fun `skal ikke hente navn til relatertVedSivilstand fra sivilstand når det ikke finnes sivilstand`() {
        val sivilstand = Sivilstand(Sivilstandstype.UOPPGITT, null, null, null, Metadata(false))
        every { personService.hentSøker(any()) } returns
            PdlClientConfig
                .opprettPdlSøker()
                .copy(
                    sivilstand = listOf(sivilstand),
                    fullmakt = emptyList(),
                    vergemaalEllerFremtidsfullmakt = emptyList(),
                )
        every { fullmaktService.hentFullmakt(any()) } returns listOf()
        service.hentFraRegisterForPersonOgAndreForeldre("1", emptyList())

        verify(exactly = 0) { pdlClient.hentPersonKortBolk(any()) }
    }

    @Test
    internal fun `skal sjekke om personen har historikk i infotrygd`() {
        val identifikatorSøker = folkeregisteridentifikator(PdlClientConfig.SØKER_FNR)
        val folkeregisteridentifikatorAnnenForelder = folkeregisteridentifikator(ANNEN_FORELDER_FNR)
        val defaultTidligereInnvilgetVedtak = TidligereInnvilgetVedtak(true, true, false)

        every { tidligereVedtaksperioderService.hentTidligereVedtaksperioder(eq(listOf(identifikatorSøker))) } returns
            TidligereVedtaksperioder(defaultTidligereInnvilgetVedtak)

        every {
            tidligereVedtaksperioderService.hentTidligereVedtaksperioder(listOf(folkeregisteridentifikatorAnnenForelder))
        } returns
            TidligereVedtaksperioder(defaultTidligereInnvilgetVedtak, defaultTidligereInnvilgetVedtak)

        val grunnlagsdata = service.hentFraRegisterForPersonOgAndreForeldre(identifikatorSøker.ident, emptyList())

        val tidligereVedtaksperioder = grunnlagsdata.tidligereVedtaksperioder!!
        assertThat(tidligereVedtaksperioder.infotrygd.harTidligereOvergangsstønad).isTrue
        assertThat(tidligereVedtaksperioder.infotrygd.harTidligereBarnetilsyn).isTrue
        assertThat(tidligereVedtaksperioder.infotrygd.harTidligereSkolepenger).isFalse

        verify(exactly = 1) { tidligereVedtaksperioderService.hentTidligereVedtaksperioder(listOf(identifikatorSøker)) }
        verify(exactly = 1) {
            tidligereVedtaksperioderService.hentTidligereVedtaksperioder(listOf(folkeregisteridentifikatorAnnenForelder))
        }
    }
}
