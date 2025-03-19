package no.nav.familie.ef.sak.vilkår.gjenbruk

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.opprettGrunnlagsdata
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Søknadsverdier
import no.nav.familie.ef.sak.opplysninger.søknad.domain.tilSøknadsverdier
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.barnMedIdent
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.sivilstand
import no.nav.familie.ef.sak.repository.søker
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.samværsavtale.SamværsavtaleService
import no.nav.familie.ef.sak.testutil.søknadBarnTilBehandlingBarn
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class GjenbrukVilkårServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val barnService = mockk<BarnService>()
    private val tilordnetRessursService = mockk<TilordnetRessursService>()
    private val samværsavtaleService = mockk<SamværsavtaleService>()
    private val gjenbrukVilkårService =
        GjenbrukVilkårService(
            behandlingService = behandlingService,
            fagsakService = fagsakService,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            grunnlagsdataService = grunnlagsdataService,
            barnService = barnService,
            tilordnetRessursService = tilordnetRessursService,
            samværsavtaleService = samværsavtaleService,
        )

    private val barn1 = FnrGenerator.generer(LocalDate.now())
    private val barn2 = FnrGenerator.generer(LocalDate.now().minusYears(5))
    private val barn3 = FnrGenerator.generer(LocalDate.now().minusYears(12))
    private val søknadOS =
        SøknadsskjemaMapper
            .tilDomene(
                TestsøknadBuilder
                    .Builder()
                    .setBarn(
                        listOf(
                            TestsøknadBuilder.Builder().defaultBarn("Barn Nummer En", barn1),
                            TestsøknadBuilder.Builder().defaultBarn("Barn Nummer To", barn2),
                        ),
                    ).build()
                    .søknadOvergangsstønad,
            ).tilSøknadsverdier()
    private val søknadBT =
        SøknadsskjemaMapper
            .tilDomene(
                TestsøknadBuilder
                    .Builder()
                    .setBarn(
                        listOf(
                            TestsøknadBuilder.Builder().defaultBarn("Barn Nummer To", barn2),
                            TestsøknadBuilder.Builder().defaultBarn("Barn Nummer Tre", barn3),
                        ),
                    ).build()
                    .søknadBarnetilsyn,
            ).tilSøknadsverdier()

    private val fagsakPersonId = UUID.randomUUID()
    private val fagsakOS = fagsak(stønadstype = StønadType.OVERGANGSSTØNAD, fagsakPersonId = fagsakPersonId)
    private val fagsakBT = fagsak(stønadstype = StønadType.BARNETILSYN, fagsakPersonId = fagsakPersonId)
    private val ferdigstiltBehandlingOS = behandling(fagsakOS, status = BehandlingStatus.FERDIGSTILT)
    private val nyBehandlingBT = behandling(fagsak = fagsakBT, status = BehandlingStatus.UTREDES)
    private val nyBehandlingOS = behandling(fagsak = fagsakOS, status = BehandlingStatus.OPPRETTET)

    private val vilkårsvurderingerSlot = slot<List<Vilkårsvurdering>>()
    private val vilkårsvurderingSlot = slot<Vilkårsvurdering>()

    private val ferdigstiltOS =
        TestData(fagsakOS, ferdigstiltBehandlingOS, søknadOS, Vilkårsresultat.OPPFYLT)
    private val nyBT =
        TestData(fagsakBT, nyBehandlingBT, søknadBT, Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    private val nyOS =
        TestData(fagsakOS, nyBehandlingOS, søknadOS, Vilkårsresultat.IKKE_TATT_STILLING_TIL)

    @BeforeEach
    internal fun setUp() {
        vilkårsvurderingerSlot.clear()
        vilkårsvurderingSlot.clear()
        listOf(ferdigstiltOS, nyBT, nyOS).forEach {
            every { fagsakService.hentFagsakForBehandling(it.behandling.id) } returns it.fagsak
            every { fagsakService.hentFagsak(it.fagsak.id) } returns it.fagsak

            every { behandlingService.hentSaksbehandling(it.behandling.id) } returns it.saksbehandling

            mockGrunnlagsdata(it.behandling.id, Sivilstandstype.SEPARERT)

            every { vilkårsvurderingRepository.findByBehandlingId(it.behandling.id) } returns listOf()

            every { vilkårsvurderingRepository.findByBehandlingId(it.behandling.id) } returns
                listOf(it.sivilstandsvilkår, it.aktivitetsvilkår) + it.aleneomsorgsvilkår

            every { barnService.finnBarnPåBehandling(it.behandling.id) } returns it.behandlingBarn
        }
        every { behandlingService.hentBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsakPersonId) } returns
            listOf(
                ferdigstiltBehandlingOS,
                nyBehandlingBT,
            )

        every { vilkårsvurderingRepository.updateAll(capture(vilkårsvurderingerSlot)) } answers { firstArg() }
        every { vilkårsvurderingRepository.update(capture(vilkårsvurderingSlot)) } answers { firstArg() }
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true
    }

    @Test
    internal fun `skal ikke hente ut behandlingen man står på når det skal gjenbrukes vilkår`() {
        val behandlingerForGjenruk = gjenbrukVilkårService.finnBehandlingerForGjenbruk(nyBT.behandling.id)
        assertThat(behandlingerForGjenruk.map { it.id }).containsOnly(ferdigstiltOS.behandling.id)
    }

    @Test
    internal fun `skal hente ut behandlinger for gjenbruk av vilkår`() {
        val behandlingerForGjenruk = gjenbrukVilkårService.finnBehandlingerForGjenbruk(nyOS.behandling.id)
        assertThat(behandlingerForGjenruk.map { it.id }).containsExactlyInAnyOrder(ferdigstiltOS.behandling.id, nyBT.behandling.id)
    }

    @Test
    internal fun `gjennbruk vilkår for sivilstand`() {
        gjenbrukVilkår()

        assertThat(vilkårsvurderingerSlot.captured).hasSize(2)
        val oppdaterteSivilstand = vilkårsvurderingerSlot.captured.single { it.type == VilkårType.SIVILSTAND }
        assertThat(oppdaterteSivilstand.id).isEqualTo(nyBT.sivilstandsvilkår.id)
        assertThat(oppdaterteSivilstand.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    internal fun `skal ikke gjenbruke aktivitetsvilkår`() {
        gjenbrukVilkår()

        assertThat(vilkårsvurderingerSlot.captured).hasSize(2)
        assertThat(vilkårsvurderingerSlot.captured.map { it.type })
            .containsExactlyInAnyOrder(VilkårType.SIVILSTAND, VilkårType.ALENEOMSORG)
    }

    @Test
    internal fun `skal ikke gjenbruke vilkår for sivilstand dersom sivilstand er endret`() {
        mockGrunnlagsdata(nyBT.behandling.id, Sivilstandstype.GIFT)
        gjenbrukVilkår()

        assertThat(vilkårsvurderingerSlot.captured).hasSize(1)
        assertThat(vilkårsvurderingerSlot.captured.map { it.type })
            .containsExactlyInAnyOrder(VilkårType.ALENEOMSORG)
    }

    @Test
    internal fun `gjenbruk vilkår for barn 2 som finnes på begge behandlinger`() {
        gjenbrukVilkår()

        val oppdatertAleneomsorg = vilkårsvurderingerSlot.captured.single { it.type == VilkårType.ALENEOMSORG }

        val barnSomSkalGjennbrukes = nyBT.behandlingBarn.single { it.personIdent == barn2 }
        val aleneomsorgsvilkår = nyBT.aleneomsorgsvilkår.single { it.barnId == barnSomSkalGjennbrukes.id }

        assertThat(oppdatertAleneomsorg.id).isEqualTo(aleneomsorgsvilkår.id)
        assertThat(oppdatertAleneomsorg.barnId).isNotNull
        assertThat(oppdatertAleneomsorg.barnId).isEqualTo(aleneomsorgsvilkår.barnId)
        assertThat(oppdatertAleneomsorg.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)

        assertThat(nyBT.sivilstandsvilkår.resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(nyBT.aleneomsorgsvilkår.map { it.resultat }).containsOnly(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    internal fun `skal kaste feil dersom behandlingen er låst for videre redigering`() {
        every { behandlingService.hentSaksbehandling(nyBehandlingBT.id) } returns
            nyBT.saksbehandling.copy(status = BehandlingStatus.IVERKSETTER_VEDTAK)

        assertThatThrownBy { gjenbrukVilkår() }
            .hasMessageContaining("Behandlingen er låst og vilkår kan ikke oppdateres på behandling med id=")
    }

    @Test
    internal fun `skal kaste feil dersom dersom ingen behandlinger for gjenbruk eksisterer`() {
        every { behandlingService.hentBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsakPersonId) } returns emptyList()

        assertThatThrownBy { gjenbrukVilkår() }
            .hasMessageContaining("Fant ingen tidligere behandlinger som kan benyttes til gjenbruk av inngangsvilkår for behandling med id=")
    }

    @Test
    internal fun `skal kaste feil dersom dersom behandlinger for gjenbruk ikke inneholder tidligere behandling`() {
        every { behandlingService.hentBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsakPersonId) } returns listOf(behandling(id = UUID.randomUUID()))
        assertThatThrownBy { gjenbrukVilkår() }
            .hasMessageContaining("kan ikke benyttes til gjenbruk av inngangsvilkår for behandling med id=")
    }

    @Test
    internal fun `skal gjenbruke enkel vilkårsvurdering - silvilstand`() {
        gjenbrukEnkelVilkårsvurdering(nyBT.sivilstandsvilkår.id)

        assertThat(vilkårsvurderingSlot.isCaptured).isEqualTo(true)
        assertThat(vilkårsvurderingSlot.captured.id).isEqualTo(nyBT.sivilstandsvilkår.id)
        assertThat(vilkårsvurderingSlot.captured.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        verify(exactly = 0) { samværsavtaleService.gjenbrukSamværsavtale(any(), any(), any(), any()) }
    }

    @Test
    internal fun `skal gjenbruke enkel vilkårsvurdering og samværsavtale - aleneomsorg`() {
        val behandlingSomSkalOppdateresSlot = slot<UUID>()
        val behandlingForGjenbrukSlot = slot<UUID>()
        val barnPåBehandlingSomSkalOppdateresSlot = slot<List<BehandlingBarn>>()
        val vilkårsvurderingSomSkalOppdateresSlot = slot<Vilkårsvurdering>()

        every { samværsavtaleService.gjenbrukSamværsavtale(capture(behandlingSomSkalOppdateresSlot), capture(behandlingForGjenbrukSlot), capture(barnPåBehandlingSomSkalOppdateresSlot), capture(vilkårsvurderingSomSkalOppdateresSlot)) } just Runs
        gjenbrukEnkelVilkårsvurdering(nyBT.aleneomsorgsvilkår.first().id)

        assertThat(vilkårsvurderingSlot.isCaptured).isEqualTo(true)
        assertThat(vilkårsvurderingSlot.captured.id).isEqualTo(nyBT.aleneomsorgsvilkår.first().id)
        assertThat(vilkårsvurderingSlot.captured.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)

        verify(exactly = 1) { samværsavtaleService.gjenbrukSamværsavtale(any(), any(), any(), any()) }

        assertThat(behandlingSomSkalOppdateresSlot.captured).isEqualTo(nyBT.behandling.id)
        assertThat(behandlingForGjenbrukSlot.captured).isEqualTo(ferdigstiltBehandlingOS.id)
        assertThat(barnPåBehandlingSomSkalOppdateresSlot.captured.size).isEqualTo(2)
        assertThat(barnPåBehandlingSomSkalOppdateresSlot.captured.first().behandlingId).isEqualTo(nyBT.behandling.id)
        assertThat(barnPåBehandlingSomSkalOppdateresSlot.captured.first().navn).isEqualTo("Barn Nummer To")
        assertThat(barnPåBehandlingSomSkalOppdateresSlot.captured.last().behandlingId).isEqualTo(nyBT.behandling.id)
        assertThat(barnPåBehandlingSomSkalOppdateresSlot.captured.last().navn).isEqualTo("Barn Nummer Tre")
        assertThat(vilkårsvurderingSomSkalOppdateresSlot.captured.id).isEqualTo(nyBT.aleneomsorgsvilkår.first().id)
    }

    @Test
    internal fun `utledGjenbrukbareVilkårsvurderinger skal returnere en liste med IDer`() {
        val gjenbrukbareVilkårsvurderinger =
            gjenbrukVilkårService.utledGjenbrukbareVilkårsvurderinger(
                nyBT.behandling.id,
                ferdigstiltOS.behandling.id,
            )

        val silvilstandIdForGjenbruk = nyBT.sivilstandsvilkår.id
        assertThat(gjenbrukbareVilkårsvurderinger.map { it.id }).contains(
            silvilstandIdForGjenbruk,
        )
        assertThat(gjenbrukbareVilkårsvurderinger).isNotEmpty()
    }

    private fun gjenbrukVilkår() {
        gjenbrukVilkårService.gjenbrukInngangsvilkårVurderinger(
            nyBT.behandling.id,
            ferdigstiltOS.behandling.id,
        )
    }

    private fun gjenbrukEnkelVilkårsvurdering(vilkårId: UUID): Vilkårsvurdering =
        gjenbrukVilkårService.gjenbrukInngangsvilkårVurderingOgSamværsavtale(
            nyBT.behandling.id,
            ferdigstiltOS.behandling.id,
            vilkårId,
            nyBT.behandlingBarn,
        )

    private fun mockGrunnlagsdata(
        behandlingId: UUID,
        sivilstandstype: Sivilstandstype,
    ) {
        val grunnlagsdata =
            opprettGrunnlagsdata().copy(
                søker = søker(sivilstand = listOf(sivilstand(sivilstandstype))),
                barn = listOf(barnMedIdent(fnr = "123", navn = "fornavn etternavn")),
            )
        every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns
            GrunnlagsdataMedMetadata(grunnlagsdata, LocalDateTime.now())
    }

    data class TestData(
        val fagsak: Fagsak,
        val behandling: Behandling,
        val søknad: Søknadsverdier,
        val vilkårsresultat: Vilkårsresultat,
    ) {
        val saksbehandling: Saksbehandling = saksbehandling(fagsak, behandling)
        val behandlingBarn = søknadBarnTilBehandlingBarn(søknad.barn, behandling.id)

        val sivilstandsvilkår: Vilkårsvurdering
        val aleneomsorgsvilkår: List<Vilkårsvurdering>
        val aktivitetsvilkår: Vilkårsvurdering

        init {
            sivilstandsvilkår = vilkårsvurdering(behandling.id, vilkårsresultat, type = VilkårType.SIVILSTAND)
            aleneomsorgsvilkår =
                behandlingBarn.map {
                    vilkårsvurdering(
                        behandling.id,
                        vilkårsresultat,
                        type = VilkårType.ALENEOMSORG,
                        barnId = it.id,
                    )
                }
            aktivitetsvilkår = vilkårsvurdering(behandling.id, vilkårsresultat, type = VilkårType.AKTIVITET)
        }
    }
}
