/*
package no.nav.familie.ef.sak.service

import io.mockk.*
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.api.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.SivilstandRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.SivilstandSøknadsgrunnlagDto
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.DelvilkårType
import no.nav.familie.ef.sak.repository.domain.DelvilkårType.*
import no.nav.familie.ef.sak.repository.domain.Delvilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.DelvilkårÅrsak
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.UUID


internal class VurderingServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()
    private val blankettRepository = mockk<BlankettRepository>()
    private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val vurderingService = VurderingService(behandlingService = behandlingService,
                                                    pdlClient = PdlClientConfig().pdlClient(),
                                                    vilkårsvurderingRepository = vilkårsvurderingRepository,
                                                    grunnlagsdataService = grunnlagsdataService,
                                                    blankettRepository = blankettRepository)

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
        every { grunnlagsdataService.hentGrunnlag(any(), any()) } returns VilkårGrunnlagDto(mockk(relaxed = true),
                                                                                            mockk(relaxed = true),
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

        vurderingService.hentVilkår(BEHANDLING_ID)

        assertThat(nyeVilkårsvurderinger.captured).hasSize(vilkår.size + 1)
        assertThat(nyeVilkårsvurderinger.captured.map { it.type }.distinct()).containsExactlyElementsOf(vilkår)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.type == VilkårType.ALENEOMSORG }).hasSize(2)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.barnId != null }).hasSize(2)
        assertThat(nyeVilkårsvurderinger.captured.map { it.resultat }.toSet()).containsOnly(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(nyeVilkårsvurderinger.captured.map { it.behandlingId }.toSet()).containsOnly(BEHANDLING_ID)
    }

    @Test
    internal fun `skal filtrere bort delvilkår pga grunnlag i søknaden`() {
        val nyeVilkårsvurderinger = slot<List<Vilkårsvurdering>>()
        every { vilkårsvurderingRepository.insertAll(capture(nyeVilkårsvurderinger)) } answers
                { it.invocation.args.first() as List<Vilkårsvurdering> }
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns emptyList()
        val søknadMed1Barn = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)
        every { behandlingService.hentOvergangsstønad(any()) }.returns(søknadMed1Barn)
        every { grunnlagsdataService.hentGrunnlag(BEHANDLING_ID, any()) } returns mockkVilkårMedUformeltGiftPerson()

        vurderingService.hentVilkår(BEHANDLING_ID)
        assertThat(nyeVilkårsvurderinger.captured.flatMap {
            it.delvilkårsvurdering.delvilkårsvurderinger
                    .filter { it.resultat != Vilkårsresultat.IKKE_AKTUELL }
                    .map { it.type }
        }).containsExactlyInAnyOrderElementsOf(
                listOf(
                        FEM_ÅRS_MEDLEMSKAP,
                        BOR_OG_OPPHOLDER_SEG_I_NORGE,
                        SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                        KRAV_SIVILSTAND,
                        LEVER_IKKE_MED_ANNEN_FORELDER,
                        LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD,
                        SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                        NÆRE_BOFORHOLD,
                        MER_AV_DAGLIG_OMSORG,
                        OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN,
                        HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER,
                        SAGT_OPP_ELLER_REDUSERT
                ))

    }

    @Test
    fun `skal ikke opprette nye Vilkårsvurderinger for vilkår som allerede har en vurdering`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns
                listOf(vilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                        type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                        behandlingId = BEHANDLING_ID))

        val nyeVilkårsvurderinger = slot<List<Vilkårsvurdering>>()
        every { vilkårsvurderingRepository.insertAll(capture(nyeVilkårsvurderinger)) } answers
                { it.invocation.args.first() as List<Vilkårsvurdering> }
        val vilkår = VilkårType.hentVilkår()

        val alleVilkårsvurderinger = vurderingService.hentVilkår(BEHANDLING_ID).vurderinger
        assertThat(nyeVilkårsvurderinger.captured).hasSize(vilkår.size)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.type == VilkårType.ALENEOMSORG }).hasSize(2)
        assertThat(alleVilkårsvurderinger).hasSize(vilkår.size + 1)
        assertThat(nyeVilkårsvurderinger.captured.map { it.type }).doesNotContain(VilkårType.FORUTGÅENDE_MEDLEMSKAP)
        assertThat(nyeVilkårsvurderinger.captured.map { it.type }).contains(VilkårType.LOVLIG_OPPHOLD)
        assertThat(nyeVilkårsvurderinger.captured.map { it.type }).contains(VilkårType.SIVILSTAND)
        assertThat(nyeVilkårsvurderinger.captured.map { it.type }).contains(VilkårType.SAMLIV)

    }

    @Test
    internal fun `skal ikke opprette vilkårsvurderinger hvis behandling er låst for videre vurdering`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.FERDIGSTILT)
        val vilkårsvurderinger = listOf(vilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                                         type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                         behandlingId = BEHANDLING_ID))
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns vilkårsvurderinger

        val alleVilkårsvurderinger = vurderingService.hentVilkår(BEHANDLING_ID).vurderinger

        assertThat(alleVilkårsvurderinger).hasSize(1)
        verify(exactly = 0) { vilkårsvurderingRepository.insertAll(any()) }
        assertThat(alleVilkårsvurderinger.map { it.id }).isEqualTo(vilkårsvurderinger.map { it.id })
    }

    @Test
    internal fun `kan ikke oppdatere vilkårsvurdering koblet til en behandling som ikke finnes`() {
        val vurderingId = UUID.randomUUID()
        every { vilkårsvurderingRepository.findByIdOrNull(vurderingId) } returns null
        assertThat(catchThrowable {
            vurderingService.oppdaterVilkår(VilkårsvurderingDto(id = vurderingId,
                                                                behandlingId = BEHANDLING_ID,
                                                                resultat = Vilkårsresultat.OPPFYLT,
                                                                vilkårType = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                                endretAv = "",
                                                                endretTid = LocalDateTime.now()))
        }).hasMessageContaining("Finner ikke Vilkårsvurdering med id")
    }

    @Test
    fun `kan ikke oppdatere vilkårsvurderingen hvis innsendte delvurderinger ikke motsvarerer de som finnes på vilkåret`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        val vilkårsvurdering = vilkårsvurdering(BEHANDLING_ID,
                                                resultat = Vilkårsresultat.IKKE_VURDERT,
                                                type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                delvilkårsvurdering =
                                                listOf(Delvilkårsvurdering(DelvilkårType.DOKUMENTERT_FLYKTNINGSTATUS)))
        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering

        assertThat(catchThrowable {
            vurderingService.oppdaterVilkår(VilkårsvurderingDto(id = vilkårsvurdering.id,
                                                                behandlingId = BEHANDLING_ID,
                                                                resultat = Vilkårsresultat.OPPFYLT,
                                                                vilkårType = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                                endretAv = "",
                                                                endretTid = LocalDateTime.now()))
        }).hasMessageContaining("Nye og eksisterende delvilkårsvurderinger har ulike antall vurderinger")
    }

    @Test
    internal fun `skal oppdatere vilkårsvurdering med resultat, begrunnelse og unntak`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        val vilkårsvurdering = vilkårsvurdering(BEHANDLING_ID,
                                                Vilkårsresultat.IKKE_VURDERT,
                                                VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                listOf(Delvilkårsvurdering(FEM_ÅRS_MEDLEMSKAP,
                                                                           Vilkårsresultat.IKKE_VURDERT)))
        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering
        val lagretVilkårsvurdering = slot<Vilkårsvurdering>()
        every { vilkårsvurderingRepository.update(capture(lagretVilkårsvurdering)) } answers
                { it.invocation.args.first() as Vilkårsvurdering }

        vurderingService.oppdaterVilkår(VilkårsvurderingDto(id = vilkårsvurdering.id,
                                                            behandlingId = BEHANDLING_ID,
                                                            resultat = Vilkårsresultat.OPPFYLT,
                                                            begrunnelse = "Ok",
                                                            unntak = "Nei",
                                                            vilkårType = vilkårsvurdering.type,
                                                            delvilkårsvurderinger =
                                                            listOf(DelvilkårsvurderingDto(vilkårsvurdering.delvilkårsvurdering
                                                                                                  .delvilkårsvurderinger
                                                                                                  .first().type,
                                                                                          Vilkårsresultat.OPPFYLT)),
                                                            endretAv = "",
                                                            endretTid = LocalDateTime.now()))
        assertThat(lagretVilkårsvurdering.captured.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(lagretVilkårsvurdering.captured.begrunnelse).isEqualTo("Ok")
        assertThat(lagretVilkårsvurdering.captured.unntak).isEqualTo("Nei")
        assertThat(lagretVilkårsvurdering.captured.delvilkårsvurdering.delvilkårsvurderinger.first().resultat)
                .isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(lagretVilkårsvurdering.captured.type).isEqualTo(vilkårsvurdering.type)
    }

    @Test
    internal fun `skal oppdatere begrunnelse og resultat for delvilkårsvurdering`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        val vilkårsvurdering = vilkårsvurdering(BEHANDLING_ID,
                                                Vilkårsresultat.IKKE_VURDERT,
                                                VilkårType.SAMLIV,
                                                listOf(
                                                        Delvilkårsvurdering(LEVER_IKKE_MED_ANNEN_FORELDER,
                                                                            Vilkårsresultat.IKKE_VURDERT,
                                                                            null),
                                                ))
        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering
        val lagretVilkårsvurdering = slot<Vilkårsvurdering>()
        every { vilkårsvurderingRepository.update(capture(lagretVilkårsvurdering)) } answers
                { it.invocation.args.first() as Vilkårsvurdering }

        val oppdatertVilkårsvurderingDto = VilkårsvurderingDto(vilkårsvurdering.id,
                                                               vilkårsvurdering.behandlingId,
                                                               Vilkårsresultat.OPPFYLT,
                                                               vilkårsvurdering.type,
                                                               null,
                                                               null,
                                                               null,
                                                               "jens123@trugdeetaten.no",
                                                               LocalDateTime.now(),
                                                               listOf(DelvilkårsvurderingDto(LEVER_IKKE_MED_ANNEN_FORELDER,
                                                                                             Vilkårsresultat.OPPFYLT,
                                                                                             null,
                                                                                             "Delvilkår ok")))
        vurderingService.oppdaterVilkår(oppdatertVilkårsvurderingDto)

        assertThat(lagretVilkårsvurdering.captured.delvilkårsvurdering.delvilkårsvurderinger.first().resultat).isEqualTo(
                Vilkårsresultat.OPPFYLT)
        assertThat(lagretVilkårsvurdering.captured.delvilkårsvurdering.delvilkårsvurderinger.first().begrunnelse).isEqualTo("Delvilkår ok")
    }

    @Test
    internal fun `skal oppdatere årsak for delvilkårsvurdering`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        val vilkårsvurdering = vilkårsvurdering(BEHANDLING_ID,
                                                Vilkårsresultat.IKKE_VURDERT,
                                                VilkårType.ALENEOMSORG,
                                                listOf(
                                                        Delvilkårsvurdering(NÆRE_BOFORHOLD,
                                                                            Vilkårsresultat.IKKE_VURDERT,
                                                                            DelvilkårÅrsak.SELVSTENDIGE_BOLIGER_SAMME_GÅRDSTUN),
                                                ))
        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering
        val lagretVilkårsvurdering = slot<Vilkårsvurdering>()
        every { vilkårsvurderingRepository.update(capture(lagretVilkårsvurdering)) } answers
                { it.invocation.args.first() as Vilkårsvurdering }

        val oppdatertVilkårsvurderingDto = VilkårsvurderingDto(vilkårsvurdering.id,
                                                               vilkårsvurdering.behandlingId,
                                                               Vilkårsresultat.OPPFYLT,
                                                               vilkårsvurdering.type,
                                                               null,
                                                               null,
                                                               null,
                                                               "jens123@trugdeetaten.no",
                                                               LocalDateTime.now(),
                                                               listOf(DelvilkårsvurderingDto(NÆRE_BOFORHOLD,
                                                                                             Vilkårsresultat.OPPFYLT,
                                                                                             DelvilkårÅrsak.SAMME_HUS_OG_FLERE_ENN_4_BOENHETER_MEN_VURDERT_NÆRT,
                                                                                             "Delvilkår ok")))
        vurderingService.oppdaterVilkår(oppdatertVilkårsvurderingDto)
        assertThat(lagretVilkårsvurdering.captured.delvilkårsvurdering.delvilkårsvurderinger.first().årsak).isEqualTo(
                DelvilkårÅrsak.SAMME_HUS_OG_FLERE_ENN_4_BOENHETER_MEN_VURDERT_NÆRT)
    }

    @Test
    internal fun `skal ikke oppdatere vilkårsvurdering hvis behandlingen er låst for videre behandling`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.FERDIGSTILT)
        val vilkårsvurdering = vilkårsvurdering(BEHANDLING_ID,
                                                resultat = Vilkårsresultat.IKKE_VURDERT,
                                                VilkårType.FORUTGÅENDE_MEDLEMSKAP)
        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering

        assertThat(catchThrowable {
            vurderingService.oppdaterVilkår(VilkårsvurderingDto(id = vilkårsvurdering.id,
                                                                behandlingId = BEHANDLING_ID,
                                                                resultat = Vilkårsresultat.OPPFYLT,
                                                                begrunnelse = "Ok",
                                                                unntak = "Nei",
                                                                vilkårType = vilkårsvurdering.type,
                                                                endretAv = "",
                                                                endretTid = LocalDateTime.now()
            ))
        }).isInstanceOf(Feil::class.java)
                .hasMessageContaining("er låst for videre redigering")
        verify(exactly = 0) { vilkårsvurderingRepository.insertAll(any()) }
    }

    @Test
    fun `skal hente vilkårtyper for vilkår som mangler vurdering`() {
        val behandling = behandling(fagsak(), true, BehandlingStatus.UTREDES)
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        val ikkeVurdertVilkår = vilkårsvurdering(BEHANDLING_ID,
                                                 resultat = Vilkårsresultat.IKKE_VURDERT,
                                                 VilkårType.FORUTGÅENDE_MEDLEMSKAP)
        val vurdertVilkår = vilkårsvurdering(BEHANDLING_ID,
                                             resultat = Vilkårsresultat.OPPFYLT,
                                             VilkårType.LOVLIG_OPPHOLD)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(ikkeVurdertVilkår, vurdertVilkår)

        val vilkårTyperUtenVurdering = vurderingService.hentVilkårSomManglerVurdering(BEHANDLING_ID)

        val vilkårtyper = VilkårType.hentVilkår().filterNot { it === VilkårType.LOVLIG_OPPHOLD }
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

        val vilkårtyper = VilkårType.hentVilkår().filterNot { it === VilkårType.FORUTGÅENDE_MEDLEMSKAP }
        assertThat(vilkårTyperUtenVurdering).containsExactlyInAnyOrderElementsOf(vilkårtyper)
    }


    private fun mockkVilkårMedUformeltGiftPerson(): VilkårGrunnlagDto {
        val sivilstandSøknadsgrunnlagDto = mockk<SivilstandSøknadsgrunnlagDto>(relaxed = true)
        every { sivilstandSøknadsgrunnlagDto.erUformeltGift } returns true
        val sivilstandRegistergrunnlagDto = SivilstandRegistergrunnlagDto(Sivilstandstype.GIFT, null)
        return VilkårGrunnlagDto(mockk(),
                                 SivilstandInngangsvilkårDto(sivilstandSøknadsgrunnlagDto, sivilstandRegistergrunnlagDto),
                                 mockk(),
                                 mockk(),
                                 mockk(),
                                 mockk(),
                                 mockk())
    }

    companion object {

        private val BEHANDLING_ID = UUID.randomUUID()
        private const val BEHANDLING_EKSTERN_ID = 12345L
    }
}*/
