package no.nav.familie.ef.sak.vilkår

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil.mockVilkårGrunnlagDto
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.tilSøknadsverdier
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.testutil.søknadBarnTilBehandlingBarn
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat.OPPFYLT
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat.SKAL_IKKE_VURDERES
import no.nav.familie.ef.sak.vilkår.dto.AnnenForelderDto
import no.nav.familie.ef.sak.vilkår.dto.AvstandTilSøkerDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.BarnepassDto
import no.nav.familie.ef.sak.vilkår.dto.LangAvstandTilSøker
import no.nav.familie.ef.sak.vilkår.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.vilkår.dto.SivilstandRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.gjenbruk.GjenbrukVilkårService
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.ef.StønadType.BARNETILSYN
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class VurderingServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val søknadService = mockk<SøknadService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    private val blankettRepository = mockk<BlankettRepository>()
    private val barnService = mockk<BarnService>()
    private val vilkårGrunnlagService = mockk<VilkårGrunnlagService>()
    private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val fagsakService = mockk<FagsakService>()
    private val gjenbrukVilkårService = mockk<GjenbrukVilkårService>()
    private val tilordnetRessursService = mockk<TilordnetRessursService>()
    private val vurderingService =
        VurderingService(
            behandlingService = behandlingService,
            søknadService = søknadService,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            vilkårGrunnlagService = vilkårGrunnlagService,
            grunnlagsdataService = grunnlagsdataService,
            barnService = barnService,
            fagsakService = fagsakService,
            gjenbrukVilkårService = gjenbrukVilkårService,
            tilordnetRessursService = tilordnetRessursService,
        )
    private val søknad =
        SøknadsskjemaMapper
            .tilDomene(
                TestsøknadBuilder
                    .Builder()
                    .setBarn(
                        listOf(
                            TestsøknadBuilder.Builder().defaultBarn(
                                "Navn navnesen",
                                no.nav.familie.util.FnrGenerator
                                    .generer(LocalDate.now().minusYears(5)),
                            ),
                            TestsøknadBuilder.Builder().defaultBarn(
                                "Navn navnesen",
                                no.nav.familie.util.FnrGenerator
                                    .generer(LocalDate.now().minusYears(3)),
                            ),
                        ),
                    ).build()
                    .søknadOvergangsstønad,
            ).tilSøknadsverdier()
    private val barn = søknadBarnTilBehandlingBarn(søknad.barn)
    private val behandling = behandling(fagsak(), BehandlingStatus.OPPRETTET, årsak = BehandlingÅrsak.PAPIRSØKNAD)
    private val behandlingLåst = behandling(fagsak(), BehandlingStatus.FERDIGSTILT, årsak = BehandlingÅrsak.PAPIRSØKNAD)
    private val behandlingId = UUID.randomUUID()
    private val behandlingDto = mockk<BehandlingDto>()

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentAktivIdent(behandlingId) } returns søknad.fødselsnummer
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { søknadService.hentSøknadsgrunnlag(any()) }.returns(søknad)
        every { blankettRepository.deleteById(any()) } just runs
        every { personopplysningerIntegrasjonerClient.hentMedlemskapsinfo(any()) }
            .returns(
                Medlemskapsinfo(
                    personIdent = søknad.fødselsnummer,
                    gyldigePerioder = emptyList(),
                    uavklartePerioder = emptyList(),
                    avvistePerioder = emptyList(),
                ),
            )
        every { vilkårsvurderingRepository.insertAll(any()) } answers { firstArg() }
        every { barnService.finnBarnPåBehandling(behandlingId) } returns barn
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak(stønadstype = OVERGANGSSTØNAD)
        val sivilstand =
            SivilstandInngangsvilkårDto(
                mockk(relaxed = true),
                SivilstandRegistergrunnlagDto(Sivilstandstype.GIFT, "1", "Navn", null),
            )

        val barnMedSamvær = barn.map { lagBarnetilsynBarn(it.id) }

        every { vilkårGrunnlagService.hentGrunnlag(any(), any(), any(), any()) } returns
            mockVilkårGrunnlagDto(
                sivilstand = sivilstand,
                barnMedSamvær = barnMedSamvær,
            )
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true
        every { gjenbrukVilkårService.finnBehandlingerForGjenbruk(any()) } returns listOf(behandlingDto)
        every { gjenbrukVilkårService.utledGjenbrukbareVilkårsvurderinger(any(), any()) } returns listOf()
        every { behandlingDto.id } returns behandlingId
    }

    private fun lagBarnetilsynBarn(barnId: UUID = UUID.randomUUID()) =
        BarnMedSamværDto(
            barnId,
            søknadsgrunnlag = mockk(relaxed = true),
            registergrunnlag =
                BarnMedSamværRegistergrunnlagDto(
                    UUID.randomUUID(),
                    "navn",
                    "fnr",
                    false,
                    emptyList(),
                    false,
                    AnnenForelderDto(
                        "navn",
                        "fnr2",
                        LocalDate.now().minusYears(23),
                        true,
                        "Norge",
                        "Vei 1B",
                        null,
                        null,
                        AvstandTilSøkerDto(null, LangAvstandTilSøker.UKJENT),
                    ),
                    null,
                    null,
                    null,
                    null,
                ),
            barnepass =
                BarnepassDto(
                    barnId,
                    skalHaBarnepass = true,
                    barnepassordninger = listOf(),
                    årsakBarnepass = null,
                ),
        )

    @Test
    fun `skal opprette nye Vilkårsvurdering for overgangsstønad med alle vilkår dersom ingen vurderinger finnes`() {
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns emptyList()

        val nyeVilkårsvurderinger = slot<List<Vilkårsvurdering>>()
        every { vilkårsvurderingRepository.insertAll(capture(nyeVilkårsvurderinger)) } answers
            {
                @Suppress("UNCHECKED_CAST")
                it.invocation.args.first() as List<Vilkårsvurdering>
            }
        val vilkår = VilkårType.hentVilkårForStønad(OVERGANGSSTØNAD)

        vurderingService.hentEllerOpprettVurderinger(behandlingId)

        assertThat(nyeVilkårsvurderinger.captured).hasSize(vilkår.size + 1) // 2 barn
        assertThat(
            nyeVilkårsvurderinger.captured
                .map { it.type }
                .distinct(),
        ).containsExactlyInAnyOrderElementsOf(vilkår)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.type == VilkårType.ALENEOMSORG }).hasSize(2)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.barnId != null }).hasSize(2)
        assertThat(
            nyeVilkårsvurderinger.captured
                .map { it.resultat }
                .toSet(),
        ).containsOnly(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(nyeVilkårsvurderinger.captured.map { it.behandlingId }.toSet()).containsOnly(behandlingId)
    }

    @Test
    fun `skal opprette nye Vilkårsvurdering for barnetilsyn med alle vilkår dersom ingen vurderinger finnes`() {
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns emptyList()
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak(stønadstype = BARNETILSYN)

        val nyeVilkårsvurderinger = slot<List<Vilkårsvurdering>>()
        every { vilkårsvurderingRepository.insertAll(capture(nyeVilkårsvurderinger)) } answers { firstArg() }
        val vilkår = VilkårType.hentVilkårForStønad(BARNETILSYN)

        vurderingService.hentEllerOpprettVurderinger(behandlingId)

        assertThat(nyeVilkårsvurderinger.captured).hasSize(vilkår.size + 2) // 2 barn, Ekstra aleneomsorgsvilkår og aldersvilkår
        assertThat(
            nyeVilkårsvurderinger.captured
                .map { it.type }
                .distinct(),
        ).containsExactlyInAnyOrderElementsOf(vilkår)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.type == VilkårType.ALENEOMSORG }).hasSize(2)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.type == VilkårType.ALDER_PÅ_BARN }).hasSize(2)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.barnId != null }).hasSize(4)
        assertThat(
            nyeVilkårsvurderinger.captured
                .filter { it.type == VilkårType.ALENEOMSORG }
                .map { it.resultat }
                .toSet(),
        ).containsOnly(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(
            nyeVilkårsvurderinger.captured
                .filter { it.type == VilkårType.ALDER_PÅ_BARN }
                .map { it.resultat }
                .toSet(),
        ).containsOnly(OPPFYLT)
        assertThat(nyeVilkårsvurderinger.captured.map { it.behandlingId }.toSet()).containsOnly(behandlingId)
    }

    @Test
    fun `skal ikke opprette nye Vilkårsvurderinger for behandlinger som allerede har vurderinger`() {
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns
            listOf(
                vilkårsvurdering(
                    resultat = OPPFYLT,
                    type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                    behandlingId = behandlingId,
                ),
            )

        vurderingService.hentEllerOpprettVurderinger(behandlingId)

        verify(exactly = 0) { vilkårsvurderingRepository.updateAll(any()) }
        verify(exactly = 0) { vilkårsvurderingRepository.insertAll(any()) }
    }

    @Test
    internal fun `skal ikke returnere delvilkår som er ikke aktuelle til frontend`() {
        val delvilkårsvurdering =
            SivilstandRegel().initiereDelvilkårsvurdering(
                HovedregelMetadata(
                    mockk(),
                    Sivilstandstype.ENKE_ELLER_ENKEMANN,
                    barn = emptyList(),
                    søktOmBarnetilsyn = emptyList(),
                    vilkårgrunnlagDto = mockk(),
                    behandling = behandling,
                ),
            )
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns
            listOf(
                Vilkårsvurdering(
                    behandlingId = behandlingId,
                    type = VilkårType.SIVILSTAND,
                    delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurdering),
                    opphavsvilkår = null,
                ),
            )

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
        val vilkårsvurderinger =
            listOf(
                vilkårsvurdering(
                    resultat = OPPFYLT,
                    type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                    behandlingId = behandlingId,
                ),
            )
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns vilkårsvurderinger

        val alleVilkårsvurderinger = vurderingService.hentEllerOpprettVurderinger(behandlingId).vurderinger

        assertThat(alleVilkårsvurderinger).hasSize(1)
        verify(exactly = 0) { vilkårsvurderingRepository.insertAll(any()) }
        assertThat(alleVilkårsvurderinger.map { it.id }).isEqualTo(vilkårsvurderinger.map { it.id })
    }

    @Test
    internal fun `Skal returnere ikke oppfylt hvis vilkårsvurderinger ikke inneholder alle vilkår`() {
        val vilkårsvurderinger =
            listOf(
                vilkårsvurdering(
                    resultat = OPPFYLT,
                    type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                    behandlingId = behandlingId,
                ),
            )
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
    internal fun `vilkår som kan gjenbrukes skal ha satt kanGjenbrukes-flagg lik true i DTOen`() {
        val vilkårsvurderinger = lagVilkårsvurderinger(behandlingId, OPPFYLT)
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns vilkårsvurderinger
        every { gjenbrukVilkårService.utledGjenbrukbareVilkårsvurderinger(any(), any()) } returns listOf(vilkårsvurderinger.get(0))
        val vurderinger = vurderingService.hentEllerOpprettVurderinger(behandlingId)
        assertThat(vurderinger.vurderinger.get(0).kanGjenbrukes).isTrue()
        for (i in 1 until vurderinger.vurderinger.size) {
            assertThat(vurderinger.vurderinger.get(i).kanGjenbrukes).isFalse()
        }
    }

    @Test
    internal fun `ingen vilkår som ikke kan gjenbrukes skal ha satt kanGjenbrukes-flagg i DTOen`() {
        val vilkårsvurderinger = lagVilkårsvurderinger(behandlingId, OPPFYLT)
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns vilkårsvurderinger
        val vurderinger = vurderingService.hentEllerOpprettVurderinger(behandlingId)
        vurderinger.vurderinger.forEach {
            assertThat(it.kanGjenbrukes).isFalse()
        }
    }

    @Test
    internal fun `kanGjenbrukesFlagg skal uansett være false om behandlingen er låst`() {
        val vilkårsvurderinger = lagVilkårsvurderinger(behandlingId, OPPFYLT)
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns vilkårsvurderinger
        every { gjenbrukVilkårService.utledGjenbrukbareVilkårsvurderinger(any(), any()) } returns listOf(vilkårsvurderinger.get(0))
        every { behandlingService.hentBehandling(behandlingId) } returns behandlingLåst
        val vurderinger = vurderingService.hentEllerOpprettVurderinger(behandlingId)
        vurderinger.vurderinger.forEach {
            assertThat(it.kanGjenbrukes).isFalse()
        }
    }

    @Test
    internal fun `Skal returnere ikke oppfylt hvis noen vurderinger er SKAL_IKKE_VURDERES`() {
        val vilkårsvurderinger = lagVilkårsvurderingerMedResultat()
        // Guard
        assertThat(
            (
                vilkårsvurderinger
                    .map { it.type }
                    .containsAll(VilkårType.hentVilkårForStønad(OVERGANGSSTØNAD))
            ),
        ).isTrue
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns vilkårsvurderinger

        val erAlleVilkårOppfylt = vurderingService.erAlleVilkårOppfylt(behandlingId)
        assertThat(erAlleVilkårOppfylt).isFalse
    }

    private fun lagVilkårsvurderingerMedResultat(
        resultat1: Vilkårsresultat = OPPFYLT,
        resultat2: Vilkårsresultat = SKAL_IKKE_VURDERES,
    ) = lagVilkårsvurderinger(behandlingId, resultat1).subList(fromIndex = 0, toIndex = 3) +
        lagVilkårsvurderinger(behandlingId, resultat2).subList(fromIndex = 3, toIndex = 10)

    private fun lagVilkårsvurderinger(
        behandlingId: UUID,
        resultat: Vilkårsresultat = OPPFYLT,
    ): List<Vilkårsvurdering> =
        VilkårType.hentVilkårForStønad(OVERGANGSSTØNAD).map {
            vilkårsvurdering(
                behandlingId = behandlingId,
                resultat = resultat,
                type = it,
                delvilkårsvurdering = listOf(),
            )
        }
}
