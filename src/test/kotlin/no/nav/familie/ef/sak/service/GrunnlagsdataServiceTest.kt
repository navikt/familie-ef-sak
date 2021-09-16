package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstand
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

internal class GrunnlagsdataServiceTest {


    private val featureToggleService = mockk<FeatureToggleService>()
    private val grunnlagsdataRepository = mockk<GrunnlagsdataRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val pdlClient = PdlClientConfig().pdlClient()
    private val søknadService = mockk<SøknadService>()
    private val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()

    private val søknad = SøknadsskjemaMapper.tilDomene(TestsøknadBuilder.Builder().setBarn(listOf(
            TestsøknadBuilder.Builder().defaultBarn("Navn1 navnesen", fødselTermindato = LocalDate.now().plusMonths(4)),
            TestsøknadBuilder.Builder().defaultBarn("Navn2 navnesen", fødselTermindato = LocalDate.now().plusMonths(6))
    )).build().søknadOvergangsstønad)

    private val service = GrunnlagsdataService(pdlClient = pdlClient,
                                               grunnlagsdataRepository = grunnlagsdataRepository,
                                               søknadService = søknadService,
                                               familieIntegrasjonerClient = familieIntegrasjonerClient)

    @BeforeEach
    internal fun setUp() {
        every { søknadService.hentOvergangsstønad(any()) } returns søknad
        every { familieIntegrasjonerClient.hentMedlemskapsinfo(any()) } returns
                Medlemskapsinfo("", emptyList(), emptyList(), emptyList())
    }

    @Test
    internal fun `skal kaste feil hvis behandlingen savner grunnlagsdata`() {
        val behandling = behandling(fagsak(),
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING)
        val behandlingId = behandling.id

        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns null
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        assertThat(catchThrowable { service.hentGrunnlagsdata(behandlingId) })

        verify(exactly = 0) { pdlClient.hentSøker(any()) }
    }

    @Test
    internal fun `skal kaste feil hvis behandlingen er blanket og savner grunnlagsdata`() {
        val behandling = behandling(fagsak(), type = BehandlingType.BLANKETT)
        val behandlingId = behandling.id

        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { grunnlagsdataRepository.findByIdOrNull(behandlingId) } returns null
        every { behandlingService.hentBehandling(behandlingId) } returns behandling

        assertThat(catchThrowable { service.hentGrunnlagsdata(behandlingId) })

        verify(exactly = 0) { pdlClient.hentSøker(any()) }
    }

    @Test
    internal fun `skal hente navn til relatertVedSivilstand fra sivilstand når personen har sivilstand`() {
        val sivilstand = Sivilstand(Sivilstandstype.GIFT, null, "11111122222", null, Metadata(false))
        val pdlSøker = PdlClientConfig.opprettPdlSøker().copy(sivilstand = listOf(sivilstand),
                                                              vergemaalEllerFremtidsfullmakt = emptyList())
        val fullmakt = pdlSøker.fullmakt.map { it.motpartsPersonident }
        every { pdlClient.hentSøker(any()) } returns pdlSøker

        service.hentGrunnlagsdataFraRegister("1", emptyList())

        verify(exactly = 1) { pdlClient.hentPersonKortBolk(listOf(sivilstand.relatertVedSivilstand!!) + fullmakt) }
    }

    @Test
    internal fun `skal ikke hente navn til relatertVedSivilstand fra sivilstand når det ikke finnes sivilstand`() {
        val sivilstand = Sivilstand(Sivilstandstype.UOPPGITT, null, null, null, Metadata(false))
        every { pdlClient.hentSøker(any()) } returns PdlClientConfig.opprettPdlSøker()
                .copy(sivilstand = listOf(sivilstand),
                      fullmakt = emptyList(),
                      vergemaalEllerFremtidsfullmakt = emptyList())

        service.hentGrunnlagsdataFraRegister("1", emptyList())

        verify(exactly = 0) { pdlClient.hentPersonKortBolk(any()) }
    }

}