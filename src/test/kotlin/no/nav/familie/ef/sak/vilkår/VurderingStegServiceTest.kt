package no.nav.familie.ef.sak.vilkår

import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
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
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.samværsavtale.SamværsavtaleService
import no.nav.familie.ef.sak.testutil.søknadBarnTilBehandlingBarn
import no.nav.familie.ef.sak.vilkår.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.OppdaterVilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.vilkår.dto.SivilstandRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.SvarPåVurderingerDto
import no.nav.familie.ef.sak.vilkår.dto.VurderingDto
import no.nav.familie.ef.sak.vilkår.gjenbruk.GjenbrukVilkårService
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår.opprettNyeVilkårsvurderinger
import no.nav.familie.ef.sak.vilkår.regler.vilkårsreglerForStønad
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.UUID

internal class VurderingStegServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val søknadService = mockk<SøknadService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val barnService = mockk<BarnService>()
    private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    private val blankettRepository = mockk<BlankettRepository>()
    private val vilkårGrunnlagService = mockk<VilkårGrunnlagService>()
    private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val fagsakService = mockk<FagsakService>()
    private val gjenbrukVilkårService = mockk<GjenbrukVilkårService>()
    private val tilordnetRessursService = mockk<TilordnetRessursService>()
    private val samværsavtaleService = mockk<SamværsavtaleService>()
    private val behandlingStegOppdaterer = mockk<BehandlingStegOppdaterer>()
    private val vurderingService =
        VurderingService(
            behandlingService,
            søknadService,
            vilkårsvurderingRepository,
            barnService,
            vilkårGrunnlagService,
            grunnlagsdataService,
            fagsakService,
            gjenbrukVilkårService,
            tilordnetRessursService,
            samværsavtaleService,
        )
    private val vurderingStegService =
        VurderingStegService(
            behandlingService = behandlingService,
            vurderingService = vurderingService,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            blankettRepository = blankettRepository,
            tilordnetRessursService = tilordnetRessursService,
            behandlingStegOppdaterer = behandlingStegOppdaterer,
        )
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
    val fagsak = fagsak()
    private val behandling = behandling(fagsak, BehandlingStatus.OPPRETTET)
    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandlingService.hentSaksbehandling(behandlingId) } returns saksbehandling(fagsak, behandling)
        every { behandlingService.hentAktivIdent(behandlingId) } returns søknad.fødselsnummer
        every { behandlingService.oppdaterStatusPåBehandling(any(), any()) } returns behandling
        every { behandlingService.oppdaterKategoriPåBehandling(any(), any()) } returns behandling
        every { søknadService.hentSøknadsgrunnlag(any()) }.returns(søknad)
        every { behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(behandlingId) } just Runs
        every { blankettRepository.deleteById(any()) } just runs
        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsak(stønadstype = OVERGANGSSTØNAD)
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
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true
        val sivilstand =
            SivilstandInngangsvilkårDto(
                mockk(relaxed = true),
                SivilstandRegistergrunnlagDto(Sivilstandstype.GIFT, "1", "Navn", null),
            )
        every { vilkårGrunnlagService.hentGrunnlag(any(), any(), any(), any()) } returns
            mockVilkårGrunnlagDto(sivilstand = sivilstand)
        every { gjenbrukVilkårService.finnBehandlingerForGjenbruk(any()) } returns emptyList()

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
        assertThat(
            catchThrowable {
                vurderingStegService.oppdaterVilkår(
                    SvarPåVurderingerDto(
                        id = vurderingId,
                        behandlingId = behandlingId,
                        delvilkårsvurderinger = listOf(),
                    ),
                )
            },
        ).hasMessageContaining("Finner ikke Vilkårsvurdering med id")
    }

    @Test
    internal fun `skal oppdatere vilkårsvurdering med resultat, begrunnelse og unntak`() {
        val lagretVilkårsvurdering = slot<Vilkårsvurdering>()
        val vilkårsvurdering = initiererVurderinger(lagretVilkårsvurdering)

        val delvilkårDto =
            listOf(
                DelvilkårsvurderingDto(
                    Vilkårsresultat.IKKE_OPPFYLT,
                    listOf(
                        VurderingDto(
                            RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN,
                            SvarId.JA,
                            "a",
                        ),
                    ),
                ),
            )
        vurderingStegService.oppdaterVilkår(
            SvarPåVurderingerDto(
                id = vilkårsvurdering.id,
                behandlingId = behandlingId,
                delvilkårsvurderinger = delvilkårDto,
            ),
        )

        assertThat(lagretVilkårsvurdering.captured.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(lagretVilkårsvurdering.captured.type).isEqualTo(vilkårsvurdering.type)
        assertThat(lagretVilkårsvurdering.captured.opphavsvilkår).isNull()

        val delvilkårsvurdering =
            lagretVilkårsvurdering.captured.delvilkårsvurdering.delvilkårsvurderinger
                .first()
        assertThat(delvilkårsvurdering.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(delvilkårsvurdering.vurderinger).hasSize(1)
        assertThat(delvilkårsvurdering.vurderinger.first().svar).isEqualTo(SvarId.JA)
        assertThat(delvilkårsvurdering.vurderinger.first().begrunnelse).isEqualTo("a")
    }

    @Test
    internal fun `skal oppdatere vilkårsvurdering med resultat SKAL_IKKE_VURDERES`() {
        every { barnService.finnBarnPåBehandling(behandlingId) } returns barn
        val oppdatertVurdering = slot<Vilkårsvurdering>()
        val vilkårsvurdering = initiererVurderinger(oppdatertVurdering)

        vurderingStegService.settVilkårTilSkalIkkeVurderes(
            OppdaterVilkårsvurderingDto(
                id = vilkårsvurdering.id,
                behandlingId = behandlingId,
            ),
        )

        assertThat(oppdatertVurdering.captured.resultat).isEqualTo(Vilkårsresultat.SKAL_IKKE_VURDERES)
        assertThat(oppdatertVurdering.captured.type).isEqualTo(vilkårsvurdering.type)
        assertThat(oppdatertVurdering.captured.opphavsvilkår).isNull()

        val delvilkårsvurdering =
            oppdatertVurdering.captured.delvilkårsvurdering.delvilkårsvurderinger
                .first()
        assertThat(delvilkårsvurdering.resultat).isEqualTo(Vilkårsresultat.SKAL_IKKE_VURDERES)
        assertThat(delvilkårsvurdering.vurderinger).hasSize(1)
        assertThat(delvilkårsvurdering.vurderinger.first().svar).isNull()
        assertThat(delvilkårsvurdering.vurderinger.first().begrunnelse).isNull()
    }

    @Test
    internal fun `nullstille skal fjerne opphavsvilkår fra vilkårsvurdering`() {
        every { barnService.finnBarnPåBehandling(behandlingId) } returns barn
        val oppdatertVurdering = slot<Vilkårsvurdering>()
        val vilkårsvurdering = initiererVurderinger(oppdatertVurdering)

        vurderingStegService.nullstillVilkår(OppdaterVilkårsvurderingDto(vilkårsvurdering.id, behandlingId))

        assertThat(oppdatertVurdering.captured.resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(oppdatertVurdering.captured.type).isEqualTo(vilkårsvurdering.type)
        assertThat(oppdatertVurdering.captured.opphavsvilkår).isNull()
    }

    @Test
    internal fun `skal ikke oppdatere vilkårsvurdering hvis behandlingen er låst for videre behandling`() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(fagsak(), BehandlingStatus.FERDIGSTILT)
        val vilkårsvurdering =
            vilkårsvurdering(
                behandlingId,
                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                VilkårType.FORUTGÅENDE_MEDLEMSKAP,
            )
        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering

        assertThat(
            catchThrowable {
                vurderingStegService.oppdaterVilkår(
                    SvarPåVurderingerDto(
                        id = vilkårsvurdering.id,
                        behandlingId = behandlingId,
                        listOf(),
                    ),
                )
            },
        ).isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("er låst for videre redigering")
        verify(exactly = 0) { vilkårsvurderingRepository.insertAll(any()) }
    }

    @Test
    internal fun `skal oppdatere status fra OPPRETTET til UTREDES og lage historikkinnslag for første vilkår`() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns
            saksbehandling(
                fagsak(),
                status = BehandlingStatus.OPPRETTET,
            )
        val lagretVilkårsvurdering = slot<Vilkårsvurdering>()
        val vilkårsvurdering = initiererVurderinger(lagretVilkårsvurdering)
        val delvilkårDto =
            listOf(
                DelvilkårsvurderingDto(
                    Vilkårsresultat.IKKE_OPPFYLT,
                    listOf(
                        VurderingDto(
                            RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN,
                            SvarId.JA,
                            "a",
                        ),
                    ),
                ),
            )
        vurderingStegService.oppdaterVilkår(
            SvarPåVurderingerDto(
                id = vilkårsvurdering.id,
                behandlingId = behandlingId,
                delvilkårsvurderinger = delvilkårDto,
            ),
        )

        verify(exactly = 1) { behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(behandlingId) }
    }

    @Test
    internal fun `skal ikke oppdatere status til UTREDES eller opprette historikkinnslag hvis den allerede er dette `() {
        val fagsak = fagsak()
        every { behandlingService.hentSaksbehandling(behandlingId) } returns
            saksbehandling(
                fagsak,
                status = BehandlingStatus.UTREDES,
            )
        val lagretVilkårsvurdering = slot<Vilkårsvurdering>()
        val vilkårsvurdering = initiererVurderinger(lagretVilkårsvurdering)
        val delvilkårDto =
            listOf(
                DelvilkårsvurderingDto(
                    Vilkårsresultat.IKKE_OPPFYLT,
                    listOf(
                        VurderingDto(
                            RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN,
                            SvarId.JA,
                            "a",
                        ),
                    ),
                ),
            )
        vurderingStegService.oppdaterVilkår(
            SvarPåVurderingerDto(
                id = vilkårsvurdering.id,
                behandlingId = behandlingId,
                delvilkårsvurderinger = delvilkårDto,
            ),
        )

        verify(exactly = 1) { behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(behandlingId) }
    }

    @Test
    internal fun `behandlingen uten barn skal likevel opprette et vilkår for aleneomsorg`() {
        val vilkårsvurderinger =
            opprettNyeVilkårsvurderinger(
                behandlingId,
                HovedregelMetadata(
                    null,
                    Sivilstandstype.UGIFT,
                    barn = emptyList(),
                    søktOmBarnetilsyn = emptyList(),
                    vilkårgrunnlagDto = mockVilkårGrunnlagDto(),
                    behandling = behandling,
                ),
                OVERGANGSSTØNAD,
            )

        assertThat(vilkårsvurderinger).hasSize(vilkårsreglerForStønad(OVERGANGSSTØNAD).size)
        assertThat(vilkårsvurderinger.count { it.type == VilkårType.ALENEOMSORG }).isEqualTo(1)
    }

    // KUN FOR Å TESTE OPPDATERSTEG
    private fun initiererVurderinger(lagretVilkårsvurdering: CapturingSlot<Vilkårsvurdering>): Vilkårsvurdering {
        val vilkårsvurdering =
            vilkårsvurdering(
                behandlingId,
                Vilkårsresultat.OPPFYLT,
                VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                listOf(
                    Delvilkårsvurdering(
                        Vilkårsresultat.OPPFYLT,
                        listOf(Vurdering(RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN)),
                    ),
                ),
                opphavsvilkår = Opphavsvilkår(UUID.randomUUID(), LocalDateTime.now()),
            )
        val vilkårsvurderinger =
            opprettNyeVilkårsvurderinger(
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
            ).map { if (it.type == vilkårsvurdering.type) vilkårsvurdering else it }

        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns vilkårsvurderinger
        every { vilkårsvurderingRepository.update(capture(lagretVilkårsvurdering)) } answers
            { it.invocation.args.first() as Vilkårsvurdering }
        return vilkårsvurdering
    }
}
