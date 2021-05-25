package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.api.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.api.dto.MedlUnntakDto
import no.nav.familie.ef.sak.api.dto.MedlemskapRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.SivilstandRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.pdl.Metadata
import no.nav.familie.ef.sak.integration.dto.pdl.Sivilstand
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.GrunnlagsdataRepository
import no.nav.familie.ef.sak.repository.RegistergrunnlagRepository
import no.nav.familie.ef.sak.repository.domain.Endret
import no.nav.familie.ef.sak.repository.domain.Registergrunnlag
import no.nav.familie.ef.sak.repository.domain.RegistergrunnlagData
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.familie.ef.sak.integration.dto.pdl.Sivilstandstype as SivilstandstypePdl

internal class GrunnlagsdataServiceTest {

    private val registergrunnlagRepository = mockk<RegistergrunnlagRepository>()
    private val grunnlagsdataRepository = mockk<GrunnlagsdataRepository>()
    private val pdlClient = PdlClientConfig().pdlClient()
    private val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()
    private val søknadService = mockk<SøknadService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val medlemskapMapper = MedlemskapMapper(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

    private val persisterGrunnlagsdataService = PersisterGrunnlagsdataService(pdlClient,
                                                                              grunnlagsdataRepository,
                                                                              søknadService,
                                                                              featureToggleService,
                                                                              familieIntegrasjonerClient)

    private val service = GrunnlagsdataService(registergrunnlagRepository,
                                               featureToggleService,
                                               medlemskapMapper,
                                               søknadService,
                                               persisterGrunnlagsdataService)
    private val behandling = behandling(fagsak())
    private val behandlingId = behandling.id
    private val updateSlot = slot<Registergrunnlag>()
    private val insertSlot = slot<Registergrunnlag>()

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
        every { registergrunnlagRepository.insert(capture(insertSlot)) } answers { firstArg() }
        every { registergrunnlagRepository.update(capture(updateSlot)) } answers { firstArg() }
    }

