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
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRepository
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadRepository
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaBarnetilsyn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.repository.vedtaksperiode
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.testutil.søknadBarnTilBehandlingBarn
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AleneomsorgRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class RevurderingServiceIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var revurderingService: RevurderingService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    lateinit var søknadService: SøknadService

    @Autowired
    lateinit var barnRepository: BarnRepository

    @Autowired
    lateinit var søknadRepository: SøknadRepository

    @Autowired
    lateinit var vedtakService: VedtakService

    @Autowired
    lateinit var grunnlagsdataRepository: GrunnlagsdataRepository

    @Autowired
    private lateinit var beregnYtelseSteg: BeregnYtelseSteg

    private lateinit var fagsak: Fagsak
    private val personIdent = "123456789012"
    private val behandlingsårsak = BehandlingÅrsak.NYE_OPPLYSNINGER
    private val kravMottatt = LocalDate.of(2021, 9, 9)
    private lateinit var revurderingDto: RevurderingDto
    private val identer = fagsakpersoner(setOf(personIdent))

    @BeforeEach
    fun setUp() {
        BrukerContextUtil.mockBrukerContext("Heider")
        fagsak = testoppsettService.lagreFagsak(fagsak(identer = identer))
        revurderingDto = RevurderingDto(fagsak.id, behandlingsårsak, kravMottatt, emptyList())
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal opprette revurdering`() {
        val behandling = opprettFerdigstiltBehandling(fagsak)
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
        val behandling = opprettFerdigstiltBehandling(fagsak, BehandlingResultat.AVSLÅTT)
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
        val behandling = opprettFerdigstiltBehandling(fagsak)
        opprettVilkår(behandling, lagreSøknad(behandling))

        val revurdering1 = behandlingRepository.insert(
            behandling(
                fagsak = fagsak,
                type = BehandlingType.REVURDERING,
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.AVSLÅTT
            )
        )
        opprettVilkår(behandling, lagreSøknad(revurdering1))

        val revurdering2 = revurderingService.opprettRevurderingManuelt(revurderingDto)

        val soknadsskjemaId = getSøknadsskjemaId(revurdering2)
        assertThat(soknadsskjemaId).isEqualTo(getSøknadsskjemaId(behandling))
        assertThat(soknadsskjemaId).isNotEqualTo(getSøknadsskjemaId(revurdering1))
    }

    @Test
    internal fun `revurdering - skal kopiere vilkår`() {
        val behandling = opprettFerdigstiltBehandling(fagsak)
        val søknad = lagreSøknad(behandling)
        opprettVilkår(behandling, søknad)

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingDto)
        val sivilstandVilkårForBehandling =
            vilkårsvurderingRepository.findByBehandlingId(behandling.id).first { it.type == VilkårType.SIVILSTAND }
        val sivilstandVilkårForRevurdering =
            vilkårsvurderingRepository.findByBehandlingId(revurdering.id).first { it.type == VilkårType.SIVILSTAND }
        val aleneomsorgVilkårForBehandling =
            vilkårsvurderingRepository.findByBehandlingId(behandling.id).first { it.type == VilkårType.ALENEOMSORG }
        val aleneomsorgVilkårForRevurdering =
            vilkårsvurderingRepository.findByBehandlingId(revurdering.id).first { it.type == VilkårType.ALENEOMSORG }
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

        assertThat(sivilstandVilkårForBehandling).usingRecursiveComparison()
            .ignoringFields("id", "sporbar", "behandlingId", "barnId")
            .isEqualTo(sivilstandVilkårForRevurdering)
        assertThat(aleneomsorgVilkårForBehandling).usingRecursiveComparison()
            .ignoringFields("id", "sporbar", "behandlingId", "barnId")
            .isEqualTo(aleneomsorgVilkårForRevurdering)
    }

    // TODO flytte til unittest?
    @Test
    internal fun `revurdering - skal kaste feil dersom satsendring på overgangsstønad`() {
        val behandling = opprettFerdigstiltBehandling(fagsak)
        val søknad = lagreSøknad(behandling)
        opprettVilkår(behandling, søknad)
        val feil = assertThrows<Feil> { revurderingService.opprettRevurderingManuelt(revurderingDto.copy(behandlingsårsak = BehandlingÅrsak.SATSENDRING)) }
        assertThat(feil.message).isEqualTo("Kan ikke opprette revurdering med årsak satsendring for OVERGANGSSTØNAD")
    }

    @Test
    internal fun `revurdering - skal kopiere vedtak ved satsendring`() {
        val fagsakBarnetilsyn = testoppsettService.lagreFagsak(fagsak(identer = identer, stønadstype = StønadType.BARNETILSYN))
        revurderingDto = RevurderingDto(fagsakBarnetilsyn.id, BehandlingÅrsak.SATSENDRING, kravMottatt, emptyList())

        val behandling = behandling(fagsakBarnetilsyn)
        behandlingRepository.insert(behandling)
        val søknad = lagreSøknadForBarnetilsyn(behandling)
        opprettVilkårForBarnetilsyn(behandling, søknad)
        val vedtak = vedtak(behandling.id, perioder = PeriodeWrapper(listOf(vedtaksperiode(sluttDato = LocalDate.of(2023, 12, 1)))))

        beregnYtelseSteg.utførSteg(saksbehandling(fagsak, behandling), vedtak.tilVedtakDto())
        behandlingRepository.update(
            behandling.copy(
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.INNVILGET,
                vedtakstidspunkt = SporbarUtils.now()
            )
        )

        val revurdering =
            revurderingService.opprettRevurderingManuelt(revurderingDto.copy(behandlingsårsak = BehandlingÅrsak.SATSENDRING))

        val forrigeVedtak = vedtakService.hentVedtak(behandling.id)
        val nyttVedtak = vedtakService.hentVedtak(revurdering.id)

        assertThat(forrigeVedtak.barnetilsyn?.perioder?.size).isEqualTo(nyttVedtak.barnetilsyn?.perioder?.size)
    }

    @Test
    internal fun `revurdering med nye barn - skal kopiere vilkår`() {
        val behandling = opprettFerdigstiltBehandling(fagsak)
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
        assertThat(vilkårForRevurdering.mapNotNull { it.barnId }.sorted()).isEqualTo(
            barnPåBehandling.map { it.id }
                .sorted()
        )
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

    @Test
    internal fun `kan ikke opprette g-omregning for barnetilsyn eller skolepenger`() {
        listOf(StønadType.BARNETILSYN, StønadType.SKOLEPENGER).forEach {
            val fagsak = testoppsettService.lagreFagsak(fagsak(identer = identer, stønadstype = it))
            val behandling = opprettFerdigstiltBehandling(fagsak)
            opprettVilkår(behandling, lagreSøknad(behandling))
            val revurderingInnhold = RevurderingDto(fagsak.id, BehandlingÅrsak.G_OMREGNING, kravMottatt, emptyList())

            assertThatThrownBy {
                revurderingService.opprettRevurderingManuelt(revurderingInnhold)
            }.hasMessageContaining("Kan ikke opprette revurdering med årsak g-omregning")
        }
    }

    @Test
    internal fun `kan opprette g-omregning for overgangsstønad`() {
        val behandling = opprettFerdigstiltBehandling(fagsak)
        opprettVilkår(behandling, lagreSøknad(behandling))
        val revurderingInnhold = RevurderingDto(fagsak.id, BehandlingÅrsak.G_OMREGNING, kravMottatt, emptyList())

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingInnhold)

        assertThat(revurdering.årsak).isEqualTo(BehandlingÅrsak.G_OMREGNING)
    }

    private fun opprettFerdigstiltBehandling(
        fagsak: Fagsak,
        resultat: BehandlingResultat = BehandlingResultat.INNVILGET
    ) = behandlingRepository.insert(
        behandling(
            fagsak = fagsak,
            status = BehandlingStatus.FERDIGSTILT,
            resultat = resultat
        )
    )

    private fun getSøknadsskjemaId(revurdering1: Behandling) =
        søknadRepository.findByBehandlingId(revurdering1.id)!!.soknadsskjemaId

    private fun lagreSøknad(behandling: Behandling): SøknadsskjemaOvergangsstønad {
        val søknad = TestsøknadBuilder.Builder().setBarn(
            listOf(
                TestsøknadBuilder.Builder()
                    .defaultBarn("Navn navnesen", "27062188745", fødselTermindato = LocalDate.of(2021, 6, 27))
            )
        ).build().søknadOvergangsstønad
        søknadService.lagreSøknadForOvergangsstønad(søknad, behandling.id, behandling.fagsakId, "1L")
        val overgangsstønad =
            søknadService.hentOvergangsstønad(behandling.id) ?: error("Fant ikke overgangsstønad for testen")
        barnRepository.insertAll(søknadBarnTilBehandlingBarn(overgangsstønad.barn, behandling.id))
        return overgangsstønad
    }

    private fun lagreSøknadForBarnetilsyn(behandling: Behandling): SøknadsskjemaBarnetilsyn {
        val søknad = TestsøknadBuilder.Builder().setBarn(
            listOf(
                TestsøknadBuilder.Builder().defaultBarn("any", PdlClientConfig.barnFnr),
                TestsøknadBuilder.Builder().defaultBarn("any", PdlClientConfig.barn2Fnr)
            )
        ).build().søknadBarnetilsyn
        søknadService.lagreSøknadForBarnetilsyn(søknad, behandling.id, behandling.fagsakId, "1L")
        val barnetilsyn = søknadService.hentBarnetilsyn(behandling.id) ?: error("Fant ikke overgangsstønad for testen")
        barnRepository.insertAll(søknadBarnTilBehandlingBarn(barnetilsyn.barn, behandling.id))
        return barnetilsyn
    }

    private fun opprettVilkårForBarnetilsyn(
        behandling: Behandling,
        søknad: SøknadsskjemaBarnetilsyn
    ) {
        val barn = barnRepository.findByBehandlingId(behandling.id)
        val delvilkårsvurdering =
            SivilstandRegel().initiereDelvilkårsvurdering(
                HovedregelMetadata(
                    søknad.sivilstand,
                    Sivilstandstype.ENKE_ELLER_ENKEMANN,
                    barn = emptyList(),
                    søktOmBarnetilsyn = emptyList()
                )
            )

        val delvilkårsvurderingAleneomsorg =
            AleneomsorgRegel().initiereDelvilkårsvurdering(
                HovedregelMetadata(
                    søknad.sivilstand,
                    Sivilstandstype.ENKE_ELLER_ENKEMANN,
                    barn = barn,
                    søktOmBarnetilsyn = emptyList()
                )
            )
        vilkårsvurderingRepository.insertAll(
            listOf(
                vilkårsvurdering(
                    resultat = Vilkårsresultat.OPPFYLT,
                    type = VilkårType.SIVILSTAND,
                    behandlingId = behandling.id,
                    delvilkårsvurdering = delvilkårsvurdering
                ),
                vilkårsvurdering(
                    resultat = Vilkårsresultat.OPPFYLT,
                    type = VilkårType.ALENEOMSORG,
                    behandlingId = behandling.id,
                    barnId = barn.first().id,
                    delvilkårsvurdering = delvilkårsvurderingAleneomsorg
                )
            )
        )
    }

    private fun opprettVilkår(
        behandling: Behandling,
        søknad: SøknadsskjemaOvergangsstønad
    ) {
        val barn = barnRepository.findByBehandlingId(behandling.id)
        val delvilkårsvurdering =
            SivilstandRegel().initiereDelvilkårsvurdering(
                HovedregelMetadata(
                    søknad.sivilstand,
                    Sivilstandstype.ENKE_ELLER_ENKEMANN,
                    barn = emptyList(),
                    søktOmBarnetilsyn = emptyList()
                )
            )

        val delvilkårsvurderingAleneomsorg =
            AleneomsorgRegel().initiereDelvilkårsvurdering(
                HovedregelMetadata(
                    søknad.sivilstand,
                    Sivilstandstype.ENKE_ELLER_ENKEMANN,
                    barn = barn,
                    søktOmBarnetilsyn = emptyList()
                )
            )
        vilkårsvurderingRepository.insertAll(
            listOf(
                vilkårsvurdering(
                    resultat = Vilkårsresultat.OPPFYLT,
                    type = VilkårType.SIVILSTAND,
                    behandlingId = behandling.id,
                    delvilkårsvurdering = delvilkårsvurdering
                ),
                vilkårsvurdering(
                    resultat = Vilkårsresultat.OPPFYLT,
                    type = VilkårType.ALENEOMSORG,
                    behandlingId = behandling.id,
                    barnId = barn.first().id,
                    delvilkårsvurdering = delvilkårsvurderingAleneomsorg
                )
            )
        )
    }
}
