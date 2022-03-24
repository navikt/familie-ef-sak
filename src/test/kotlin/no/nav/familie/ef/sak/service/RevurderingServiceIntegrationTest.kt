package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.RevurderingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.RevurderingBarnDto
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadRepository
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.testutil.søknadsBarnTilBehandlingBarn
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AleneomsorgRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class RevurderingServiceIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var revurderingService: RevurderingService
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository
    @Autowired lateinit var søknadService: SøknadService
    @Autowired lateinit var barnRepository: BarnRepository
    @Autowired lateinit var søknadRepository: SøknadRepository

    private lateinit var fagsak: Fagsak
    private val personIdent = "123456789012"
    private val behandlingsårsak = BehandlingÅrsak.NYE_OPPLYSNINGER
    private val kravMottatt = LocalDate.of(2021, 9, 9)
    private lateinit var revurderingDto: RevurderingDto


    @BeforeEach
    fun setUp() {
        BrukerContextUtil.mockBrukerContext("Heider")
        val identer = fagsakpersoner(setOf(personIdent))
        fagsak = testoppsettService.lagreFagsak(fagsak(identer = identer))
        revurderingDto = RevurderingDto(fagsak.id, behandlingsårsak, kravMottatt, emptyList())
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal opprette revurdering`() {
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak,
                                                                status = BehandlingStatus.FERDIGSTILT,
                                                                resultat = BehandlingResultat.INNVILGET))
        val søknad = lagreSøknad(behandling)
        opprettVilkår(behandling, søknad)

        val opprettRevurderingManuelt = revurderingService.opprettRevurderingManuelt(revurderingDto)

        val revurdering = behandlingRepository.findByIdOrThrow(opprettRevurderingManuelt.id)
        assertThat(revurdering.type).isEqualTo(BehandlingType.REVURDERING)
    }

    /**
     * Behandling 1: Avslått og ferdigstilt
     * Behandling 2: Revurdering, som bruker søknaden til behandling 1 då den er den siste som er ferdigstilt, men fortsatt avslått
     */
    @Test
    internal fun `skal opprette revurdering med en avslått førstegangsbehandling`() {
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak,
                                                                status = BehandlingStatus.FERDIGSTILT,
                                                                resultat = BehandlingResultat.AVSLÅTT))
        opprettVilkår(behandling, lagreSøknad(behandling))

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingDto)

        assertThat(getSøknadsskjemaId(revurdering)).isEqualTo(getSøknadsskjemaId(behandling))
    }

    /**
     * Behandling 1: Innvilget og ferdigstilt
     * Behandling 2: Avslått revurdering med egen søknad
     * Behandling 3: Revurdering, som bruker søknaden til behandling 1
     */
    @Test
    internal fun `skal peke til forrige iverksatte behandling hvis den finnes`() {
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak,
                                                                status = BehandlingStatus.FERDIGSTILT,
                                                                resultat = BehandlingResultat.INNVILGET))
        opprettVilkår(behandling, lagreSøknad(behandling))

        val revurdering1 = behandlingRepository.insert(behandling(fagsak = fagsak,
                                                                  type = BehandlingType.REVURDERING,
                                                                  status = BehandlingStatus.FERDIGSTILT,
                                                                  resultat = BehandlingResultat.AVSLÅTT))
        opprettVilkår(behandling, lagreSøknad(revurdering1))

        val revurdering2 = revurderingService.opprettRevurderingManuelt(revurderingDto)

        val soknadsskjemaId = getSøknadsskjemaId(revurdering2)
        assertThat(soknadsskjemaId).isEqualTo(getSøknadsskjemaId(behandling))
        assertThat(soknadsskjemaId).isNotEqualTo(getSøknadsskjemaId(revurdering1))
    }

    @Test
    internal fun `revurdering - skal kopiere vilkår`() {
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak,
                                                                status = BehandlingStatus.FERDIGSTILT,
                                                                resultat = BehandlingResultat.INNVILGET))
        val søknad = lagreSøknad(behandling)
        opprettVilkår(behandling, søknad)

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingDto)
        val sivilstandVilkårForBehandling = vilkårsvurderingRepository.findByBehandlingId(behandling.id).first { it.type == VilkårType.SIVILSTAND }
        val sivilstandVilkårForRevurdering = vilkårsvurderingRepository.findByBehandlingId(revurdering.id).first { it.type == VilkårType.SIVILSTAND }
        val aleneomsorgVilkårForBehandling = vilkårsvurderingRepository.findByBehandlingId(behandling.id).first { it.type == VilkårType.ALENEOMSORG }
        val aleneomsorgVilkårForRevurdering = vilkårsvurderingRepository.findByBehandlingId(revurdering.id).first { it.type == VilkårType.ALENEOMSORG }
        val barnPåBehandling = barnRepository.findByBehandlingId(revurdering.id).first()

        assertThat(sivilstandVilkårForBehandling.id).isNotEqualTo(sivilstandVilkårForRevurdering.id)
        assertThat(sivilstandVilkårForBehandling.behandlingId).isNotEqualTo(sivilstandVilkårForRevurdering.behandlingId)
        assertThat(sivilstandVilkårForBehandling.sporbar.opprettetTid).isNotEqualTo(sivilstandVilkårForRevurdering.sporbar.opprettetTid)
        assertThat(sivilstandVilkårForBehandling.sporbar.endret.endretTid).isEqualTo(sivilstandVilkårForRevurdering.sporbar.endret.endretTid)
        assertThat(sivilstandVilkårForRevurdering.barnId).isNull()
        assertThat(sivilstandVilkårForBehandling.barnId).isNull()

        assertThat(aleneomsorgVilkårForBehandling.id).isNotEqualTo(aleneomsorgVilkårForRevurdering.id)
        assertThat(aleneomsorgVilkårForBehandling.barnId).isNotEqualTo(aleneomsorgVilkårForRevurdering.barnId)
        assertThat(aleneomsorgVilkårForBehandling.behandlingId).isNotEqualTo(aleneomsorgVilkårForRevurdering.behandlingId)
        assertThat(aleneomsorgVilkårForBehandling.sporbar.opprettetTid).isNotEqualTo(aleneomsorgVilkårForRevurdering.sporbar.opprettetTid)
        assertThat(aleneomsorgVilkårForBehandling.sporbar.endret.endretTid).isEqualTo(aleneomsorgVilkårForRevurdering.sporbar.endret.endretTid)
        assertThat(aleneomsorgVilkårForBehandling.barnId).isNotNull
        assertThat(aleneomsorgVilkårForRevurdering.barnId).isEqualTo(barnPåBehandling.id)


        assertThat(sivilstandVilkårForBehandling).usingRecursiveComparison().ignoringFields("id", "sporbar", "behandlingId", "barnId")
                .isEqualTo(sivilstandVilkårForRevurdering)
        assertThat(aleneomsorgVilkårForBehandling).usingRecursiveComparison().ignoringFields("id", "sporbar", "behandlingId", "barnId")
                .isEqualTo(aleneomsorgVilkårForRevurdering)
    }

    @Test
    internal fun `revurdering med nye barn - skal kopiere vilkår`() {
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak,
                                                                status = BehandlingStatus.FERDIGSTILT,
                                                                resultat = BehandlingResultat.INNVILGET))
        val søknad = lagreSøknad(behandling)
        opprettVilkår(behandling, søknad)
        val nyttBarn = RevurderingBarnDto(personIdent = "44445555666")

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingDto.copy(barn = listOf(nyttBarn)))
        val vilkårForBehandling = vilkårsvurderingRepository.findByBehandlingId(behandling.id)
        val vilkårForRevurdering = vilkårsvurderingRepository.findByBehandlingId(revurdering.id)
        val barnPåBehandling = barnRepository.findByBehandlingId(revurdering.id)

        assertThat(vilkårForBehandling).hasSize(2)
        assertThat(vilkårForRevurdering).hasSize(3)
        assertThat(vilkårForBehandling.filter { it.barnId != null }).hasSize(1)
        assertThat(vilkårForRevurdering.filter { it.barnId != null }).hasSize(2)
        assertThat(vilkårForBehandling.mapNotNull { it.barnId }).isNotIn(barnPåBehandling.map { it.id })
        assertThat(vilkårForRevurdering.mapNotNull { it.barnId }.sorted()).isEqualTo(barnPåBehandling.map { it.id }.sorted())
        assertThat(vilkårForBehandling.map { it.behandlingId }).isNotIn(vilkårForRevurdering.map { it.behandlingId })
        assertThat(vilkårForBehandling.map { it.sporbar.opprettetTid }).isNotIn(vilkårForRevurdering.map { it.sporbar.opprettetTid })

        assertThat(vilkårForBehandling.first { it.type == VilkårType.SIVILSTAND }).usingRecursiveComparison()
                .ignoringFields("id", "sporbar", "behandlingId", "barnId")
                .isEqualTo(vilkårForRevurdering.first { it.type == VilkårType.SIVILSTAND })
    }

    @Test
    internal fun `skal ikke være mulig å opprette fagsak hvis siste behandling ikke er ferdig`() {
        behandlingRepository.insert(behandling(fagsak = fagsak, status = BehandlingStatus.UTREDES))

        assertThat(catchThrowable { revurderingService.opprettRevurderingManuelt(revurderingDto) })
                .hasMessageContaining("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
    }

    @Test
    internal fun `skal ikke være mulig å opprette fagsak hvis det ikke finnes en behandling fra før`() {
        assertThat(catchThrowable { revurderingService.opprettRevurderingManuelt(revurderingDto) })
                .hasMessageContaining("Det finnes ikke en tidligere behandling på fagsaken")
    }

    private fun getSøknadsskjemaId(revurdering1: Behandling) =
            søknadRepository.findByBehandlingId(revurdering1.id)!!.soknadsskjemaId

    private fun lagreSøknad(behandling: Behandling): SøknadsskjemaOvergangsstønad {
        val søknad = TestsøknadBuilder.Builder().setBarn(listOf(
                TestsøknadBuilder.Builder()
                        .defaultBarn("Navn navnesen", "27062188745", fødselTermindato = LocalDate.of(2021, 6, 27))
        )).build().søknadOvergangsstønad
        søknadService.lagreSøknadForOvergangsstønad(søknad, behandling.id, behandling.fagsakId, "1L")
        val overgangsstønad = søknadService.hentOvergangsstønad(behandling.id) ?: error("Fant ikke overgangsstønad for testen")
        barnRepository.insertAll(søknadsBarnTilBehandlingBarn(overgangsstønad.barn, behandling.id))
        return overgangsstønad
    }

    private fun opprettVilkår(behandling: Behandling,
                              søknad: SøknadsskjemaOvergangsstønad) {
        val barn = barnRepository.findByBehandlingId(behandling.id)
        val delvilkårsvurdering =
                SivilstandRegel().initereDelvilkårsvurdering(HovedregelMetadata(søknad.sivilstand,
                                                                                Sivilstandstype.ENKE_ELLER_ENKEMANN,
                                                                                barn = emptyList(),
                                                                                søktOmBarnetilsyn = emptyList()))

        val delvilkårsvurderingAleneomsorg =
                AleneomsorgRegel().initereDelvilkårsvurdering(HovedregelMetadata(søknad.sivilstand,
                                                                                 Sivilstandstype.ENKE_ELLER_ENKEMANN,
                                                                                 barn = barn, søktOmBarnetilsyn = emptyList()))
        vilkårsvurderingRepository.insertAll(listOf(vilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                                                     type = VilkårType.SIVILSTAND,
                                                                     behandlingId = behandling.id,
                                                                     delvilkårsvurdering = delvilkårsvurdering),
                                                    vilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                                                     type = VilkårType.ALENEOMSORG,
                                                                     behandlingId = behandling.id,
                                                                     barnId = barn.first().id,
                                                                     delvilkårsvurdering = delvilkårsvurderingAleneomsorg)))
    }
}