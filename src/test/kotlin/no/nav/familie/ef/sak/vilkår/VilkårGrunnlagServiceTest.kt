package no.nav.familie.ef.sak.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.kodeverk.KodeverkService
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.kontantstøtte.KontantstøtteService
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.opplysninger.mapper.adresseMapper
import no.nav.familie.ef.sak.opplysninger.mapper.barnMedSamværMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRegisterService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRepository
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.TidligereVedtaksperioderService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt.FullmaktService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.medl.MedlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.medl.MedlService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.tilSøknadsverdier
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.testutil.søknadBarnTilBehandlingBarn
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

internal class VilkårGrunnlagServiceTest {
    private val grunnlagsdataRepository = mockk<GrunnlagsdataRepository>()
    private val personService = PersonService(PdlClientConfig().pdlClient(), ConcurrentMapCacheManager())
    private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    private val søknadService = mockk<SøknadService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val kodeverkService = mockk<KodeverkService>(relaxed = true)
    private val medlemskapMapper = MedlemskapMapper(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), kodeverkService)
    private val behandlingService = mockk<BehandlingService>()
    private val tidligereVedtaksperioderService = mockk<TidligereVedtaksperioderService>(relaxed = true)
    private val arbeidsforholdService = mockk<ArbeidsforholdService>(relaxed = true)
    private val tilordnetRessursService = mockk<TilordnetRessursService>(relaxed = true)
    private val kontantstøtteService = mockk<KontantstøtteService>(relaxed = true)
    private val fullmaktService = mockk<FullmaktService>(relaxed = true)
    private val medlService = mockk<MedlService>(relaxed = true)

    private val grunnlagsdataRegisterService =
        GrunnlagsdataRegisterService(
            personService,
            tidligereVedtaksperioderService,
            arbeidsforholdService,
            kontantstøtteService,
            fullmaktService,
            medlService,
        )

    private val fagsakService = mockk<FagsakService>()
    private val grunnlagsdataService =
        GrunnlagsdataService(
            grunnlagsdataRepository,
            søknadService,
            grunnlagsdataRegisterService,
            behandlingService,
            mockk(),
            tilordnetRessursService,
        )

    private val service =
        VilkårGrunnlagService(
            medlemskapMapper = medlemskapMapper,
            grunnlagsdataService = grunnlagsdataService,
            fagsakService = fagsakService,
            barnMedsamværMapper = barnMedSamværMapper(),
            adresseMapper = adresseMapper(),
        )
    private val behandling = behandling(fagsak())
    private val behandlingId = behandling.id

    private val søknadsBuilder = TestsøknadBuilder.Builder()
    val barnepassOrdning =
        søknadsBuilder.defaultBarnepassordning(
            type = "barnehageOgLiknende",
            navn = "Humpetitten barnehage",
            fraDato = LocalDate.of(2021, 1, 1),
            tilDato = LocalDate.of(2021, 6, 30),
            beløp = 3000.0,
        )
    val søknadsbarn =
        listOf(
            søknadsBuilder.defaultBarn(
                navn = "Navn1 navnesen",
                fødselTermindato = LocalDate.now().plusMonths(4),
                barnepass =
                    søknadsBuilder.defaultBarnepass(
                        årsakSvarId = "trengerMerPassEnnJevnaldrede",
                        ordninger = listOf(barnepassOrdning),
                    ),
                skalHaBarnepass = true,
            ),
            søknadsBuilder.defaultBarn(
                navn = "Navn2 navnesen",
                fødselTermindato = LocalDate.now().plusMonths(6),
                barnepass =
                    søknadsBuilder.defaultBarnepass(
                        årsakSvarId = null,
                        ordninger = listOf(søknadsBuilder.defaultBarnepassordning(beløp = 2000.0)),
                    ),
                skalHaBarnepass = true,
            ),
        )
    val oppholdsland = Søknadsfelt(label = "I hvilket land oppholder du deg?", verdi = "Polen", svarId = "POL")
    private val søknadOvergangsstønad =
        SøknadsskjemaMapper
            .tilDomene(
                søknadsBuilder
                    .setBarn(
                        søknadsbarn,
                    ).setMedlemskapsdetaljer(oppholderDuDegINorge = false, oppholdsland = oppholdsland)
                    .build()
                    .søknadOvergangsstønad,
            ).tilSøknadsverdier()

    private val søknadBarnetilsyn =
        SøknadsskjemaMapper
            .tilDomene(søknadsBuilder.setBarn(søknadsbarn).build().søknadOvergangsstønad)
            .tilSøknadsverdier()
    private val barn = søknadBarnTilBehandlingBarn(søknadOvergangsstønad.barn)
    private val barnBarnetilsyn = søknadBarnTilBehandlingBarn(søknadBarnetilsyn.barn)
    private val medlemskapsinfo = Medlemskapsinfo(søknadOvergangsstønad.fødselsnummer, emptyList(), emptyList(), emptyList())
    private val fagsak = fagsak(identer = setOf(PersonIdent(søknadOvergangsstønad.fødselsnummer)))

    @BeforeEach
    internal fun setUp() {
        every { søknadService.hentSøknadsgrunnlag(behandlingId) } returns søknadOvergangsstønad
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak
        every { personopplysningerIntegrasjonerClient.hentMedlemskapsinfo(any()) } returns medlemskapsinfo
        every { featureToggleService.isEnabled(any()) } returns false
        every { kodeverkService.hentLand("POL", any()) } returns "Polen"
        every { kodeverkService.hentLand("SWE", any()) } returns "Sverige"
    }

    @Test
    internal fun `mapping går ok`() {
        val data = grunnlagsdataService.hentFraRegisterForPersonOgAndreForeldre("1", emptyList())
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns Grunnlagsdata(behandlingId, data)
        service.hentGrunnlag(behandlingId, søknadOvergangsstønad, søknadOvergangsstønad.fødselsnummer, barn)
    }

    @Test
    internal fun `sortering av barnMedSamvær`() {
        val data = grunnlagsdataService.hentFraRegisterForPersonOgAndreForeldre("1", emptyList())
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns Grunnlagsdata(behandlingId, data)

        val grunnlag = service.hentGrunnlag(behandlingId, søknadOvergangsstønad, søknadOvergangsstønad.fødselsnummer, barn)

        assertThat(grunnlag.barnMedSamvær.size).isEqualTo(2)
        assertThat(grunnlag.barnMedSamvær[0].søknadsgrunnlag.navn).isEqualTo("Navn2 navnesen")
        assertThat(grunnlag.barnMedSamvær[1].søknadsgrunnlag.navn).isEqualTo("Navn1 navnesen")
    }

    @Test
    internal fun `skal ikke ha barnepass for overgangsstønad`() {
        val data = grunnlagsdataService.hentFraRegisterForPersonOgAndreForeldre("1", emptyList())
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns Grunnlagsdata(behandlingId, data)

        val grunnlag = service.hentGrunnlag(behandlingId, søknadOvergangsstønad, søknadOvergangsstønad.fødselsnummer, barn)

        assertThat(grunnlag.barnMedSamvær.size).isEqualTo(2)
        assertThat(grunnlag.barnMedSamvær[0].barnepass).isNull()
        assertThat(grunnlag.barnMedSamvær[1].barnepass).isNull()
    }

    @Test
    internal fun `skal ha barnepass for barnetilsyn`() {
        val data = grunnlagsdataService.hentFraRegisterForPersonOgAndreForeldre("1", emptyList())
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns Grunnlagsdata(behandlingId, data)
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak.copy(stønadstype = StønadType.BARNETILSYN)

        val grunnlag = service.hentGrunnlag(behandlingId, søknadBarnetilsyn, søknadOvergangsstønad.fødselsnummer, barnBarnetilsyn)

        assertThat(grunnlag.barnMedSamvær.size).isEqualTo(2)

        assertThat(grunnlag.barnMedSamvær[0].barnepass?.skalHaBarnepass).isTrue()
        assertThat(grunnlag.barnMedSamvær[0].barnepass?.årsakBarnepass).isNull()
        assertThat(grunnlag.barnMedSamvær[0].barnepass?.barnepassordninger).hasSize(1)
        assertThat(
            grunnlag.barnMedSamvær[0]
                .barnepass
                ?.barnepassordninger
                ?.first()
                ?.beløp,
        ).isEqualTo(2000)

        assertThat(grunnlag.barnMedSamvær[1].barnepass?.skalHaBarnepass).isTrue()
        assertThat(grunnlag.barnMedSamvær[1].barnepass?.årsakBarnepass).isEqualTo("trengerMerPassEnnJevnaldrede")
        assertThat(grunnlag.barnMedSamvær[1].barnepass?.barnepassordninger).hasSize(1)
        assertThat(
            grunnlag.barnMedSamvær[1]
                .barnepass
                ?.barnepassordninger
                ?.first()
                ?.navn,
        ).isEqualTo("Humpetitten barnehage")
        assertThat(
            grunnlag.barnMedSamvær[1]
                .barnepass
                ?.barnepassordninger
                ?.first()
                ?.beløp,
        ).isEqualTo(3000)
        assertThat(
            grunnlag.barnMedSamvær[1]
                .barnepass
                ?.barnepassordninger
                ?.first()
                ?.fra,
        ).isEqualTo(LocalDate.of(2021, 1, 1))
        assertThat(
            grunnlag.barnMedSamvær[1]
                .barnepass
                ?.barnepassordninger
                ?.first()
                ?.til,
        ).isEqualTo(LocalDate.of(2021, 6, 30))
        assertThat(
            grunnlag.barnMedSamvær[1]
                .barnepass
                ?.barnepassordninger
                ?.first()
                ?.type,
        ).isEqualTo("barnehageOgLiknende")
    }

    @Test
    internal fun `skal mappe registergrunnlag`() {
        val data = grunnlagsdataService.hentFraRegisterForPersonOgAndreForeldre("1", emptyList())
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns Grunnlagsdata(behandlingId, data)

        val grunnlag = service.hentGrunnlag(behandlingId, søknadOvergangsstønad, søknadOvergangsstønad.fødselsnummer, barn)
        assertThat(grunnlag.personalia.personIdent).isEqualTo(søknadOvergangsstønad.fødselsnummer)
        assertThat(grunnlag.personalia.navn.visningsnavn).isEqualTo("Fornavn mellomnavn Etternavn")
        assertThat(grunnlag.personalia.bostedsadresse!!.visningsadresse)
            .isEqualTo("c/o CONAVN, Charlies vei 13 b, 0575 Oslo")
    }

    @Test
    internal fun `skal mappe oppholdsland og land i medlemskap`() {
        val data = grunnlagsdataService.hentFraRegisterForPersonOgAndreForeldre("1", emptyList())
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns Grunnlagsdata(behandlingId, data)

        val grunnlag = service.hentGrunnlag(behandlingId, søknadOvergangsstønad, søknadOvergangsstønad.fødselsnummer, barn)
        val test = grunnlag.medlemskap.søknadsgrunnlag
        assertThat(test?.oppholdsland).isEqualTo("Polen")
        assertThat(test!!.utenlandsopphold[0].land).isEqualTo("Sverige")
    }
}
