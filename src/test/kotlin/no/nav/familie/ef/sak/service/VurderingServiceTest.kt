package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.vilkår.VilkårGrunnlagService
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat.OPPFYLT
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat.SKAL_IKKE_VURDERES
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.vilkår.dto.SivilstandRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID


internal class VurderingServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val søknadService = mockk<SøknadService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    private val blankettRepository = mockk<BlankettRepository>()
    private val vilkårGrunnlagService = mockk<VilkårGrunnlagService>()
    private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val vurderingService = VurderingService(behandlingService = behandlingService,
                                                    søknadService = søknadService,
                                                    vilkårsvurderingRepository = vilkårsvurderingRepository,
                                                    vilkårGrunnlagService = vilkårGrunnlagService,
                                                    grunnlagsdataService = grunnlagsdataService)
    private val søknad = SøknadsskjemaMapper.tilDomene(TestsøknadBuilder.Builder().setBarn(listOf(
            TestsøknadBuilder.Builder().defaultBarn("Navn navnesen", "13071489536"),
            TestsøknadBuilder.Builder().defaultBarn("Navn navnesen", "01012067050")
    )).build().søknadOvergangsstønad)
    private val behandling = behandling(fagsak(), BehandlingStatus.OPPRETTET)
    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { søknadService.hentOvergangsstønad(any()) }.returns(søknad)
        every { blankettRepository.deleteById(any()) } just runs
        every { personopplysningerIntegrasjonerClient.hentMedlemskapsinfo(any()) }
                .returns(Medlemskapsinfo(personIdent = søknad.fødselsnummer,
                                         gyldigePerioder = emptyList(),
                                         uavklartePerioder = emptyList(),
                                         avvistePerioder = emptyList()))
        every { vilkårsvurderingRepository.insertAll(any()) } answers { firstArg() }
        val sivilstand = SivilstandInngangsvilkårDto(mockk(relaxed = true),
                                                     SivilstandRegistergrunnlagDto(Sivilstandstype.GIFT, "Navn", null))
        every { vilkårGrunnlagService.hentGrunnlag(any(), any()) } returns VilkårGrunnlagDto(mockk(relaxed = true),
                                                                                             mockk(relaxed = true),
                                                                                             sivilstand,
                                                                                             mockk(relaxed = true),
                                                                                             mockk(relaxed = true),
                                                                                             mockk(relaxed = true),
                                                                                             mockk(relaxed = true),
                                                                                             mockk(relaxed = true),
                                                                                             false,
                                                                                             mockk(relaxed = true))
    }

    @Test
    fun `skal opprette nye Vilkårsvurdering for alle vilkår dersom ingen vurderinger finnes`() {
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns emptyList()

        val nyeVilkårsvurderinger = slot<List<Vilkårsvurdering>>()
        every { vilkårsvurderingRepository.insertAll(capture(nyeVilkårsvurderinger)) } answers
                { it.invocation.args.first() as List<Vilkårsvurdering> }
        val vilkår = VilkårType.hentVilkår()


        vurderingService.hentEllerOpprettVurderinger(behandlingId)

        assertThat(nyeVilkårsvurderinger.captured).hasSize(vilkår.size + 1) // 2 barn
        assertThat(nyeVilkårsvurderinger.captured.map { it.type }.distinct()).containsExactlyInAnyOrderElementsOf(vilkår)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.type == VilkårType.ALENEOMSORG }).hasSize(2)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.barnId != null }).hasSize(2)
        assertThat(nyeVilkårsvurderinger.captured.map { it.resultat }
                           .toSet()).containsOnly(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(nyeVilkårsvurderinger.captured.map { it.behandlingId }.toSet()).containsOnly(behandlingId)
    }

    @Test
    fun `skal ikke opprette nye Vilkårsvurderinger for behandlinger som allerede har vurderinger`() {
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns
                listOf(vilkårsvurdering(resultat = OPPFYLT,
                                        type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                        behandlingId = behandlingId))

        vurderingService.hentEllerOpprettVurderinger(behandlingId)

        verify(exactly = 0) { vilkårsvurderingRepository.updateAll(any()) }
        verify(exactly = 0) { vilkårsvurderingRepository.insertAll(any()) }
    }

    @Test
    internal fun `skal ikke returnere delvilkår som er ikke aktuelle til frontend`() {
        val delvilkårsvurdering =
                SivilstandRegel().initereDelvilkårsvurdering(HovedregelMetadata(mockk(),
                                                                                Sivilstandstype.ENKE_ELLER_ENKEMANN))
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns
                listOf(Vilkårsvurdering(behandlingId = behandlingId,
                                        type = VilkårType.SIVILSTAND,
                                        delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurdering)))

        val vilkår = vurderingService.hentEllerOpprettVurderinger(behandlingId)

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
        val vilkårsvurderinger = listOf(vilkårsvurdering(resultat = OPPFYLT,
                                                         type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                         behandlingId = behandlingId))
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns vilkårsvurderinger

        val alleVilkårsvurderinger = vurderingService.hentEllerOpprettVurderinger(behandlingId).vurderinger

        assertThat(alleVilkårsvurderinger).hasSize(1)
        verify(exactly = 0) { vilkårsvurderingRepository.insertAll(any()) }
        assertThat(alleVilkårsvurderinger.map { it.id }).isEqualTo(vilkårsvurderinger.map { it.id })
    }


    @Test
    internal fun `Skal returnere ikke oppfylt hvis vilkårsvurderinger ikke inneholder alle vilkår`() {
        val vilkårsvurderinger = listOf(vilkårsvurdering(resultat = OPPFYLT,
                                                         type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                         behandlingId = behandlingId))
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns vilkårsvurderinger
        val erAlleVilkårOppfylt = vurderingService.erAlleVilkårOppfylt(behandlingId)
        assertThat(erAlleVilkårOppfylt).isFalse
    }

    @Test
    internal fun `Skal returnere oppfylt hvis alle vilkårsvurderinger er oppfylt`() {
        val vilkårsvurderinger = lagVilkårsvurderinger(behandlingId, OPPFYLT)
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns vilkårsvurderinger
        val erAlleVilkårOppfylt = vurderingService.erAlleVilkårOppfylt(behandlingId)
        assertThat(erAlleVilkårOppfylt).isTrue
    }

    @Test
    internal fun `Skal returnere ikke oppfylt hvis noen vurderinger er SKAL_IKKE_VURDERES`() {
        val vilkårsvurderinger = lagVilkårsvurderingerMedResultat()
        // Guard
        assertThat((vilkårsvurderinger.map { it.type }.containsAll(VilkårType.hentVilkår()))).isTrue()
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns vilkårsvurderinger


        val erAlleVilkårOppfylt = vurderingService.erAlleVilkårOppfylt(behandlingId)
        assertThat(erAlleVilkårOppfylt).isFalse
    }

    private fun lagVilkårsvurderingerMedResultat(resultat1: Vilkårsresultat = OPPFYLT,
                                                 resultat2: Vilkårsresultat = SKAL_IKKE_VURDERES) =
            lagVilkårsvurderinger(behandlingId, resultat1).subList(fromIndex = 0, toIndex = 3) +
            lagVilkårsvurderinger(behandlingId, resultat2).subList(fromIndex = 3, toIndex = 10)

    private fun lagVilkårsvurderinger(behandlingId: UUID,
                                      resultat: Vilkårsresultat = OPPFYLT): List<Vilkårsvurdering> {
        return VilkårType.hentVilkår().map {
            vilkårsvurdering(behandlingId = behandlingId,
                             resultat = resultat,
                             type = it,
                             delvilkårsvurdering = listOf())
        }
    }

}