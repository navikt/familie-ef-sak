package no.nav.familie.ef.sak.vilkår

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil.mockVilkårGrunnlagDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.domain.tilSøknadsverdier
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.opprettAlleVilkårsvurderinger
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.testutil.søknadBarnTilBehandlingBarn
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.kontrakter.ef.iverksett.BehandlingKategori
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Test
import java.util.UUID

internal class BehandlingStegOppdatererTest {
    val behandlingService: BehandlingService = mockk<BehandlingService>()
    val vilkårsvurderingRepository: VilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    val stegService: StegService = mockk<StegService>()
    val behandlingshistorikkService: BehandlingshistorikkService = mockk<BehandlingshistorikkService>()
    val taskService: TaskService = mockk<TaskService>()

    val behandlingStegOppdaterer =
        BehandlingStegOppdaterer(
            behandlingService = behandlingService,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            stegService = stegService,
            behandlingshistorikkService = behandlingshistorikkService,
            taskService = taskService,
        )

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak = fagsak, status = BehandlingStatus.OPPRETTET, steg = StegType.VILKÅR)
    private val saksbehandling = saksbehandling(fagsak = fagsak, behandling = behandling)

    private val søknad =
        SøknadsskjemaMapper
            .tilDomene(
                TestsøknadBuilder
                    .Builder()
                    .setBarn(
                        listOf(
                            TestsøknadBuilder.Builder().defaultBarn("Navn navnesen", "14041385481"),
                            TestsøknadBuilder.Builder().defaultBarn("Navn navnesen", "01012067050"),
                        ),
                    ).build()
                    .søknadOvergangsstønad,
            ).tilSøknadsverdier()

    private val barn = søknadBarnTilBehandlingBarn(søknad.barn)

    @Test
    internal fun `skal sette behandling til beregne ytelsesteg - gitt en behandling i vilkårsteg hvor alle vilkår er tatt stilling til`() {
        val vilkårsvurderinger = initiererVilkårsvurderinger(behandling.id, Vilkårsresultat.OPPFYLT)

        every { behandlingService.hentSaksbehandling(behandling.id) } returns saksbehandling
        every { stegService.håndterVilkår(saksbehandling) } returns behandling
        every { vilkårsvurderingRepository.findByBehandlingId(behandling.id) } returns vilkårsvurderinger

        behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(behandling.id)

        verify(exactly = 1) { stegService.håndterVilkår(any()) }
    }

    @Test
    internal fun `skal sette behandling tilbake til vilkårsteg - gitt en behandling i beregne ytelsesteg hvor ikke alle vilkår er tatt stilling til`() {
        val vilkårsvurderinger = initiererVilkårsvurderinger(behandling.id, Vilkårsresultat.IKKE_TATT_STILLING_TIL)

        every { behandlingService.hentSaksbehandling(behandling.id) } returns saksbehandling.copy(steg = StegType.BEREGNE_YTELSE)
        every { stegService.resetSteg(saksbehandling.id, StegType.VILKÅR) } just Runs
        every { vilkårsvurderingRepository.findByBehandlingId(behandling.id) } returns vilkårsvurderinger

        behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(behandling.id)

        verify(exactly = 1) { stegService.resetSteg(saksbehandling.id, StegType.VILKÅR) }
    }

    @Test
    internal fun `skal sette status på behandling fra opprettet til utredes`() {
        val vilkårsvurderinger = initiererVilkårsvurderinger(behandling.id, Vilkårsresultat.IKKE_TATT_STILLING_TIL)

        every { behandlingService.hentSaksbehandling(behandling.id) } returns saksbehandling.copy(status = BehandlingStatus.OPPRETTET)
        every { behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES) } returns behandling
        every { behandlingshistorikkService.opprettHistorikkInnslag(saksbehandling.id, StegType.VILKÅR, StegUtfall.UTREDNING_PÅBEGYNT, null) } just Runs
        every { taskService.save(any()) } returns Task("", "")
        every { vilkårsvurderingRepository.findByBehandlingId(behandling.id) } returns vilkårsvurderinger

        behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(behandling.id)

        verify(exactly = 1) { behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES) }
        verify(exactly = 1) { behandlingshistorikkService.opprettHistorikkInnslag(saksbehandling.id, StegType.VILKÅR, StegUtfall.UTREDNING_PÅBEGYNT, null) }
        verify(exactly = 1) { taskService.save(any()) }
    }

    @Test
    internal fun `skal ikke oppdatere steg eller endre status på behandling`() {
        val vilkårsvurderinger = initiererVilkårsvurderinger(behandling.id, Vilkårsresultat.IKKE_TATT_STILLING_TIL)

        every { behandlingService.hentSaksbehandling(behandling.id) } returns saksbehandling.copy(status = BehandlingStatus.FATTER_VEDTAK)
        every { vilkårsvurderingRepository.findByBehandlingId(behandling.id) } returns vilkårsvurderinger

        behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(behandling.id)

        verify(exactly = 0) { stegService.håndterVilkår(any()) }
        verify(exactly = 0) { stegService.resetSteg(any(), any()) }
        verify(exactly = 0) { behandlingService.oppdaterStatusPåBehandling(any(), any()) }
        verify(exactly = 0) { behandlingshistorikkService.opprettHistorikkInnslag(any(), any(), any(), any()) }
        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    internal fun `skal endre behandlingkategori til nasjonal`() {
        val vilkårsvurderinger = initiererVilkårsvurderinger(behandling.id, Vilkårsresultat.OPPFYLT)
        val saksbehandlingEøs = saksbehandling.copy(kategori = BehandlingKategori.EØS)

        every { behandlingService.hentSaksbehandling(behandling.id) } returns saksbehandlingEøs
        every { behandlingService.oppdaterKategoriPåBehandling(saksbehandlingEøs.id, BehandlingKategori.NASJONAL) } returns behandling
        every { stegService.håndterVilkår(saksbehandlingEøs) } returns behandling
        every { vilkårsvurderingRepository.findByBehandlingId(behandling.id) } returns vilkårsvurderinger

        behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(behandling.id)

        verify(exactly = 1) { behandlingService.oppdaterKategoriPåBehandling(saksbehandlingEøs.id, BehandlingKategori.NASJONAL) }
    }

    @Test
    internal fun `skal ikke endre behandlingkategori`() {
        val vilkårsvurderinger = initiererVilkårsvurderinger(behandling.id, Vilkårsresultat.OPPFYLT)
        val saksbehandlingNasjonal = saksbehandling.copy(kategori = BehandlingKategori.NASJONAL)

        every { behandlingService.hentSaksbehandling(behandling.id) } returns saksbehandlingNasjonal
        every { stegService.håndterVilkår(saksbehandlingNasjonal) } returns behandling
        every { vilkårsvurderingRepository.findByBehandlingId(behandling.id) } returns vilkårsvurderinger

        behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(behandling.id)

        verify(exactly = 0) { behandlingService.oppdaterKategoriPåBehandling(saksbehandlingNasjonal.id, BehandlingKategori.NASJONAL) }
    }

    private fun initiererVilkårsvurderinger(
        behandlingId: UUID,
        vilkårsresultat: Vilkårsresultat,
    ) = opprettAlleVilkårsvurderinger(
        behandlingId,
        HovedregelMetadata(
            søknad.sivilstand,
            Sivilstandstype.UGIFT,
            barn = barn,
            søktOmBarnetilsyn = emptyList(),
            vilkårgrunnlagDto = mockVilkårGrunnlagDto(),
            behandling = behandling,
        ),
        OVERGANGSSTØNAD,
        vilkårsresultat,
    ).map { it }
}
