package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service

import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.OppdaterVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.SivilstandRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.api.dto.SvarPåVurderingerDto
import no.nav.familie.ef.sak.api.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.api.dto.VurderingDto
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.util.BrukerContextUtil
import no.nav.familie.ef.sak.regler.HovedregelMetadata
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.regler.evalutation.OppdaterVilkår.opprettNyeVilkårsvurderinger
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.Delvilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.Vurdering
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.SøknadService
import no.nav.familie.ef.sak.service.VilkårGrunnlagService
import no.nav.familie.ef.sak.service.VurderingStegService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.Properties
import java.util.UUID


internal class VurderingStegServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val søknadService = mockk<SøknadService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()
    private val blankettRepository = mockk<BlankettRepository>()
    private val vilkårGrunnlagService = mockk<VilkårGrunnlagService>()
    private val stegService = mockk<StegService>()
    private val taskRepository = mockk<TaskRepository>()
    private val vurderingService = VurderingStegService(behandlingService = behandlingService,
                                                        søknadService = søknadService,
                                                        vilkårsvurderingRepository = vilkårsvurderingRepository,
                                                        blankettRepository = blankettRepository,
                                                        stegService = stegService,
                                                        vilkårGrunnlagService = vilkårGrunnlagService,
                                                        taskRepository = taskRepository
    )
    private val søknad = SøknadsskjemaMapper.tilDomene(TestsøknadBuilder.Builder().setBarn(listOf(
            TestsøknadBuilder.Builder().defaultBarn("Navn navnesen", "13071489536"),
            TestsøknadBuilder.Builder().defaultBarn("Navn navnesen", "01012067050")
    )).build().søknadOvergangsstønad)
    private val behandling = behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
    private val BEHANDLING_ID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { behandlingService.oppdaterStatusPåBehandling(any(), any()) } returns behandling
        every { søknadService.hentOvergangsstønad(any()) }.returns(søknad)
        every { blankettRepository.deleteById(any()) } just runs
        every { taskRepository.save(any())} answers { firstArg() }
        every { familieIntegrasjonerClient.hentMedlemskapsinfo(any()) }
                .returns(Medlemskapsinfo(personIdent = søknad.fødselsnummer,
                                         gyldigePerioder = emptyList(),
                                         uavklartePerioder = emptyList(),
                                         avvistePerioder = emptyList()))
        every { vilkårsvurderingRepository.insertAll(any()) } answers { firstArg() }
        val sivilstand = SivilstandInngangsvilkårDto(mockk(relaxed = true),
                                                     SivilstandRegistergrunnlagDto(Sivilstandstype.GIFT, "Navn", null))
        every { vilkårGrunnlagService.hentGrunnlag(any(), any()) } returns VilkårGrunnlagDto(mockk(relaxed = true),
                                                                                             sivilstand,
                                                                                             mockk(relaxed = true),
                                                                                             mockk(relaxed = true),
                                                                                             mockk(relaxed = true),
                                                                                             mockk(relaxed = true),
                                                                                             mockk(relaxed = true),
                                                                                             false)

        BrukerContextUtil.mockBrukerContext("saksbehandlernavn")
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `kan ikke oppdatere vilkårsvurdering koblet til en behandling som ikke finnes`() {
        val vurderingId = UUID.randomUUID()
        every { vilkårsvurderingRepository.findByIdOrNull(vurderingId) } returns null
        assertThat(catchThrowable {
            vurderingService.oppdaterVilkår(SvarPåVurderingerDto(id = vurderingId,
                                                                 behandlingId = BEHANDLING_ID,
                                                                 delvilkårsvurderinger = listOf()))
        }).hasMessageContaining("Finner ikke Vilkårsvurdering med id")
    }

    @Test
    internal fun `skal oppdatere vilkårsvurdering med resultat, begrunnelse og unntak`() {
        val lagretVilkårsvurdering = slot<Vilkårsvurdering>()
        val vilkårsvurdering = initiererVurderinger(lagretVilkårsvurdering)

        val delvilkårDto = listOf(DelvilkårsvurderingDto(Vilkårsresultat.IKKE_OPPFYLT,
                                                         listOf(VurderingDto(RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN,
                                                                             SvarId.JA,
                                                                             "a"))))
        vurderingService.oppdaterVilkår(SvarPåVurderingerDto(id = vilkårsvurdering.id,
                                                             behandlingId = BEHANDLING_ID,
                                                             delvilkårsvurderinger = delvilkårDto))

        assertThat(lagretVilkårsvurdering.captured.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(lagretVilkårsvurdering.captured.type).isEqualTo(vilkårsvurdering.type)

        val delvilkårsvurdering = lagretVilkårsvurdering.captured.delvilkårsvurdering.delvilkårsvurderinger.first()
        assertThat(delvilkårsvurdering.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(delvilkårsvurdering.vurderinger).hasSize(1)
        assertThat(delvilkårsvurdering.vurderinger.first().svar).isEqualTo(SvarId.JA)
        assertThat(delvilkårsvurdering.vurderinger.first().begrunnelse).isEqualTo("a")
    }

    @Test
    internal fun `skal oppdatere vilkårsvurdering med resultat SKAL_IKKE_VURDERES`() {
        val oppdatertVurdering = slot<Vilkårsvurdering>()
        val vilkårsvurdering = initiererVurderinger(oppdatertVurdering)

        vurderingService.settVilkårTilSkalIkkeVurderes(OppdaterVilkårsvurderingDto(id = vilkårsvurdering.id,
                                                                                   behandlingId = BEHANDLING_ID))

        assertThat(oppdatertVurdering.captured.resultat).isEqualTo(Vilkårsresultat.SKAL_IKKE_VURDERES)
        assertThat(oppdatertVurdering.captured.type).isEqualTo(vilkårsvurdering.type)

        val delvilkårsvurdering = oppdatertVurdering.captured.delvilkårsvurdering.delvilkårsvurderinger.first()
        assertThat(delvilkårsvurdering.resultat).isEqualTo(Vilkårsresultat.SKAL_IKKE_VURDERES)
        assertThat(delvilkårsvurdering.vurderinger).hasSize(1)
        assertThat(delvilkårsvurdering.vurderinger.first().svar).isNull()
        assertThat(delvilkårsvurdering.vurderinger.first().begrunnelse).isNull()
    }

    @Test
    internal fun `skal ikke oppdatere vilkårsvurdering hvis behandlingen er låst for videre behandling`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.FERDIGSTILT)
        val vilkårsvurdering = vilkårsvurdering(BEHANDLING_ID,
                                                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                VilkårType.FORUTGÅENDE_MEDLEMSKAP)
        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering

        assertThat(catchThrowable {
            vurderingService.oppdaterVilkår(SvarPåVurderingerDto(id = vilkårsvurdering.id,
                                                                 behandlingId = BEHANDLING_ID,
                                                                 listOf()))
        }).isInstanceOf(Feil::class.java)
                .hasMessageContaining("er låst for videre redigering")
        verify(exactly = 0) { vilkårsvurderingRepository.insertAll(any()) }
    }

    @Test
    internal fun `skal oppdatere status fra OPPRETTET til UTREDES for første vilkår`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        val lagretVilkårsvurdering = slot<Vilkårsvurdering>()
        val vilkårsvurdering = initiererVurderinger(lagretVilkårsvurdering)
        val delvilkårDto = listOf(DelvilkårsvurderingDto(Vilkårsresultat.IKKE_OPPFYLT,
                                                         listOf(VurderingDto(RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN,
                                                                             SvarId.JA,
                                                                             "a"))))
        vurderingService.oppdaterVilkår(SvarPåVurderingerDto(id = vilkårsvurdering.id,
                                                             behandlingId = BEHANDLING_ID,
                                                             delvilkårsvurderinger = delvilkårDto))

        verify(exactly = 1) { behandlingService.oppdaterStatusPåBehandling(any(), BehandlingStatus.UTREDES) }
    }

    @Test
    internal fun `skal ikke oppdatere status til UTREDES hvis den allerede er dette `() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.UTREDES)
        val lagretVilkårsvurdering = slot<Vilkårsvurdering>()
        val vilkårsvurdering = initiererVurderinger(lagretVilkårsvurdering)
        val delvilkårDto = listOf(DelvilkårsvurderingDto(Vilkårsresultat.IKKE_OPPFYLT,
                                                         listOf(VurderingDto(RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN,
                                                                             SvarId.JA,
                                                                             "a"))))
        vurderingService.oppdaterVilkår(SvarPåVurderingerDto(id = vilkårsvurdering.id,
                                                             behandlingId = BEHANDLING_ID,
                                                             delvilkårsvurderinger = delvilkårDto))

        verify(exactly = 0) { behandlingService.oppdaterStatusPåBehandling(any(), BehandlingStatus.UTREDES) }
    }

    //KUN FOR Å TESTE OPPDATERSTEG
    private fun initiererVurderinger(lagretVilkårsvurdering: CapturingSlot<Vilkårsvurdering>): Vilkårsvurdering {
        val vilkårsvurdering = vilkårsvurdering(BEHANDLING_ID,
                                                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                listOf(Delvilkårsvurdering(Vilkårsresultat.OPPFYLT,
                                                                           listOf(Vurdering(RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN)))))
        val vilkårsvurderinger =
                opprettNyeVilkårsvurderinger(BEHANDLING_ID, HovedregelMetadata(søknad, Sivilstandstype.UGIFT))
                        .map { if (it.type == vilkårsvurdering.type) vilkårsvurdering else it }

        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns vilkårsvurderinger
        every { vilkårsvurderingRepository.update(capture(lagretVilkårsvurdering)) } answers
                { it.invocation.args.first() as Vilkårsvurdering }
        return vilkårsvurdering
    }
}