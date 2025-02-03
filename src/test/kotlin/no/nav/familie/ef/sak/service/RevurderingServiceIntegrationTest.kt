package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.RevurderingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadRepository
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstand
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaBarnetilsyn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.vedtakBarnetilsyn
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.testutil.søknadBarnTilBehandlingBarn
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.KontantstøtteWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeMedBeløp
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.Opphavsvilkår
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AleneomsorgRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.søknad.Barn
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

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
    lateinit var grunnlagsdataService: GrunnlagsdataService

    @Autowired
    private lateinit var beregnYtelseSteg: BeregnYtelseSteg

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    private lateinit var fagsak: Fagsak
    private lateinit var fagsakBarnetilsyn: Fagsak
    private lateinit var fagsakSkolepenger: Fagsak
    private val personIdent = "123456789012"
    private val behandlingsårsak = BehandlingÅrsak.NYE_OPPLYSNINGER
    private val kravMottatt = LocalDate.of(2021, 9, 9)
    private lateinit var revurderingDto: RevurderingDto
    private val identer = fagsakpersoner(setOf(personIdent))

    @BeforeEach
    fun setUp() {
        mockBrukerContext(preferredUsername = "Heider", groups = listOf(rolleConfig.saksbehandlerRolle))
        fagsak = testoppsettService.lagreFagsak(fagsak(identer = identer))
        fagsakBarnetilsyn = testoppsettService.lagreFagsak(fagsak(identer = identer, stønadstype = StønadType.BARNETILSYN))
        fagsakSkolepenger = testoppsettService.lagreFagsak(fagsak(identer = identer, stønadstype = StønadType.SKOLEPENGER))
        revurderingDto = RevurderingDto(fagsak.id, behandlingsårsak, kravMottatt, VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE)
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal opprette revurdering`() {
        val behandling = opprettFerdigstiltBehandling(fagsak)
        opprettVilkår(behandling, lagreSøknad(behandling).sivilstand)

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
        opprettVilkår(behandling, lagreSøknad(behandling).sivilstand)

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
        opprettVilkår(behandling, lagreSøknad(behandling).sivilstand)

        val revurdering1 =
            behandlingRepository.insert(
                behandling(
                    fagsak = fagsak,
                    type = BehandlingType.REVURDERING,
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.AVSLÅTT,
                ),
            )
        grunnlagsdataService.opprettGrunnlagsdata(revurdering1.id)
        opprettVilkår(revurdering1, lagreSøknad(revurdering1).sivilstand)
        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        val revurdering2 = revurderingService.opprettRevurderingManuelt(revurderingDto)

        val soknadsskjemaId = getSøknadsskjemaId(revurdering2)
        assertThat(soknadsskjemaId).isEqualTo(getSøknadsskjemaId(behandling))
        assertThat(soknadsskjemaId).isNotEqualTo(getSøknadsskjemaId(revurdering1))
    }

    @Test
    internal fun `revurdering - skal kopiere vilkår`() {
        val behandling = opprettFerdigstiltBehandling(fagsak)
        opprettVilkår(behandling, lagreSøknad(behandling).sivilstand)

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingDto)
        val vilkårBehandling = vilkårsvurderingRepository.findByBehandlingId(behandling.id)
        val vilkårRevurdering = vilkårsvurderingRepository.findByBehandlingId(revurdering.id)
        val barnPåBehandlingRevurdering =
            barnRepository.findByBehandlingId(revurdering.id).first {
                it.navn.equals("Barn Barnesen")
            }
        val barnPåBehandling =
            barnRepository.findByBehandlingId(behandling.id).first {
                it.navn.equals("Barn Barnesen")
            }

        val sivilstandVilkårForBehandling = vilkårBehandling.first { it.type == VilkårType.SIVILSTAND }
        val sivilstandVilkårForRevurdering = vilkårRevurdering.first { it.type == VilkårType.SIVILSTAND }
        val aleneomsorgVilkårForBehandling =
            vilkårBehandling.first { it.type == VilkårType.ALENEOMSORG && barnPåBehandling.id == it.barnId }
        val aleneomsorgVilkårForRevurdering =
            vilkårRevurdering.first { it.type == VilkårType.ALENEOMSORG && barnPåBehandlingRevurdering.id == it.barnId }

        assertThat(vilkårRevurdering).hasSize(vilkårBehandling.size)

        validerVilkårUtenBarn(sivilstandVilkårForBehandling, sivilstandVilkårForRevurdering, behandling)

        validerVilkårMedBarn(
            aleneomsorgVilkårForBehandling,
            aleneomsorgVilkårForRevurdering,
            barnPåBehandlingRevurdering,
            behandling,
        )

        assertThat(sivilstandVilkårForBehandling)
            .usingRecursiveComparison()
            .ignoringFields("id", "sporbar", "behandlingId", "barnId", "opphavsvilkår")
            .isEqualTo(sivilstandVilkårForRevurdering)
        assertThat(aleneomsorgVilkårForBehandling)
            .usingRecursiveComparison()
            .ignoringFields("id", "sporbar", "behandlingId", "barnId", "opphavsvilkår")
            .isEqualTo(aleneomsorgVilkårForRevurdering)
    }

    @Test
    internal fun `revurdering - skal kopiere vedtak ved satsendring`() {
        val (fagsakBarnetilsyn, behandling) = opprettBarnetilsynBehandling()
        val vedtak =
            vedtakBarnetilsyn(
                behandlingId = behandling.id,
                barn = barnRepository.findByBehandlingId(behandling.id).map { it.id },
                beløp = 8000,
                kontantstøtteWrapper =
                    KontantstøtteWrapper(
                        listOf(
                            PeriodeMedBeløp(
                                Månedsperiode(
                                    YearMonth.of(2025, 9),
                                    YearMonth.of(2025, 10),
                                ),
                                1000,
                            ),
                        ),
                        null,
                    ),
                fom = YearMonth.of(2024, 6),
                tom = YearMonth.of(2025, 12),
            )
        ferdigstillVedtak(vedtak, behandling, fagsakBarnetilsyn)

        val revurdering =
            revurderingService.opprettRevurderingManuelt(revurderingDto.copy(behandlingsårsak = BehandlingÅrsak.SATSENDRING))

        val nyttVedtak = vedtakService.hentVedtak(revurdering.id)
        val barnetilsynPerioder = nyttVedtak.barnetilsyn?.perioder
        assertThat(barnetilsynPerioder?.size).isEqualTo(3) // 3 perioder: Før, under og etter kontantstøtte-periode
        assertThat(barnetilsynPerioder?.first()?.utgifter).isEqualTo(8000)
        assertThat(barnetilsynPerioder?.first()?.barn?.size).isEqualTo(2)
        assertThat(barnetilsynPerioder?.first()?.periode?.fom).isEqualTo(YearMonth.of(2025, 1))
    }

    /**
     * B1 - Førstegangsbehandling overgangsstønad
     * B2 - Førstegangsbehandling barnetilsyn
     * B3 - Revurdering overgangsstønad - inngangsvilkår fra barnetilsyn, aktivitetsvilkår fra overgangsstønad
     */
    @Test
    fun `revurdering - skal kopiere vilkår fra nyeste behandling fra annen stønad`() {
        val førstegangsbehandlingOS = opprettFerdigstiltBehandling(fagsak)
        opprettVilkår(førstegangsbehandlingOS, lagreSøknad(førstegangsbehandlingOS).sivilstand)
        grunnlagsdataService.opprettGrunnlagsdata(førstegangsbehandlingOS.id)
        val førstegangsbehandlingBT = opprettFerdigstiltBehandling(fagsakBarnetilsyn)
        opprettVilkår(førstegangsbehandlingBT, lagreSøknadForBarnetilsyn(førstegangsbehandlingBT).sivilstand)
        grunnlagsdataService.opprettGrunnlagsdata(førstegangsbehandlingBT.id)

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingDto)
        val vilkårForBT = vilkårsvurderingRepository.findByBehandlingId(førstegangsbehandlingBT.id)
        val vilkårForRevurdering = vilkårsvurderingRepository.findByBehandlingId(revurdering.id)

        val barnPåRevurdering = barnRepository.findByBehandlingId(revurdering.id).first { it.navn.equals("Barn Barnesen") }
        val barnForBT = barnRepository.findByBehandlingId(førstegangsbehandlingBT.id).first { it.navn.equals("Barn Barnesen") }

        val sivilstandVilkårForBT = vilkårForBT.first { it.type == VilkårType.SIVILSTAND }
        val sivilstandVilkårForRevurdering = vilkårForRevurdering.first { it.type == VilkårType.SIVILSTAND }

        val aleneomsorgVilkårForBT = vilkårForBT.first { it.type == VilkårType.ALENEOMSORG && it.barnId == barnForBT.id }
        val aleneomsorgVilkårForRevurdering =
            vilkårForRevurdering.first { it.type == VilkårType.ALENEOMSORG && it.barnId == barnPåRevurdering.id }

        validerVilkårUtenBarn(sivilstandVilkårForBT, sivilstandVilkårForRevurdering, førstegangsbehandlingBT)
        validerVilkårMedBarn(aleneomsorgVilkårForBT, aleneomsorgVilkårForRevurdering, barnPåRevurdering, førstegangsbehandlingBT)
    }

    /**
     * B1 - Førstegangsbehandling overgangsstønad
     * B2 - Førstegangsbehandling barnetilsyn, med aleneomsorg satt til "skal ikke vurderes"
     * B3 - Revurdering overgangsstønad - inngangsvilkår fra barnetilsyn utenom aleneomsorg, aktivitetsvilkår fra overgangsstønad
     */
    @Test
    fun `revurdering - skal kopiere vilkår fra nyeste behandling fra annen stønad med unntak av aleneomsorgsvilkår`() {
        val førstegangsbehandlingOS = opprettFerdigstiltBehandling(fagsak)
        opprettVilkår(førstegangsbehandlingOS, lagreSøknad(førstegangsbehandlingOS).sivilstand)
        grunnlagsdataService.opprettGrunnlagsdata(førstegangsbehandlingOS.id)
        val førstegangsbehandlingBT = opprettFerdigstiltBehandling(fagsakBarnetilsyn)
        opprettVilkår(førstegangsbehandlingBT, lagreSøknadForBarnetilsyn(førstegangsbehandlingBT).sivilstand)
        grunnlagsdataService.opprettGrunnlagsdata(førstegangsbehandlingBT.id)

        val vilkårForOS = vilkårsvurderingRepository.findByBehandlingId(førstegangsbehandlingOS.id)
        val vilkårForBT = vilkårsvurderingRepository.findByBehandlingId(førstegangsbehandlingBT.id)

        val barnPåOS = barnRepository.findByBehandlingId(førstegangsbehandlingOS.id).first { it.navn.equals("Barn Barnesen") }
        val barnPåBT = barnRepository.findByBehandlingId(førstegangsbehandlingBT.id).first { it.navn.equals("Barn Barnesen") }

        val aleneomsorgVilkårForBTMedSkalIkkeVurderes =
            vilkårForBT
                .first { it.type == VilkårType.ALENEOMSORG && it.barnId == barnPåBT.id }
                .copy(resultat = Vilkårsresultat.SKAL_IKKE_VURDERES)
        vilkårsvurderingRepository.update(aleneomsorgVilkårForBTMedSkalIkkeVurderes)

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingDto)
        val vilkårForRevurdering = vilkårsvurderingRepository.findByBehandlingId(revurdering.id)

        val barnPåRevurdering =
            barnRepository.findByBehandlingId(revurdering.id).first {
                it.navn.equals("Barn Barnesen")
            }

        val sivilstandVilkårForBT = vilkårForBT.first { it.type == VilkårType.SIVILSTAND }
        val sivilstandVilkårForRevurdering = vilkårForRevurdering.first { it.type == VilkårType.SIVILSTAND }

        val aleneomsorgVilkårForRevurdering =
            vilkårForRevurdering.first { it.type == VilkårType.ALENEOMSORG && it.barnId == barnPåRevurdering.id }
        val aleneomsorgVilkårForOS = vilkårForOS.first { it.type == VilkårType.ALENEOMSORG && it.barnId == barnPåOS.id }

        validerVilkårUtenBarn(sivilstandVilkårForBT, sivilstandVilkårForRevurdering, førstegangsbehandlingBT)
        validerVilkårMedBarn(aleneomsorgVilkårForOS, aleneomsorgVilkårForRevurdering, barnPåRevurdering, førstegangsbehandlingOS)
    }

    /**
     * B1 - Førstegangsbehandling overgangsstønad
     * B2 - Førstegangsbehandling barnetilsyn, med aleneomsorg satt til "skal ikke vurderes"
     * B3 - Revurdering barnetilsyn - inngangsvilkår fra barnetilsyn MED aleneomsorg
     */
    @Test
    fun `revurdering - skal kopiere vilkår fra nyeste behandling fra samme stønad og ta med aleneomsorgsvilkår`() {
        val førstegangsbehandlingOS = opprettFerdigstiltBehandling(fagsak)
        opprettVilkår(førstegangsbehandlingOS, lagreSøknad(førstegangsbehandlingOS).sivilstand)
        grunnlagsdataService.opprettGrunnlagsdata(førstegangsbehandlingOS.id)
        val førstegangsbehandlingBT = opprettFerdigstiltBehandling(fagsakBarnetilsyn)
        opprettVilkår(førstegangsbehandlingBT, lagreSøknadForBarnetilsyn(førstegangsbehandlingBT).sivilstand)
        grunnlagsdataService.opprettGrunnlagsdata(førstegangsbehandlingBT.id)

        val vilkårForBT = vilkårsvurderingRepository.findByBehandlingId(førstegangsbehandlingBT.id)

        val aleneomsorgVilkårForBTMedSkalIkkeVurderes =
            vilkårForBT
                .first { it.type == VilkårType.ALENEOMSORG }
                .copy(resultat = Vilkårsresultat.SKAL_IKKE_VURDERES)
        vilkårsvurderingRepository.update(aleneomsorgVilkårForBTMedSkalIkkeVurderes)

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingDto.copy(fagsakId = fagsakBarnetilsyn.id))
        val vilkårForRevurdering = vilkårsvurderingRepository.findByBehandlingId(revurdering.id)
        val vilkårForBTOppdatert = vilkårsvurderingRepository.findByBehandlingId(førstegangsbehandlingBT.id)
        val barnPåRevurdering =
            barnRepository.findByBehandlingId(revurdering.id).first {
                it.navn.equals("Barn Barnesen")
            }
        val barnPåFørstegangsbehandlingBT =
            barnRepository.findByBehandlingId(førstegangsbehandlingBT.id).first {
                it.navn.equals("Barn Barnesen")
            }

        val sivilstandVilkårForBT = vilkårForBTOppdatert.first { it.type == VilkårType.SIVILSTAND }
        val sivilstandVilkårForRevurdering = vilkårForRevurdering.first { it.type == VilkårType.SIVILSTAND }

        val aleneomsorgVilkårForRevurdering =
            vilkårForRevurdering.first { it.type == VilkårType.ALENEOMSORG && it.barnId == barnPåRevurdering.id }
        val aleneomsorgVilkårForBT =
            vilkårForBTOppdatert.first { it.type == VilkårType.ALENEOMSORG && it.barnId == barnPåFørstegangsbehandlingBT.id }

        validerVilkårUtenBarn(sivilstandVilkårForBT, sivilstandVilkårForRevurdering, førstegangsbehandlingBT)
        validerVilkårMedBarn(aleneomsorgVilkårForBT, aleneomsorgVilkårForRevurdering, barnPåRevurdering, førstegangsbehandlingBT)
    }

    private fun validerVilkårMedBarn(
        aleneomsorgVilkårForBehandling: Vilkårsvurdering,
        aleneomsorgVilkårForRevurdering: Vilkårsvurdering,
        barnPåBehandlingRevurdering: BehandlingBarn,
        opphavsBehandling: Behandling,
    ) {
        assertThat(aleneomsorgVilkårForBehandling.id).isNotEqualTo(aleneomsorgVilkårForRevurdering.id)
        assertThat(aleneomsorgVilkårForBehandling.barnId).isNotEqualTo(aleneomsorgVilkårForRevurdering.barnId)
        assertThat(aleneomsorgVilkårForBehandling.behandlingId).isNotEqualTo(aleneomsorgVilkårForRevurdering.behandlingId)
        assertThat(aleneomsorgVilkårForBehandling.sporbar.opprettetTid).isNotEqualTo(aleneomsorgVilkårForRevurdering.sporbar.opprettetTid)
        assertThat(
            aleneomsorgVilkårForBehandling.sporbar.endret.endretTid,
        ).isNotEqualTo(aleneomsorgVilkårForRevurdering.sporbar.endret.endretTid)
        assertThat(aleneomsorgVilkårForBehandling.barnId).isNotNull
        assertThat(aleneomsorgVilkårForRevurdering.barnId).isEqualTo(barnPåBehandlingRevurdering.id)
        assertThat(aleneomsorgVilkårForBehandling.opphavsvilkår).isNull()
        assertThat(aleneomsorgVilkårForRevurdering.opphavsvilkår)
            .isEqualTo(Opphavsvilkår(opphavsBehandling.id, aleneomsorgVilkårForBehandling.sporbar.endret.endretTid))
    }

    private fun validerVilkårUtenBarn(
        sivilstandVilkårForBehandling: Vilkårsvurdering,
        sivilstandVilkårForRevurdering: Vilkårsvurdering,
        opphavsBehandling: Behandling,
    ) {
        assertThat(sivilstandVilkårForBehandling.id).isNotEqualTo(sivilstandVilkårForRevurdering.id)
        assertThat(sivilstandVilkårForBehandling.behandlingId).isNotEqualTo(sivilstandVilkårForRevurdering.behandlingId)
        assertThat(sivilstandVilkårForBehandling.sporbar.opprettetTid).isNotEqualTo(sivilstandVilkårForRevurdering.sporbar.opprettetTid)
        assertThat(
            sivilstandVilkårForBehandling.sporbar.endret.endretTid,
        ).isNotEqualTo(sivilstandVilkårForRevurdering.sporbar.endret.endretTid)
        assertThat(sivilstandVilkårForRevurdering.barnId).isNull()
        assertThat(sivilstandVilkårForBehandling.barnId).isNull()
        assertThat(sivilstandVilkårForBehandling.opphavsvilkår).isNull()
        assertThat(sivilstandVilkårForRevurdering.opphavsvilkår)
            .isEqualTo(Opphavsvilkår(opphavsBehandling.id, sivilstandVilkårForBehandling.sporbar.endret.endretTid))
    }

    private fun ferdigstillVedtak(
        vedtak: Vedtak,
        behandling: Behandling,
        fagsakBarnetilsyn: Fagsak,
    ) {
        vedtakService.lagreVedtak(vedtak.tilVedtakDto(), behandling.id, StønadType.BARNETILSYN)

        beregnYtelseSteg.utførSteg(saksbehandling(fagsakBarnetilsyn, behandling), vedtak.tilVedtakDto())
        behandlingRepository.update(
            behandling.copy(
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.INNVILGET,
                vedtakstidspunkt = SporbarUtils.now(),
            ),
        )
    }

    private fun opprettBarnetilsynBehandling(): Pair<Fagsak, Behandling> {
        revurderingDto =
            RevurderingDto(
                fagsakBarnetilsyn.id,
                BehandlingÅrsak.SATSENDRING,
                kravMottatt,
                VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE,
            )

        val behandling = behandling(fagsakBarnetilsyn)
        behandlingRepository.insert(behandling)
        opprettVilkår(behandling, lagreSøknadForBarnetilsyn(behandling).sivilstand)
        return Pair(fagsakBarnetilsyn, behandling)
    }

    @Test
    internal fun `revurdering med nye barn - skal kopiere vilkår`() {
        val behandling = opprettFerdigstiltBehandling(fagsak)
        val barn = listOf(TestsøknadBuilder.Builder().defaultBarn("Barn Barnesen", PdlClientConfig.BARN_FNR))
        opprettVilkår(behandling, lagreSøknad(behandling, barn).sivilstand)

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingDto)
        val vilkårForBehandling = vilkårsvurderingRepository.findByBehandlingId(behandling.id)
        val vilkårForRevurdering = vilkårsvurderingRepository.findByBehandlingId(revurdering.id)
        val barnPåRevurdering = barnRepository.findByBehandlingId(revurdering.id)

        assertThat(vilkårForBehandling).hasSize(2)
        assertThat(vilkårForRevurdering).hasSize(3)
        assertThat(vilkårForBehandling.filter { it.barnId != null }).hasSize(1)
        assertThat(vilkårForRevurdering.filter { it.barnId != null }).hasSize(2)
        assertThat(vilkårForBehandling.mapNotNull { it.barnId }).isNotIn(barnPåRevurdering.map { it.id })
        assertThat(vilkårForRevurdering.mapNotNull { it.barnId })
            .containsExactlyInAnyOrderElementsOf(barnPåRevurdering.map { it.id })
        assertThat(vilkårForBehandling.map { it.behandlingId }).isNotIn(vilkårForRevurdering.map { it.behandlingId })
        assertThat(vilkårForBehandling.map { it.sporbar.opprettetTid }).isNotIn(vilkårForRevurdering.map { it.sporbar.opprettetTid })

        assertThat(vilkårForBehandling.first { it.type == VilkårType.SIVILSTAND })
            .usingRecursiveComparison()
            .ignoringFields("id", "sporbar", "behandlingId", "barnId", "opphavsvilkår")
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
        listOf(fagsakBarnetilsyn, fagsakSkolepenger).forEach { fagsak ->
            val behandling = opprettFerdigstiltBehandling(fagsak)
            opprettVilkår(behandling, lagreSøknad(behandling).sivilstand)
            val revurderingInnhold =
                RevurderingDto(fagsak.id, BehandlingÅrsak.G_OMREGNING, kravMottatt, VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE)

            assertThatThrownBy {
                revurderingService.opprettRevurderingManuelt(revurderingInnhold)
            }.hasMessageContaining("Kan ikke opprette revurdering med årsak g-omregning")
        }
    }

    @Test
    internal fun `kan opprette g-omregning for overgangsstønad`() {
        val behandling = opprettFerdigstiltBehandling(fagsak)
        opprettVilkår(behandling, lagreSøknad(behandling).sivilstand)
        val revurderingInnhold =
            RevurderingDto(fagsak.id, BehandlingÅrsak.G_OMREGNING, kravMottatt, VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE)

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingInnhold)

        assertThat(revurdering.årsak).isEqualTo(BehandlingÅrsak.G_OMREGNING)
    }

    private fun opprettFerdigstiltBehandling(
        fagsak: Fagsak,
        resultat: BehandlingResultat = BehandlingResultat.INNVILGET,
    ) = behandlingRepository.insert(
        behandling(
            fagsak = fagsak,
            status = BehandlingStatus.FERDIGSTILT,
            årsak = BehandlingÅrsak.SØKNAD,
            resultat = resultat,
        ),
    )

    private fun getSøknadsskjemaId(revurdering1: Behandling) = søknadRepository.findByBehandlingId(revurdering1.id)!!.soknadsskjemaId

    private fun lagreSøknad(
        behandling: Behandling,
        barn: List<Barn> =
            listOf(
                TestsøknadBuilder.Builder().defaultBarn("Barn Barnesen", PdlClientConfig.BARN_FNR),
                TestsøknadBuilder.Builder().defaultBarn("Barn2 Barnesen", PdlClientConfig.BARN2_FNR),
            ),
    ): SøknadsskjemaOvergangsstønad {
        val søknad =
            TestsøknadBuilder
                .Builder()
                .setBarn(barn)
                .build()
                .søknadOvergangsstønad
        søknadService.lagreSøknadForOvergangsstønad(søknad, behandling.id, behandling.fagsakId, "1L")
        val overgangsstønad =
            søknadService.hentOvergangsstønad(behandling.id)
                ?: error("Fant ikke overgangsstønad for testen")
        barnRepository.insertAll(søknadBarnTilBehandlingBarn(overgangsstønad.barn, behandling.id))
        return overgangsstønad
    }

    private fun lagreSøknadForBarnetilsyn(behandling: Behandling): SøknadsskjemaBarnetilsyn {
        val søknad =
            TestsøknadBuilder
                .Builder()
                .setBarn(
                    listOf(
                        TestsøknadBuilder.Builder().defaultBarn("Barn Barnesen", PdlClientConfig.BARN_FNR),
                        TestsøknadBuilder.Builder().defaultBarn("Barn2 Barnesen", PdlClientConfig.BARN2_FNR),
                    ),
                ).build()
                .søknadBarnetilsyn
        søknadService.lagreSøknadForBarnetilsyn(søknad, behandling.id, behandling.fagsakId, "1L")
        val barnetilsyn = søknadService.hentBarnetilsyn(behandling.id) ?: error("Fant ikke overgangsstønad for testen")
        barnRepository.insertAll(søknadBarnTilBehandlingBarn(barnetilsyn.barn, behandling.id))
        return barnetilsyn
    }

    private fun opprettVilkår(
        behandling: Behandling,
        sivilstand: Sivilstand?,
    ) {
        val barn = barnRepository.findByBehandlingId(behandling.id)
        val delvilkårsvurdering =
            lagSivilstandDelvilkår(sivilstand)

        val delvilkårsvurderingAleneomsorg =
            lagDelvilkårsvurderingAleneomsorg(barn, sivilstand, behandling)
        lagreVilkår(behandling, delvilkårsvurdering, barn, delvilkårsvurderingAleneomsorg)
    }

    private fun lagDelvilkårsvurderingAleneomsorg(
        barn: List<BehandlingBarn>,
        sivilstand: Sivilstand?,
        behandling: Behandling,
    ): List<Delvilkårsvurdering> {
        val delvilkårsvurderingAleneomsorg =
            AleneomsorgRegel().initiereDelvilkårsvurdering(
                HovedregelMetadata(
                    sivilstand,
                    Sivilstandstype.ENKE_ELLER_ENKEMANN,
                    barn = barn,
                    søktOmBarnetilsyn = emptyList(),
                    vilkårgrunnlagDto = VilkårTestUtil.mockVilkårGrunnlagDto(),
                    behandling = behandling,
                ),
            )
        return delvilkårsvurderingAleneomsorg
    }

    private fun lagSivilstandDelvilkår(sivilstand: Sivilstand?): List<Delvilkårsvurdering> {
        val behandlingMock = mockk<Behandling>()
        every { behandlingMock.erDigitalSøknad() } returns true
        val delvilkårsvurdering =
            SivilstandRegel().initiereDelvilkårsvurdering(
                HovedregelMetadata(
                    sivilstand,
                    Sivilstandstype.ENKE_ELLER_ENKEMANN,
                    barn = emptyList(),
                    søktOmBarnetilsyn = emptyList(),
                    vilkårgrunnlagDto = mockk(),
                    behandling = behandlingMock,
                ),
            )
        return delvilkårsvurdering
    }

    private fun lagreVilkår(
        behandling: Behandling,
        delvilkårsvurdering: List<Delvilkårsvurdering>,
        barn: List<BehandlingBarn>,
        delvilkårsvurderingAleneomsorg: List<Delvilkårsvurdering>,
    ) {
        val vilkårForBarn =
            barn.map {
                vilkårsvurdering(
                    resultat = Vilkårsresultat.OPPFYLT,
                    type = VilkårType.ALENEOMSORG,
                    behandlingId = behandling.id,
                    barnId = it.id,
                    delvilkårsvurdering = delvilkårsvurderingAleneomsorg,
                )
            }
        vilkårsvurderingRepository.insertAll(
            vilkårForBarn +
                vilkårsvurdering(
                    resultat = Vilkårsresultat.OPPFYLT,
                    type = VilkårType.SIVILSTAND,
                    behandlingId = behandling.id,
                    delvilkårsvurdering = delvilkårsvurdering,
                ),
        )
    }
}