    @Test
    internal fun `hentGrunnlag - skal kaste feil hvis det ikke finnes noe registergrunnlag`() {
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns null
        assertThat(catchThrowable { service.hentGrunnlag(behandlingId, søknad) })
                .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    internal fun `hentGrunnlag - skal hente data fra databasen`() {
        val registergrunnlag = registergrunnlag()
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns registergrunnlag

        val grunnlag = service.hentGrunnlag(behandlingId, søknad)
        assertThat(registergrunnlag.endringer).isNull()
        assertThat(grunnlag.medlemskap.registergrunnlag).isEqualTo(registergrunnlag.data.medlemskap)
        assertThat(grunnlag.sivilstand.registergrunnlag).isEqualTo(registergrunnlag.data.sivilstand)

        verify(exactly = 0) { pdlClient.hentSøker(any()) }
        verify(exactly = 0) { familieIntegrasjonerClient.hentMedlemskapsinfo(any()) }
    }

    @Test
    internal fun `hentGrunnlag - skal returnere endringer hvis det finnes`() {
        val registergrunnlag = registergrunnlag(medEndringer = true)
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns registergrunnlag

        val grunnlag = service.hentGrunnlag(behandlingId, søknad)
        assertThat(registergrunnlag.endringer).isNotNull
        val endringer = registergrunnlag.endringer!!
        assertThat(grunnlag.medlemskap.registergrunnlag).isEqualTo(endringer.medlemskap)
        assertThat(grunnlag.sivilstand.registergrunnlag).isEqualTo(endringer.sivilstand)

        verify(exactly = 0) { pdlClient.hentSøker(any()) }
        verify(exactly = 0) { familieIntegrasjonerClient.hentMedlemskapsinfo(any()) }
    }

    @Test
    internal fun `hentEndringer - skal opprette registergrunnlag hvis det ikke finnes fra før`() {
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns null
        service.hentEndringerIRegistergrunnlag(behandlingId)

        verify(exactly = 2) { søknadService.hentOvergangsstønad(any()) }
        verify(exactly = 1) { registergrunnlagRepository.insert(any()) }
        verify(exactly = 0) { registergrunnlagRepository.update(any()) }
    }

    @Test
    internal fun `hentEndringer - skal ikke opprette registergrunnlag hvis det finnes fra før`() {
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns registergrunnlag(false)
        service.hentEndringerIRegistergrunnlag(behandlingId)

        verify(exactly = 0) { registergrunnlagRepository.insert(any()) }
        verify(exactly = 0) { registergrunnlagRepository.update(any()) }
        verify(exactly = 0) { søknadService.hentOvergangsstønad(any()) }
    }

    @Test
    internal fun `hentEndringer - differ data og endringer`() {
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns registergrunnlag(true)
        val endringer = service.hentEndringerIRegistergrunnlag(behandlingId)
        assertThat(endringer).isEqualTo(mapOf("medlemskap" to listOf("folkeregisterpersonstatus"),
                                              "sivilstand" to listOf("type"),
                                              "barnMedSamvær" to emptyList()))
    }

    @Test
    internal fun `hentEndringer - har gått mer en 4h - skal oppdatere registergrunnlag ved endringer`() {
        val registergrunnlag = registergrunnlag(false, LocalDateTime.now().minusDays(1))
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns registergrunnlag

        service.hentEndringerIRegistergrunnlag(behandlingId)

        assertThat(updateSlot.captured.endringer).isNotNull
        verify(exactly = 2) { søknadService.hentOvergangsstønad(any()) }
        verify(exactly = 1) { registergrunnlagRepository.update(any()) }
    }

    @Test
    internal fun `godkjenn - skal ikke oppdatere hvis det ikke finnes endringer`() {
        val registergrunnlag = registergrunnlag(false)
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns registergrunnlag

        service.godkjennEndringerIRegistergrunnlag(behandlingId)

        verify(exactly = 0) { registergrunnlagRepository.update(any()) }
    }

    @Test
    internal fun `godkjenn - skal sette data til endringer og sette nye endringer i endringer hvis det er diff`() {
        val registergrunnlag = registergrunnlag(true)
        assertThat(registergrunnlag.data).isNotEqualTo(registergrunnlag.endringer)
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns registergrunnlag

        service.godkjennEndringerIRegistergrunnlag(behandlingId)

        verify { registergrunnlagRepository.update(any()) }
        val captured = updateSlot.captured
        assertThat(captured.endringer).isNotNull
        assertThat(captured.data).isEqualTo(registergrunnlag.endringer)
    }

    @Test
    internal fun `sortering av barnMedSamvær`() {
        val registergrunnlag = registergrunnlag()
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns registergrunnlag

        val grunnlag = service.hentGrunnlag(behandlingId, søknad)

        assertThat(grunnlag.barnMedSamvær.size).isEqualTo(2)
        assertThat(grunnlag.barnMedSamvær.get(0).søknadsgrunnlag.navn).isEqualTo("Navn2 navnesen")
        assertThat(grunnlag.barnMedSamvær.get(1).søknadsgrunnlag.navn).isEqualTo("Navn1 navnesen")
    }

    @Test
    internal fun `skal hente navn til relatertVedSivilstand fra sivilstand når personen har sivilstand`() {
        val sivilstand = Sivilstand(SivilstandstypePdl.GIFT, null, "11111122222", null, Metadata(false))
        val pdlSøker = PdlClientConfig.opprettPdlSøker().copy(sivilstand = listOf(sivilstand))
        val fullmakt = pdlSøker.fullmakt.map { it.motpartsPersonident }
        every { pdlClient.hentSøker(any()) } returns pdlSøker
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns null

        service.hentEndringerIRegistergrunnlag(behandlingId)

        verify(exactly = 1) { pdlClient.hentPersonKortBolk(listOf(sivilstand.relatertVedSivilstand!!) + fullmakt) }
    }

    @Test
    internal fun `skal ikke hente navn til relatertVedSivilstand fra sivilstand når det ikke finnes sivilstand`() {
        val sivilstand = Sivilstand(SivilstandstypePdl.UOPPGITT, null, null, null, Metadata(false))
        every { pdlClient.hentSøker(any()) } returns PdlClientConfig.opprettPdlSøker()
                .copy(sivilstand = listOf(sivilstand), fullmakt = emptyList())
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns null
        service.hentEndringerIRegistergrunnlag(behandlingId)
        verify(exactly = 0) { pdlClient.hentPersonKortBolk(any()) }
    }

    private fun registergrunnlag(medEndringer: Boolean = false,
                                 endretTid: LocalDateTime = LocalDateTime.now()): Registergrunnlag {
        return Registergrunnlag(behandlingId = behandlingId,
                                data = registergrunnlagData(),
                                endringer = if (medEndringer) registergrunnlagData(medEndringer) else null,
                                sporbar = Sporbar(endret = Endret(endretTid = endretTid)))
    }

    private fun registergrunnlagData(medEndringer: Boolean = false): RegistergrunnlagData {
        return RegistergrunnlagData(
                MedlemskapRegistergrunnlagDto(
                        nåværendeStatsborgerskap = emptyList(),
                        statsborgerskap = emptyList(),
                        oppholdstatus = emptyList(),
                        bostedsadresse = emptyList(),
                        innflytting = emptyList(),
                        utflytting = emptyList(),
                        folkeregisterpersonstatus = if (medEndringer) null else Folkeregisterpersonstatus.BOSATT,
                        medlUnntak = MedlUnntakDto(emptyList())),
                SivilstandRegistergrunnlagDto(type = if (medEndringer) Sivilstandstype.GIFT else Sivilstandstype.UGIFT,
                                              navn = "navn",
                                              gyldigFraOgMed = null),
                søknad.barn.map {
                    BarnMedSamværRegistergrunnlagDto(id = it.id,
                                                     navn = null,
                                                     fødselsnummer = null,
                                                     harSammeAdresse = null,
                                                     forelder = null)
                })
    }
}