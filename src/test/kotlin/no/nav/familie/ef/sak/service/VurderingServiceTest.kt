package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.DelvilkårVurderingDto
import no.nav.familie.ef.sak.api.dto.VilkårVurderingDto
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.Testsøknad
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.vilkårVurdering
import no.nav.familie.ef.sak.repository.VilkårVurderingRepository
import no.nav.familie.ef.sak.repository.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.*


internal class VurderingServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val vilkårVurderingRepository = mockk<VilkårVurderingRepository>()
    private val vurderingService = VurderingService(behandlingService = behandlingService,
                                                    pdlClient = PdlClientConfig().pdlClient(),
                                                    medlemskapMapper = mockk(relaxed = true),
                                                    vilkårVurderingRepository = vilkårVurderingRepository)

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentOvergangsstønad(any()) } returns Testsøknad.søknad
    }

    @Test
    fun `skal opprette nye VilkårVurdering for alle inngangsvilkår dersom ingen vurderinger finnes`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        every { vilkårVurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns emptyList()

        val nyeVilkårVurderinger = slot<List<VilkårVurdering>>()
        every { vilkårVurderingRepository.insertAll(capture(nyeVilkårVurderinger)) } answers
                { it.invocation.args.first() as List<VilkårVurdering> }
        val inngangsvilkår = VilkårType.hentInngangsvilkår()

        vurderingService.hentInngangsvilkår(BEHANDLING_ID)

        assertThat(nyeVilkårVurderinger.captured).hasSize(inngangsvilkår.size)
        assertThat(nyeVilkårVurderinger.captured.map { it.type }).containsExactlyElementsOf(inngangsvilkår)
        assertThat(nyeVilkårVurderinger.captured.map { it.resultat }.toSet()).containsOnly(VilkårResultat.IKKE_VURDERT)
        assertThat(nyeVilkårVurderinger.captured.map { it.behandlingId }.toSet()).containsOnly(BEHANDLING_ID)
    }

    @Test
    fun `skal ikke opprette nye VilkårVurderinger for inngangsvilkår som allerede har en vurdering`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        every { vilkårVurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(vilkårVurdering(resultat = VilkårResultat.JA,
                                                                                                             type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                                                                             behandlingId = BEHANDLING_ID))

        val nyeVilkårVurderinger = slot<List<VilkårVurdering>>()
        every { vilkårVurderingRepository.insertAll(capture(nyeVilkårVurderinger)) } answers
                { it.invocation.args.first() as List<VilkårVurdering> }
        val inngangsvilkår = VilkårType.hentInngangsvilkår()

        val alleVilkårVurderinger = vurderingService.hentInngangsvilkår(BEHANDLING_ID).vurderinger

        assertThat(nyeVilkårVurderinger.captured).hasSize(inngangsvilkår.size - 1)
        assertThat(alleVilkårVurderinger).hasSize(inngangsvilkår.size)
        assertThat(nyeVilkårVurderinger.captured.map { it.type }).doesNotContain(VilkårType.FORUTGÅENDE_MEDLEMSKAP)
    }

    @Test
    internal fun `skal ikke opprette vilkårsvurderinger hvis behandling er låst for videre vurdering`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.FERDIGSTILT)
        val vilkårsvurderinger = listOf(vilkårVurdering(resultat = VilkårResultat.JA,
                                                        type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                        behandlingId = BEHANDLING_ID))
        every { vilkårVurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns vilkårsvurderinger

        val alleVilkårVurderinger = vurderingService.hentInngangsvilkår(BEHANDLING_ID).vurderinger

        assertThat(alleVilkårVurderinger).hasSize(1)
        verify(exactly = 0) { vilkårVurderingRepository.insertAll(any()) }
        assertThat(alleVilkårVurderinger.map { it.id }).isEqualTo(vilkårsvurderinger.map { it.id })
    }

    @Test
    internal fun `kan ikke oppdatere vilkårsvurdering koblet til en behandling som ikke finnes`() {
        val vurderingId = UUID.randomUUID()
        every { vilkårVurderingRepository.findByIdOrNull(vurderingId) } returns null
        assertThat(catchThrowable {
            vurderingService.oppdaterVilkår(VilkårVurderingDto(id = vurderingId,
                                                               behandlingId = BEHANDLING_ID,
                                                               resultat = VilkårResultat.JA,
                                                               vilkårType = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                               endretAv = "",
                                                               endretTid = LocalDateTime.now()))
        }).hasMessageContaining("Finner ikke VilkårVurdering med id")
    }

    @Test
    internal fun `kan ikke oppdatere vilkårsvurderingen hvis innsendte delvurderinger ikke motsvarerer de som finnes på vilkåret`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        val vilkårVurdering = vilkårVurdering(BEHANDLING_ID,
                                              resultat = VilkårResultat.IKKE_VURDERT,
                                              type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                              delvilkårVurdering = listOf(DelvilkårVurdering(DelvilkårType.DOKUMENTERT_FLYKTNINGSTATUS)))
        every { vilkårVurderingRepository.findByIdOrNull(vilkårVurdering.id) } returns vilkårVurdering

        assertThat(catchThrowable {
            vurderingService.oppdaterVilkår(VilkårVurderingDto(id = vilkårVurdering.id,
                                                               behandlingId = BEHANDLING_ID,
                                                               resultat = VilkårResultat.JA,
                                                               vilkårType = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                                               endretAv = "",
                                                               endretTid = LocalDateTime.now()))
        }).hasMessageContaining("Delvilkårstyper motsvarer ikke de som finnes lagrede på vilkåret")
    }

    @Test
    internal fun `skal oppdatere vilkårsvurdering med resultat, begrunnelse og unntak`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        val vilkårVurdering = vilkårVurdering(BEHANDLING_ID,
                                              VilkårResultat.IKKE_VURDERT,
                                              VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                              listOf(DelvilkårVurdering(DelvilkårType.TRE_ÅRS_MEDLEMSKAP,
                                                                        VilkårResultat.IKKE_VURDERT)))
        every { vilkårVurderingRepository.findByIdOrNull(vilkårVurdering.id) } returns vilkårVurdering
        val lagretVilkårVurdering = slot<VilkårVurdering>()
        every { vilkårVurderingRepository.update(capture(lagretVilkårVurdering)) } answers { it.invocation.args.first() as VilkårVurdering }

        vurderingService.oppdaterVilkår(VilkårVurderingDto(id = vilkårVurdering.id,
                                                           behandlingId = BEHANDLING_ID,
                                                           resultat = VilkårResultat.JA,
                                                           begrunnelse = "Ok",
                                                           unntak = "Nei",
                                                           vilkårType = vilkårVurdering.type,
                                                           delvilkårVurderinger = listOf(DelvilkårVurderingDto(vilkårVurdering.delvilkårVurdering.delvilkårVurderinger.first().type,
                                                                                                               VilkårResultat.JA)),
                                                           endretAv = "",
                                                           endretTid = LocalDateTime.now()))
        assertThat(lagretVilkårVurdering.captured.resultat).isEqualTo(VilkårResultat.JA)
        assertThat(lagretVilkårVurdering.captured.begrunnelse).isEqualTo("Ok")
        assertThat(lagretVilkårVurdering.captured.unntak).isEqualTo("Nei")
        assertThat(lagretVilkårVurdering.captured.delvilkårVurdering.delvilkårVurderinger.first().resultat).isEqualTo(VilkårResultat.JA)
        assertThat(lagretVilkårVurdering.captured.type).isEqualTo(vilkårVurdering.type)
    }

    @Test
    internal fun `skal ikke oppdatere vilkårsvurdering hvis behandlingen er låst for videre behandling`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.FERDIGSTILT)
        val vilkårVurdering = vilkårVurdering(BEHANDLING_ID,
                                              resultat = VilkårResultat.IKKE_VURDERT,
                                              VilkårType.FORUTGÅENDE_MEDLEMSKAP)
        every { vilkårVurderingRepository.findByIdOrNull(vilkårVurdering.id) } returns vilkårVurdering

        assertThat(catchThrowable {
            vurderingService.oppdaterVilkår(VilkårVurderingDto(id = vilkårVurdering.id,
                                                               behandlingId = BEHANDLING_ID,
                                                               resultat = VilkårResultat.JA,
                                                               begrunnelse = "Ok",
                                                               unntak = "Nei",
                                                               vilkårType = vilkårVurdering.type,
                                                               endretAv = "",
                                                               endretTid = LocalDateTime.now()
            ))
        }).isInstanceOf(Feil::class.java)
                .hasMessageContaining("er låst for videre redigering")
        verify(exactly = 0) { vilkårVurderingRepository.insertAll(any()) }
    }

    @Test
    fun `skal hente vilkårtyper for inngangsvilkår som mangler vurdering`() {
        val behandling = behandling(fagsak(), true, BehandlingStatus.UTREDES)
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        val ikkeVurdertVilkår = vilkårVurdering(BEHANDLING_ID,
                                                resultat = VilkårResultat.IKKE_VURDERT,
                                                VilkårType.FORUTGÅENDE_MEDLEMSKAP)
        val vurdertVilkår = vilkårVurdering(BEHANDLING_ID,
                                            resultat = VilkårResultat.JA,
                                            VilkårType.LOVLIG_OPPHOLD)
        every { vilkårVurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(ikkeVurdertVilkår, vurdertVilkår)

        val vilkårTyperUtenVurdering = vurderingService.hentInngangsvilkårSomManglerVurdering(BEHANDLING_ID)

        assertThat(vilkårTyperUtenVurdering.size).isEqualTo(1)
        assertThat(vilkårTyperUtenVurdering.first()).isEqualTo(VilkårType.FORUTGÅENDE_MEDLEMSKAP)
    }

    @Test
    fun `skal hente vilkårtyper for inngangsvilkår som ikke finnes`() {
        val behandling = behandling(fagsak(), true, BehandlingStatus.UTREDES)
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        val vurdertVilkår = vilkårVurdering(BEHANDLING_ID,
                                            resultat = VilkårResultat.NEI,
                                            VilkårType.FORUTGÅENDE_MEDLEMSKAP)

        every { vilkårVurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(vurdertVilkår)

        val vilkårTyperUtenVurdering = vurderingService.hentInngangsvilkårSomManglerVurdering(BEHANDLING_ID)

        assertThat(vilkårTyperUtenVurdering.size).isEqualTo(1)
        assertThat(vilkårTyperUtenVurdering.first()).isEqualTo(VilkårType.LOVLIG_OPPHOLD)
    }

    companion object {

        private val BEHANDLING_ID = UUID.randomUUID()
    }
}