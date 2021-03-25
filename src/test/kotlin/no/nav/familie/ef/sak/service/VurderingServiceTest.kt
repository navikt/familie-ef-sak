package no.nav.familie.ef.sak.service

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
import no.nav.familie.ef.sak.api.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.api.dto.VurderingDto
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.regler.HovedregelMetadata
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.regler.vilkår.SivilstandRegel
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.Delvilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.Vurdering
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID


internal class VurderingServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()
    private val blankettRepository = mockk<BlankettRepository>()
    private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val stegService = mockk<StegService>()
    private val vurderingService = VurderingService(behandlingService = behandlingService,
                                                    vilkårsvurderingRepository = vilkårsvurderingRepository,
                                                    grunnlagsdataService = grunnlagsdataService,
                                                    blankettRepository = blankettRepository,
                                                    stegService = stegService)

    @BeforeEach
    fun setUp() {
        val søknad = SøknadsskjemaMapper.tilDomene(TestsøknadBuilder.Builder().setBarn(listOf(
                TestsøknadBuilder.Builder().defaultBarn("Navn navnesen", "13071489536"),
                TestsøknadBuilder.Builder().defaultBarn("Navn navnesen", "01012067050")
        )).build().søknadOvergangsstønad)
        every { behandlingService.hentOvergangsstønad(any()) }.returns(søknad)
        every { blankettRepository.deleteById(any()) } just runs
        every { familieIntegrasjonerClient.hentMedlemskapsinfo(any()) }
                .returns(Medlemskapsinfo(personIdent = søknad.fødselsnummer,
                                         gyldigePerioder = emptyList(),
                                         uavklartePerioder = emptyList(),
                                         avvistePerioder = emptyList()))
        every { vilkårsvurderingRepository.insertAll(any()) } answers { firstArg() }
        val sivilstand = SivilstandInngangsvilkårDto(mockk(relaxed = true),
                                                     SivilstandRegistergrunnlagDto(Sivilstandstype.GIFT, null))
        every { grunnlagsdataService.hentGrunnlag(any(), any()) } returns VilkårGrunnlagDto(mockk(relaxed = true),
                                                                                            sivilstand,
                                                                                            mockk(relaxed = true),
                                                                                            mockk(relaxed = true),
                                                                                            mockk(relaxed = true),
                                                                                            mockk(relaxed = true),
                                                                                            mockk(relaxed = true))
    }

    @Test
    fun `skal opprette nye Vilkårsvurdering for alle vilkår dersom ingen vurderinger finnes`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns emptyList()

        val nyeVilkårsvurderinger = slot<List<Vilkårsvurdering>>()
        every { vilkårsvurderingRepository.insertAll(capture(nyeVilkårsvurderinger)) } answers
                { it.invocation.args.first() as List<Vilkårsvurdering> }
        val vilkår = VilkårType.hentVilkår()

        vurderingService.hentEllerOpprettVurderinger(BEHANDLING_ID)

        assertThat(nyeVilkårsvurderinger.captured).hasSize(vilkår.size + 1) // 2 barn
        assertThat(nyeVilkårsvurderinger.captured.map { it.type }.distinct()).containsExactlyInAnyOrderElementsOf(vilkår)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.type == VilkårType.ALENEOMSORG }).hasSize(2)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.barnId != null }).hasSize(2)
        assertThat(nyeVilkårsvurderinger.captured.map { it.resultat }
                           .toSet()).containsOnly(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(nyeVilkårsvurderinger.captured.map { it.behandlingId }.toSet()).containsOnly(BEHANDLING_ID)
    }

    @Test
    fun `skal ikke opprette nye Vilkårsvurderinger for behandlinger som allerede har vurderinger`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns
                listOf(vilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                        type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                        behandlingId = BEHANDLING_ID))

        vurderingService.hentEllerOpprettVurderinger(BEHANDLING_ID)

        verify(exactly = 0) { vilkårsvurderingRepository.updateAll(any()) }
        verify(exactly = 0) { vilkårsvurderingRepository.insertAll(any()) }
    }

    @Test
    internal fun `skal ikke returnere delvilkår som er ikke aktuelle til frontend`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        val delvilkårsvurdering =
                SivilstandRegel().initereDelvilkårsvurdering(HovedregelMetadata(mockk(), Sivilstandstype.ENKE_ELLER_ENKEMANN))
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns
                listOf(Vilkårsvurdering(behandlingId = BEHANDLING_ID,
                                        type = VilkårType.SIVILSTAND,
                                        delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurdering)))

        val vilkår = vurderingService.hentEllerOpprettVurderinger(BEHANDLING_ID)

        assertThat(delvilkårsvurdering).hasSize(5)
        assertThat(delvilkårsvurdering.filter { it.resultat == Vilkårsresultat.IKKE_AKTUELL }).hasSize(4)
        assertThat(delvilkårsvurdering.filter { it.resultat == Vilkårsresultat.IKKE_TATT_STILLING_TIL }).hasSize(1)

        assertThat(vilkår.vurderinger).hasSize(1)
        val delvilkårsvurderinger = vilkår.vurderinger.first().delvilkårsvurderinger
        assertThat(delvilkårsvurderinger).hasSize(1)
        assertThat(delvilkårsvurderinger.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(delvilkårsvurderinger.first().vurderinger).hasSize(1)
    }

    @Test
    internal fun `skal ikke opprette vilkårsvurderinger hvis behandling er låst for videre vurdering`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.FERDIGSTILT)
        val vilkårsvurderinger = listOf(vilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                                         type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                         behandlingId = BEHANDLING_ID))
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns vilkårsvurderinger

        val alleVilkårsvurderinger = vurderingService.hentEllerOpprettVurderinger(BEHANDLING_ID).vurderinger

        assertThat(alleVilkårsvurderinger).hasSize(1)
        verify(exactly = 0) { vilkårsvurderingRepository.insertAll(any()) }
        assertThat(alleVilkårsvurderinger.map { it.id }).isEqualTo(vilkårsvurderinger.map { it.id })
    }

    @Test
    internal fun `kan ikke oppdatere vilkårsvurdering koblet til en behandling som ikke finnes`() {
        val vurderingId = UUID.randomUUID()
        every { vilkårsvurderingRepository.findByIdOrNull(vurderingId) } returns null
        assertThat(catchThrowable {
            vurderingService.oppdaterVilkår(OppdaterVilkårsvurderingDto(id = vurderingId,
                                                                        behandlingId = BEHANDLING_ID,
                                                                        delvilkårsvurderinger = listOf()))
        }).hasMessageContaining("Finner ikke Vilkårsvurdering med id")
    }

    @Test
    internal fun `skal oppdatere vilkårsvurdering med resultat, begrunnelse og unntak`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        val vilkårsvurdering = vilkårsvurdering(BEHANDLING_ID,
                                                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                listOf(Delvilkårsvurdering(Vilkårsresultat.OPPFYLT,
                                                                           listOf(Vurdering(RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN)))))
        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID)} returns listOf(vilkårsvurdering)
        val lagretVilkårsvurdering = slot<Vilkårsvurdering>()
        every { vilkårsvurderingRepository.update(capture(lagretVilkårsvurdering)) } answers
                { it.invocation.args.first() as Vilkårsvurdering }

        val delvilkårDto = listOf(DelvilkårsvurderingDto(Vilkårsresultat.IKKE_OPPFYLT,
                                                         listOf(VurderingDto(RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN,
                                                                             SvarId.JA,
                                                                             "a"))))
        vurderingService.oppdaterVilkår(OppdaterVilkårsvurderingDto(id = vilkårsvurdering.id,
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
    internal fun `skal ikke oppdatere vilkårsvurdering hvis behandlingen er låst for videre behandling`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.FERDIGSTILT)
        val vilkårsvurdering = vilkårsvurdering(BEHANDLING_ID,
                                                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                VilkårType.FORUTGÅENDE_MEDLEMSKAP)
        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering

        assertThat(catchThrowable {
            vurderingService.oppdaterVilkår(OppdaterVilkårsvurderingDto(id = vilkårsvurdering.id,
                                                                        behandlingId = BEHANDLING_ID,
                                                                        listOf()))
        }).isInstanceOf(Feil::class.java)
                .hasMessageContaining("er låst for videre redigering")
        verify(exactly = 0) { vilkårsvurderingRepository.insertAll(any()) }
    }

    @Test
    fun `skal hente vilkårtyper for vilkår som mangler vurdering`() {
        val behandling = behandling(fagsak(), true, BehandlingStatus.UTREDES)
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        val ikkeVurdertVilkår = vilkårsvurdering(BEHANDLING_ID,
                                                 resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                 VilkårType.FORUTGÅENDE_MEDLEMSKAP)
        val vurdertVilkår = vilkårsvurdering(BEHANDLING_ID,
                                             resultat = Vilkårsresultat.OPPFYLT,
                                             VilkårType.LOVLIG_OPPHOLD)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(ikkeVurdertVilkår, vurdertVilkår)

        val vilkårTyperUtenVurdering = vurderingService.hentVilkårSomManglerVurdering(BEHANDLING_ID)

        val vilkårtyper = VilkårType.hentVilkår().filterNot { it === VilkårType.LOVLIG_OPPHOLD }.filterNot { it === VilkårType.TIDLIGERE_VEDTAKSPERIODER }
        assertThat(vilkårTyperUtenVurdering).containsExactlyInAnyOrderElementsOf(vilkårtyper)
    }

    @Test
    fun `skal hente vilkårtyper for vilkår som ikke finnes`() {
        val behandling = behandling(fagsak(), true, BehandlingStatus.UTREDES)
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        val vurdertVilkår = vilkårsvurdering(BEHANDLING_ID,
                                             resultat = Vilkårsresultat.IKKE_OPPFYLT,
                                             VilkårType.FORUTGÅENDE_MEDLEMSKAP)

        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(vurdertVilkår)

        val vilkårTyperUtenVurdering = vurderingService.hentVilkårSomManglerVurdering(BEHANDLING_ID)

        val vilkårtyper = VilkårType.hentVilkår().filterNot { it === VilkårType.FORUTGÅENDE_MEDLEMSKAP }.filterNot { it === VilkårType.TIDLIGERE_VEDTAKSPERIODER }
        assertThat(vilkårTyperUtenVurdering).containsExactlyInAnyOrderElementsOf(vilkårtyper)
    }

    companion object {
        private val BEHANDLING_ID = UUID.randomUUID()
    }
}