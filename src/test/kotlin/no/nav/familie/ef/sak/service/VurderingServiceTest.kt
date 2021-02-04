package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.PdlClientConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.repository.domain.DelvilkårType.*
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
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
    private val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()
    private val vurderingService = VurderingService(behandlingService = behandlingService,
                                                    pdlClient = PdlClientConfig().pdlClient(),
                                                    medlemskapMapper = mockk(relaxed = true),
                                                    familieIntegrasjonerClient = familieIntegrasjonerClient,
                                                    vilkårsvurderingRepository = vilkårsvurderingRepository)

    @BeforeEach
    fun setUp() {
        val noe = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)
        every { behandlingService.hentOvergangsstønad(any()) }
                .returns(noe)
        every { familieIntegrasjonerClient.hentMedlemskapsinfo(any()) }
                .returns(Medlemskapsinfo(personIdent = noe.fødselsnummer,
                                         gyldigePerioder = emptyList(),
                                         uavklartePerioder = emptyList(),
                                         avvistePerioder = emptyList(),
                ))
    }

    @Test
    fun `skal opprette nye Vilkårsvurdering for alle inngangsvilkår dersom ingen vurderinger finnes`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns emptyList()

        val nyeVilkårsvurderinger = slot<List<Vilkårsvurdering>>()
        every { vilkårsvurderingRepository.insertAll(capture(nyeVilkårsvurderinger)) } answers
                { it.invocation.args.first() as List<Vilkårsvurdering> }
        val inngangsvilkår = VilkårType.hentInngangsvilkår()

        vurderingService.hentInngangsvilkår(BEHANDLING_ID)

        assertThat(nyeVilkårsvurderinger.captured).hasSize(inngangsvilkår.size)
        assertThat(nyeVilkårsvurderinger.captured.map { it.type }).containsExactlyElementsOf(inngangsvilkår)
        assertThat(nyeVilkårsvurderinger.captured.map { it.resultat }.toSet()).containsOnly(Vilkårsresultat.IKKE_VURDERT)
        assertThat(nyeVilkårsvurderinger.captured.map { it.behandlingId }.toSet()).containsOnly(BEHANDLING_ID)
    }

    @Test
    internal fun `skal filtrere bort delvilkår pga grunnlag i søknaden`() {
        val nyeVilkårsvurderinger = slot<List<Vilkårsvurdering>>()
        every { vilkårsvurderingRepository.insertAll(capture(nyeVilkårsvurderinger)) } answers
                { it.invocation.args.first() as List<Vilkårsvurdering> }
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns emptyList()
        val søknad = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)

        every { behandlingService.hentOvergangsstønad(any()) }.returns(søknad)
        vurderingService.hentInngangsvilkår(BEHANDLING_ID)
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
                        LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD
                ))

    }

    @Test
    fun `skal ikke opprette nye Vilkårsvurderinger for inngangsvilkår som allerede har en vurdering`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.OPPRETTET)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns
                listOf(vilkårsvurdering(resultat = Vilkårsresultat.JA,
                                        type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                        behandlingId = BEHANDLING_ID))

        val nyeVilkårsvurderinger = slot<List<Vilkårsvurdering>>()
        every { vilkårsvurderingRepository.insertAll(capture(nyeVilkårsvurderinger)) } answers
                { it.invocation.args.first() as List<Vilkårsvurdering> }
        val inngangsvilkår = VilkårType.hentInngangsvilkår()

        val alleVilkårsvurderinger = vurderingService.hentInngangsvilkår(BEHANDLING_ID).vurderinger

        assertThat(nyeVilkårsvurderinger.captured).hasSize(inngangsvilkår.size - 1)
        assertThat(alleVilkårsvurderinger).hasSize(inngangsvilkår.size)
        assertThat(nyeVilkårsvurderinger.captured.map { it.type }).doesNotContain(VilkårType.FORUTGÅENDE_MEDLEMSKAP)
    }

    @Test
    internal fun `skal ikke opprette vilkårsvurderinger hvis behandling er låst for videre vurdering`() {
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling(fagsak(), true, BehandlingStatus.FERDIGSTILT)
        val vilkårsvurderinger = listOf(vilkårsvurdering(resultat = Vilkårsresultat.JA,
                                                         type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
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
                                                                resultat = Vilkårsresultat.JA,
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
                                                            resultat = Vilkårsresultat.JA,
                                                            begrunnelse = "Ok",
                                                            unntak = "Nei",
                                                            vilkårType = vilkårsvurdering.type,
                                                            delvilkårsvurderinger =
                                                            listOf(DelvilkårsvurderingDto(vilkårsvurdering.delvilkårsvurdering
                                                                                                  .delvilkårsvurderinger
                                                                                                  .first().type,
                                                                                          Vilkårsresultat.JA)),
                                                            endretAv = "",
                                                            endretTid = LocalDateTime.now()))
        assertThat(lagretVilkårsvurdering.captured.resultat).isEqualTo(Vilkårsresultat.JA)
        assertThat(lagretVilkårsvurdering.captured.begrunnelse).isEqualTo("Ok")
        assertThat(lagretVilkårsvurdering.captured.unntak).isEqualTo("Nei")
        assertThat(lagretVilkårsvurdering.captured.delvilkårsvurdering.delvilkårsvurderinger.first().resultat)
                .isEqualTo(Vilkårsresultat.JA)
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
                                                               Vilkårsresultat.JA,
                                                               vilkårsvurdering.type,
                                                               null,
                                                               null,
                                                               null,
                                                               "jens123@trugdeetaten.no",
                                                               LocalDateTime.now(),
                                                               listOf(DelvilkårsvurderingDto(LEVER_IKKE_MED_ANNEN_FORELDER,
                                                                                             Vilkårsresultat.JA,
                                                                                             "Delvilkår ok")))
        vurderingService.oppdaterVilkår(oppdatertVilkårsvurderingDto)

        assertThat(lagretVilkårsvurdering.captured.delvilkårsvurdering.delvilkårsvurderinger.first().resultat).isEqualTo(Vilkårsresultat.JA)
        assertThat(lagretVilkårsvurdering.captured.delvilkårsvurdering.delvilkårsvurderinger.first().begrunnelse).isEqualTo("Delvilkår ok")
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
                                                                resultat = Vilkårsresultat.JA,
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
    fun `skal hente vilkårtyper for inngangsvilkår som mangler vurdering`() {
        val behandling = behandling(fagsak(), true, BehandlingStatus.UTREDES)
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        val ikkeVurdertVilkår = vilkårsvurdering(BEHANDLING_ID,
                                                 resultat = Vilkårsresultat.IKKE_VURDERT,
                                                 VilkårType.FORUTGÅENDE_MEDLEMSKAP)
        val vurdertVilkår = vilkårsvurdering(BEHANDLING_ID,
                                             resultat = Vilkårsresultat.JA,
                                             VilkårType.LOVLIG_OPPHOLD)
        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(ikkeVurdertVilkår, vurdertVilkår)

        val vilkårTyperUtenVurdering = vurderingService.hentInngangsvilkårSomManglerVurdering(BEHANDLING_ID)

        val vilkårtyper = VilkårType.hentInngangsvilkår().filterNot { it === VilkårType.LOVLIG_OPPHOLD }
        assertThat(vilkårTyperUtenVurdering).containsExactlyInAnyOrderElementsOf(vilkårtyper)
    }

    @Test
    fun `skal hente vilkårtyper for inngangsvilkår som ikke finnes`() {
        val behandling = behandling(fagsak(), true, BehandlingStatus.UTREDES)
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        val vurdertVilkår = vilkårsvurdering(BEHANDLING_ID,
                                             resultat = Vilkårsresultat.NEI,
                                             VilkårType.FORUTGÅENDE_MEDLEMSKAP)

        every { vilkårsvurderingRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(vurdertVilkår)

        val vilkårTyperUtenVurdering = vurderingService.hentInngangsvilkårSomManglerVurdering(BEHANDLING_ID)

        val vilkårtyper = VilkårType.hentInngangsvilkår().filterNot { it === VilkårType.FORUTGÅENDE_MEDLEMSKAP }
        assertThat(vilkårTyperUtenVurdering).containsExactlyInAnyOrderElementsOf(vilkårtyper)
    }

    companion object {

        private val BEHANDLING_ID = UUID.randomUUID()
        private const val BEHANDLING_EKSTERN_ID = 12345L
    }
}