package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.RegistergrunnlagRepository
import no.nav.familie.ef.sak.repository.domain.Endret
import no.nav.familie.ef.sak.repository.domain.Registergrunnlag
import no.nav.familie.ef.sak.repository.domain.RegistergrunnlagData
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime

internal class GrunnlagsdataServiceTest {

    private val registergrunnlagRepository = mockk<RegistergrunnlagRepository>()
    private val pdlClient = PdlClientConfig().pdlClient()
    private val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val medlemskapMapper = MedlemskapMapper(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

    private val service = GrunnlagsdataService(registergrunnlagRepository,
                                               pdlClient,
                                               familieIntegrasjonerClient,
                                               medlemskapMapper,
                                               behandlingService)
    private val behandling = behandling(fagsak())
    private val behandlingId = behandling.id
    private val updateSlot = slot<Registergrunnlag>()
    private val insertSlot = slot<Registergrunnlag>()

    private val søknad = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)
    private val medlemskapsinfo = Medlemskapsinfo(søknad.fødselsnummer, emptyList(), emptyList(), emptyList())

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentOvergangsstønad(behandlingId) } returns søknad
        every { familieIntegrasjonerClient.hentMedlemskapsinfo(any()) } returns medlemskapsinfo
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

        verify(exactly = 1) { behandlingService.hentOvergangsstønad(any()) }
        verify(exactly = 1) { registergrunnlagRepository.insert(any()) }
        verify(exactly = 0) { registergrunnlagRepository.update(any()) }
    }

    @Test
    internal fun `hentEndringer - skal ikke opprette registergrunnlag hvis det finnes fra før`() {
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns registergrunnlag(false)
        service.hentEndringerIRegistergrunnlag(behandlingId)

        verify(exactly = 0) { registergrunnlagRepository.insert(any()) }
        verify(exactly = 0) { registergrunnlagRepository.update(any()) }
        verify(exactly = 0) { behandlingService.hentOvergangsstønad(any()) }
    }

    @Test
    internal fun `hentEndringer - differ data og endringer`() {
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns registergrunnlag(true)
        val endringer = service.hentEndringerIRegistergrunnlag(behandlingId)
        assertThat(endringer).isEqualTo(mapOf("medlemskap" to listOf("folkeregisterpersonstatus"),
                                              "sivilstand" to listOf("type")))
    }

    @Test
    internal fun `hentEndringer - skal oppdatere registergrunnlag hvis det har gått mer enn 4h`() {
        val registergrunnlag = registergrunnlag(false, LocalDateTime.now().minusDays(1))
        every { registergrunnlagRepository.findByIdOrNull(behandlingId) } returns registergrunnlag

        service.hentEndringerIRegistergrunnlag(behandlingId)

        verify(exactly = 1) { behandlingService.hentOvergangsstønad(any()) }
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

    private fun registergrunnlag(medEndringer: Boolean = false, endretTid: LocalDateTime = LocalDateTime.now()): Registergrunnlag {
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
                                              gyldigFraOgMed = null))
    }
}