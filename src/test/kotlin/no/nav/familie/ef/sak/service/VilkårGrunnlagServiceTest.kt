package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.GrunnlagsdataRepository
import no.nav.familie.ef.sak.repository.domain.Grunnlagsdata
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

internal class VilkårGrunnlagServiceTest {

    private val grunnlagsdataRepository = mockk<GrunnlagsdataRepository>()
    private val pdlClient = PdlClientConfig().pdlClient()
    private val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()
    private val søknadService = mockk<SøknadService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val medlemskapMapper = MedlemskapMapper(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

    private val grunnlagsdataService = GrunnlagsdataService(pdlClient,
                                                            grunnlagsdataRepository,
                                                            søknadService,
                                                            familieIntegrasjonerClient)

    private val service = VilkårGrunnlagService(medlemskapMapper, grunnlagsdataService)
    private val behandling = behandling(fagsak())
    private val behandlingId = behandling.id

    private val søknad = SøknadsskjemaMapper.tilDomene(TestsøknadBuilder.Builder().setBarn(listOf(
            TestsøknadBuilder.Builder().defaultBarn("Navn1 navnesen", fødselTermindato = LocalDate.now().plusMonths(4)),
            TestsøknadBuilder.Builder().defaultBarn("Navn2 navnesen", fødselTermindato = LocalDate.now().plusMonths(6))
    )).build().søknadOvergangsstønad)
    private val medlemskapsinfo = Medlemskapsinfo(søknad.fødselsnummer, emptyList(), emptyList(), emptyList())

    @BeforeEach
    internal fun setUp() {
        every { søknadService.hentOvergangsstønad(behandlingId) } returns søknad
        every { familieIntegrasjonerClient.hentMedlemskapsinfo(any()) } returns medlemskapsinfo
        every { featureToggleService.isEnabled(any(), any()) } returns false
    }

    @Test
    internal fun `mapping går ok`() {
        val data = grunnlagsdataService.hentGrunnlagsdataFraRegister("1", emptyList())
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns Grunnlagsdata(behandlingId, data)
        service.hentGrunnlag(behandlingId, søknad)
    }

    @Test
    internal fun `sortering av barnMedSamvær`() {
        val data = grunnlagsdataService.hentGrunnlagsdataFraRegister("1", emptyList())
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns Grunnlagsdata(behandlingId, data)

        val grunnlag = service.hentGrunnlag(behandlingId, søknad)

        assertThat(grunnlag.barnMedSamvær.size).isEqualTo(2)
        assertThat(grunnlag.barnMedSamvær[0].søknadsgrunnlag.navn).isEqualTo("Navn2 navnesen")
        assertThat(grunnlag.barnMedSamvær[1].søknadsgrunnlag.navn).isEqualTo("Navn1 navnesen")
    }
}