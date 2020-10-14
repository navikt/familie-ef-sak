package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.Testsøknad
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
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
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val vurderingService = VurderingService(behandlingService = behandlingService,
                                                    pdlClient = PdlClientConfig().pdlClient(),
                                                    medlemskapMapper = mockk(relaxed = true),
                                                    vilkårsvurderingRepository = vilkårsvurderingRepository)

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentOvergangsstønad(any()) } returns Testsøknad.søknad
    }

    @Test
    fun `skal opprette nye Vilkårsvurdering for alle inngangsvilkår dersom ingen vurderinger finnes`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns emptyList()

        val nyeVilkårsvurderinger = slot<List<Vilkårsvurdering>>()
        every { vilkårsvurderingRepository.insertAll(capture(nyeVilkårsvurderinger)) } answers
                { it.invocation.args.first() as List<Vilkårsvurdering> }
        val inngangsvilkår = Vilkårstype.hentInngangsvilkår()

        vurderingService.hentInngangsvilkår(BEHANDLING_ID)

        assertThat(nyeVilkårsvurderinger.captured).hasSize(inngangsvilkår.size)
        assertThat(nyeVilkårsvurderinger.captured.map { it.type }).containsExactlyElementsOf(inngangsvilkår)
        assertThat(nyeVilkårsvurderinger.captured.map { it.resultat }.toSet()).containsOnly(Vilkårsresultat.IKKE_VURDERT)
        assertThat(nyeVilkårsvurderinger.captured.map { it.behandlingId }.toSet()).containsOnly(BEHANDLING_ID)
    }

    @Test
    fun `skal ikke opprette nye Vilkårsvurderinger for inngangsvilkår som allerede har en vurdering`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(vilkårsvurdering(resultat = Vilkårsresultat.JA,
                                                                                                               type = Vilkårstype.FORUTGÅENDE_MEDLEMSKAP,
                                                                                                               behandlingId = BEHANDLING_ID))

        val nyeVilkårsvurderinger = slot<List<Vilkårsvurdering>>()
        every { vilkårsvurderingRepository.insertAll(capture(nyeVilkårsvurderinger)) } answers
                { it.invocation.args.first() as List<Vilkårsvurdering> }
        val inngangsvilkår = Vilkårstype.hentInngangsvilkår()

        val alleVilkårsvurderinger = vurderingService.hentInngangsvilkår(BEHANDLING_ID).vurderinger

        assertThat(nyeVilkårsvurderinger.captured).hasSize(inngangsvilkår.size - 1)
        assertThat(alleVilkårsvurderinger).hasSize(inngangsvilkår.size)
        assertThat(nyeVilkårsvurderinger.captured.map { it.type }).doesNotContain(Vilkårstype.FORUTGÅENDE_MEDLEMSKAP)
    }

    @Test
    internal fun `skal ikke opprette vilkårsvurderinger hvis behandling er låst for videre vurdering`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.FERDIGSTILT)
        val vilkårsvurderinger = listOf(vilkårsvurdering(resultat = Vilkårsresultat.JA,
                                                         type = Vilkårstype.FORUTGÅENDE_MEDLEMSKAP,
                                                         behandlingId = BEHANDLING_ID))
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns vilkårsvurderinger

        val alleVilkårsvurderinger = vurderingService.hentInngangsvilkår(BEHANDLING_ID).vurderinger

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
                                                                resultat = Vilkårsresultat.JA,
                                                                vilkårstype = Vilkårstype.FORUTGÅENDE_MEDLEMSKAP,
                                                                endretAv = "",
                                                                endretTid = LocalDateTime.now()))
        }).hasMessageContaining("Finner ikke Vilkårsvurdering med id")
    }

    @Test
    internal fun `kan ikke oppdatere vilkårsvurderingen hvis innsendte delvurderinger ikke motsvarerer de som finnes på vilkåret`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        val vilkårsvurdering = vilkårsvurdering(BEHANDLING_ID,
                                                resultat = Vilkårsresultat.IKKE_VURDERT,
                                                type = Vilkårstype.FORUTGÅENDE_MEDLEMSKAP,
                                                delvilkårsvurdering = listOf(Delvilkårsvurdering(DelvilkårsType.DOKUMENTERT_FLYKTNINGSTATUS)))
        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering

        assertThat(catchThrowable {
            vurderingService.oppdaterVilkår(VilkårsvurderingDto(id = vilkårsvurdering.id,
                                                                behandlingId = BEHANDLING_ID,
                                                                resultat = Vilkårsresultat.JA,
                                                                vilkårstype = Vilkårstype.FORUTGÅENDE_MEDLEMSKAP,
                                                                endretAv = "",
                                                                endretTid = LocalDateTime.now()))
        }).hasMessageContaining("Delvilkårstyper motsvarer ikke de som finnes lagrede på vilkåret")
    }

    @Test
    internal fun `skal oppdatere vilkårsvurdering med resultat, begrunnelse og unntak`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        val vilkårsvurdering = vilkårsvurdering(BEHANDLING_ID,
                                                Vilkårsresultat.IKKE_VURDERT,
                                                Vilkårstype.FORUTGÅENDE_MEDLEMSKAP,
                                                listOf(Delvilkårsvurdering(DelvilkårsType.TRE_ÅRS_MEDLEMSKAP,
                                                                           Vilkårsresultat.IKKE_VURDERT)))
        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering
        val lagretVilkårsvurdering = slot<Vilkårsvurdering>()
        every { vilkårsvurderingRepository.update(capture(lagretVilkårsvurdering)) } answers { it.invocation.args.first() as Vilkårsvurdering }

        vurderingService.oppdaterVilkår(VilkårsvurderingDto(id = vilkårsvurdering.id,
                                                            behandlingId = BEHANDLING_ID,
                                                            resultat = Vilkårsresultat.JA,
                                                            begrunnelse = "Ok",
                                                            unntak = "Nei",
                                                            vilkårstype = vilkårsvurdering.type,
                                                            delvilkårsvurderinger = listOf(DelvilkårsvurderingDto(vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger.first().type,
                                                                                                                  Vilkårsresultat.JA)),
                                                            endretAv = "",
                                                            endretTid = LocalDateTime.now()))
        assertThat(lagretVilkårsvurdering.captured.resultat).isEqualTo(Vilkårsresultat.JA)
        assertThat(lagretVilkårsvurdering.captured.begrunnelse).isEqualTo("Ok")
        assertThat(lagretVilkårsvurdering.captured.unntak).isEqualTo("Nei")
        assertThat(lagretVilkårsvurdering.captured.delvilkårsvurdering.delvilkårsvurderinger.first().resultat).isEqualTo(
                Vilkårsresultat.JA)
        assertThat(lagretVilkårsvurdering.captured.type).isEqualTo(vilkårsvurdering.type)
    }

    @Test
    internal fun `skal ikke oppdatere vilkårsvurdering hvis behandlingen er låst for videre behandling`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.FERDIGSTILT)
        val vilkårsvurdering = vilkårsvurdering(BEHANDLING_ID,
                                                resultat = Vilkårsresultat.IKKE_VURDERT,
                                                Vilkårstype.FORUTGÅENDE_MEDLEMSKAP)
        every { vilkårsvurderingRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering

        assertThat(catchThrowable {
            vurderingService.oppdaterVilkår(VilkårsvurderingDto(id = vilkårsvurdering.id,
                                                                behandlingId = BEHANDLING_ID,
                                                                resultat = Vilkårsresultat.JA,
                                                                begrunnelse = "Ok",
                                                                unntak = "Nei",
                                                                vilkårstype = vilkårsvurdering.type,
                                                                endretAv = "",
                                                                endretTid = LocalDateTime.now()
            ))
        }).isInstanceOf(Feil::class.java)
                .hasMessageContaining("er låst for videre redigering")
        verify(exactly = 0) { vilkårsvurderingRepository.insertAll(any()) }
    }

    @Test
    fun `skal hente vilkårtyper for inngangsvilkår som mangler vurdering`() {
        val behandling = behandling(fagsak(), true, BehandlingStatus.UTREDES)
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        val ikkeVurdertVilkår = vilkårsvurdering(BEHANDLING_ID,
                                                 resultat = Vilkårsresultat.IKKE_VURDERT,
                                                 Vilkårstype.FORUTGÅENDE_MEDLEMSKAP)
        val vurdertVilkår = vilkårsvurdering(BEHANDLING_ID,
                                             resultat = Vilkårsresultat.JA,
                                             Vilkårstype.LOVLIG_OPPHOLD)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(ikkeVurdertVilkår, vurdertVilkår)

        val vilkårTyperUtenVurdering = vurderingService.hentInngangsvilkårSomManglerVurdering(BEHANDLING_ID)

        assertThat(vilkårTyperUtenVurdering.size).isEqualTo(1)
        assertThat(vilkårTyperUtenVurdering.first()).isEqualTo(Vilkårstype.FORUTGÅENDE_MEDLEMSKAP)
    }

    @Test
    fun `skal hente vilkårtyper for inngangsvilkår som ikke finnes`() {
        val behandling = behandling(fagsak(), true, BehandlingStatus.UTREDES)
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        val vurdertVilkår = vilkårsvurdering(BEHANDLING_ID,
                                             resultat = Vilkårsresultat.NEI,
                                             Vilkårstype.FORUTGÅENDE_MEDLEMSKAP)

        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(vurdertVilkår)

        val vilkårTyperUtenVurdering = vurderingService.hentInngangsvilkårSomManglerVurdering(BEHANDLING_ID)

        assertThat(vilkårTyperUtenVurdering.size).isEqualTo(1)
        assertThat(vilkårTyperUtenVurdering.first()).isEqualTo(Vilkårstype.LOVLIG_OPPHOLD)
    }

    companion object {

        private val BEHANDLING_ID = UUID.randomUUID()
    }
}