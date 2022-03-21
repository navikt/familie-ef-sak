package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.config.InfotrygdReplikaMock
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRegisterService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRepository
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.tilSøknadsverdier
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.testutil.søknadsBarnTilBehandlingBarn
import no.nav.familie.ef.sak.vilkår.MedlemskapMapper
import no.nav.familie.ef.sak.vilkår.VilkårGrunnlagService
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate


internal class VilkårGrunnlagServiceTest {

    private val grunnlagsdataRepository = mockk<GrunnlagsdataRepository>()
    private val pdlClient = PdlClientConfig().pdlClient()
    private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    private val søknadService = mockk<SøknadService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val medlemskapMapper = MedlemskapMapper(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
    private val infotrygdService = InfotrygdService(InfotrygdReplikaMock().infotrygdReplikaClient(), pdlClient)
    private val behandlingService = mockk<BehandlingService>()

    private val grunnlagsdataRegisterService = GrunnlagsdataRegisterService(pdlClient,
                                                                            personopplysningerIntegrasjonerClient,
                                                                            infotrygdService)

    private val fagsakService = mockk<FagsakService>()
    private val grunnlagsdataService = GrunnlagsdataService(grunnlagsdataRepository,
                                                            søknadService,
                                                            grunnlagsdataRegisterService,
                                                            behandlingService, mockk())

    private val service = VilkårGrunnlagService(medlemskapMapper, grunnlagsdataService, fagsakService)
    private val behandling = behandling(fagsak())
    private val behandlingId = behandling.id


    private val søknadsBuilder = TestsøknadBuilder.Builder()
    val barnepassOrdning = søknadsBuilder.defaultBarnepassordning(
            type = "barnehageOgLiknende",
            navn = "Humpetitten barnehage",
            fraDato = LocalDate.of(2021, 1, 1),
            tilDato = LocalDate.of(2021, 6, 30),
            beløp = 3000.0
    )
    val søknadsbarn = listOf(
            søknadsBuilder.defaultBarn(
                    navn = "Navn1 navnesen",
                    fødselTermindato = LocalDate.now().plusMonths(4),
                    barnepass = søknadsBuilder.defaultBarnepass(årsakSvarId = "trengerMerPassEnnJevnaldrede",
                                                                ordninger = listOf(barnepassOrdning)),
                    skalHaBarnepass = true
            ),
            søknadsBuilder.defaultBarn(
                    navn = "Navn2 navnesen",
                    fødselTermindato = LocalDate.now().plusMonths(6),
                    barnepass = søknadsBuilder.defaultBarnepass(årsakSvarId = null,
                                                                ordninger = listOf(søknadsBuilder.defaultBarnepassordning(beløp = 2000.0))),
                    skalHaBarnepass = true
            )
    )
    private val søknadOvergangsstønad =
            SøknadsskjemaMapper.tilDomene(søknadsBuilder.setBarn(søknadsbarn).build().søknadOvergangsstønad)
                    .tilSøknadsverdier()

    private val søknadBarnetilsyn =
            SøknadsskjemaMapper.tilDomene(søknadsBuilder.setBarn(søknadsbarn).build().søknadOvergangsstønad)
                    .tilSøknadsverdier()
    private val barn = søknadsBarnTilBehandlingBarn(søknadOvergangsstønad.barn)
    private val barnBarnetilsyn = søknadsBarnTilBehandlingBarn(søknadBarnetilsyn.barn)
    private val medlemskapsinfo = Medlemskapsinfo(søknadOvergangsstønad.fødselsnummer, emptyList(), emptyList(), emptyList())
    private val fagsak = fagsak(identer = setOf(PersonIdent(søknadOvergangsstønad.fødselsnummer)))

    @BeforeEach
    internal fun setUp() {
        every { søknadService.hentSøknadsgrunnlag(behandlingId) } returns søknadOvergangsstønad
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak
        every { personopplysningerIntegrasjonerClient.hentMedlemskapsinfo(any()) } returns medlemskapsinfo
        every { featureToggleService.isEnabled(any(), any()) } returns false
    }

    @Test
    internal fun `mapping går ok`() {
        val data = grunnlagsdataService.hentGrunnlagsdataFraRegister("1", emptyList())
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns Grunnlagsdata(behandlingId, data)
        service.hentGrunnlag(behandlingId, søknadOvergangsstønad, søknadOvergangsstønad.fødselsnummer, barn)
    }

    @Test
    internal fun `sortering av barnMedSamvær`() {
        val data = grunnlagsdataService.hentGrunnlagsdataFraRegister("1", emptyList())
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns Grunnlagsdata(behandlingId, data)

        val grunnlag = service.hentGrunnlag(behandlingId, søknadOvergangsstønad, søknadOvergangsstønad.fødselsnummer, barn)

        assertThat(grunnlag.barnMedSamvær.size).isEqualTo(2)
        assertThat(grunnlag.barnMedSamvær[0].søknadsgrunnlag.navn).isEqualTo("Navn2 navnesen")
        assertThat(grunnlag.barnMedSamvær[1].søknadsgrunnlag.navn).isEqualTo("Navn1 navnesen")
    }

    @Test
    internal fun `skal ikke ha barnepass for overgangsstønad`() {
        val data = grunnlagsdataService.hentGrunnlagsdataFraRegister("1", emptyList())
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns Grunnlagsdata(behandlingId, data)

        val grunnlag = service.hentGrunnlag(behandlingId, søknadOvergangsstønad, søknadOvergangsstønad.fødselsnummer, barn)

        assertThat(grunnlag.barnMedSamvær.size).isEqualTo(2)
        assertThat(grunnlag.barnMedSamvær[0].barnepass).isNull()
        assertThat(grunnlag.barnMedSamvær[1].barnepass).isNull()
    }

    @Test
    internal fun `skal ha barnepass for barnetilsyn`() {
        val data = grunnlagsdataService.hentGrunnlagsdataFraRegister("1", emptyList())
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns Grunnlagsdata(behandlingId, data)
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak.copy(stønadstype = StønadType.BARNETILSYN)

        val grunnlag = service.hentGrunnlag(behandlingId, søknadBarnetilsyn, søknadOvergangsstønad.fødselsnummer, barnBarnetilsyn)

        assertThat(grunnlag.barnMedSamvær.size).isEqualTo(2)

        assertThat(grunnlag.barnMedSamvær[0].barnepass?.skalHaBarnepass).isTrue()
        assertThat(grunnlag.barnMedSamvær[0].barnepass?.årsakBarnepass).isNull()
        assertThat(grunnlag.barnMedSamvær[0].barnepass?.barnepassordninger).hasSize(1)
        assertThat(grunnlag.barnMedSamvær[0].barnepass?.barnepassordninger?.first()?.beløp).isEqualTo(2000)

        assertThat(grunnlag.barnMedSamvær[1].barnepass?.skalHaBarnepass).isTrue()
        assertThat(grunnlag.barnMedSamvær[1].barnepass?.årsakBarnepass).isEqualTo("trengerMerPassEnnJevnaldrede")
        assertThat(grunnlag.barnMedSamvær[1].barnepass?.barnepassordninger).hasSize(1)
        assertThat(grunnlag.barnMedSamvær[1].barnepass?.barnepassordninger?.first()?.navn).isEqualTo("Humpetitten barnehage")
        assertThat(grunnlag.barnMedSamvær[1].barnepass?.barnepassordninger?.first()?.beløp).isEqualTo(3000)
        assertThat(grunnlag.barnMedSamvær[1].barnepass?.barnepassordninger?.first()?.fra).isEqualTo(LocalDate.of(2021, 1, 1))
        assertThat(grunnlag.barnMedSamvær[1].barnepass?.barnepassordninger?.first()?.til).isEqualTo(LocalDate.of(2021, 6, 30))
        assertThat(grunnlag.barnMedSamvær[1].barnepass?.barnepassordninger?.first()?.type).isEqualTo("barnehageOgLiknende")

    }
}