package no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class BeregnYtelseStegBarnetilsynIntegrationTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var beregnYtelseSteg: BeregnYtelseSteg

    @Autowired
    private lateinit var barnRepository: BarnRepository

    @Autowired
    private lateinit var tilkjentytelseRepository: TilkjentYtelseRepository

    private val fagsak = fagsak(fagsakpersoner(setOf("1")), StønadType.BARNETILSYN)
    private val behandling = behandling(fagsak)
    val barn =
        behandlingBarn(
            id = UUID.randomUUID(),
            behandlingId = behandling.id,
            søknadBarnId = UUID.randomUUID(),
            personIdent = "01010112345",
            navn = "Ola",
            fødselTermindato = LocalDate.now(),
        )
    private val barnBehandling1 = listOf(barn)
    private val saksbehandling = saksbehandling(fagsak, behandling)

    private val behandling2 = behandling(fagsak, type = BehandlingType.REVURDERING, forrigeBehandlingId = behandling.id)
    private val barnBehandling2 = listOf(barn.copy(id = UUID.randomUUID(), behandlingId = behandling2.id))
    private val saksbehandling2 = saksbehandling(fagsak, behandling2)

    private val januar = YearMonth.of(2021, 1)
    private val februar = YearMonth.of(2021, 2)
    private val mars = YearMonth.of(2021, 3)
    private val april = YearMonth.of(2021, 4)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
    }

    @Test
    internal fun `kildeBehandlingId skal bli beholdt på andelen som ikke endrer seg`() {
        val utgiftsperiode = opprettUtgiftsperiode(januar, mars, barnBehandling1.map { it.id }, BigDecimal(2000))
        val utgiftsperiode2 = opprettUtgiftsperiode(mars, mars, barnBehandling2.map { it.id }, BigDecimal(2500))
        opprettBehandlingOgBarn(behandling, barnBehandling1)
        innvilge(saksbehandling, listOf(utgiftsperiode))
        settBehandlingTilIverksatt(behandling)
        opprettBehandlingOgBarn(behandling2, barnBehandling2)
        innvilge(saksbehandling2, listOf(utgiftsperiode2))
        settBehandlingTilIverksatt(behandling2)

        assertThat(hentAndeler(behandling.id)).hasSize(1)
        val andeler = hentAndeler(behandling2.id)
        assertThat(andeler).hasSize(2)
        assertThat(andeler[0].kildeBehandlingId).isEqualTo(behandling.id)
        assertThat(andeler[1].kildeBehandlingId).isEqualTo(behandling2.id)
    }

    @Test
    internal fun `kildeBehandlingId skal bli endret når man skriver over hele perioden`() {
        val utgiftsperiode1 = opprettUtgiftsperiode(mars, mars, barnBehandling1.map { it.id }, BigDecimal(2500))
        val utgiftsperiode2 = opprettUtgiftsperiode(januar, mars, barnBehandling2.map { it.id }, BigDecimal(2000))

        opprettBehandlingOgBarn(behandling, barnBehandling1)
        innvilge(saksbehandling, listOf(utgiftsperiode1))
        settBehandlingTilIverksatt(behandling)
        opprettBehandlingOgBarn(behandling2, barnBehandling2)
        innvilge(saksbehandling2, listOf(utgiftsperiode2))
        settBehandlingTilIverksatt(behandling2)

        assertThat(hentAndeler(behandling.id)).hasSize(1)
        val andeler = hentAndeler(behandling2.id)
        assertThat(andeler).hasSize(1)
        assertThat(andeler[0].kildeBehandlingId).isEqualTo(behandling2.id)
    }

    @Test
    internal fun `skal ikke kunne midlertidig opphøre en periode ved å legge inn 0 i utgifter`() {
        val utgiftsperiode = opprettUtgiftsperiode(januar, mars, barnBehandling1.map { it.id }, BigDecimal(0))
        opprettBehandlingOgBarn(behandling, barnBehandling1)
        val feil: ApiFeil =
            assertThrows {
                innvilge(saksbehandling, listOf(utgiftsperiode))
            }
        assertThat(feil.feil).contains("Kan ikke ha null utgifter på en periode som ikke er et midlertidig opphør eller sanksjon, på behandling=")
    }

    @Test
    internal fun `skal ikke kunne midlertidig opphøre første periode i en førstegangsbehandling`() {
        val barnId = barnBehandling1.map { it.id }
        val utgiftsperiodeOpphør =
            opprettUtgiftsperiode(januar, februar, emptyList(), BigDecimal(0), periodetype = PeriodetypeBarnetilsyn.OPPHØR)
        val utgiftsperiodeUtbetaling =
            opprettUtgiftsperiode(mars, april, barnId, BigDecimal(100), periodetype = PeriodetypeBarnetilsyn.ORDINÆR)
        opprettBehandlingOgBarn(behandling.copy(type = BehandlingType.FØRSTEGANGSBEHANDLING), barnBehandling1)
        val feil: ApiFeil =
            assertThrows {
                innvilge(saksbehandling, listOf(utgiftsperiodeOpphør, utgiftsperiodeUtbetaling))
            }
        assertThat(feil.feil).contains("Første periode kan ikke være en opphørsperiode, på førstegangsbehandling=")
    }

    @Test
    internal fun `skal kunne ha midlertidig opphøre etter en ordinær periode i en førstegangsbehandling`() {
        val barnId = barnBehandling1.map { it.id }
        val utgiftsperiodeUtbetaling =
            opprettUtgiftsperiode(januar, februar, barnId, BigDecimal(100), periodetype = PeriodetypeBarnetilsyn.ORDINÆR)
        val utgiftsperiodeOpphør =
            opprettUtgiftsperiode(mars, april, emptyList(), BigDecimal(0), periodetype = PeriodetypeBarnetilsyn.OPPHØR)
        opprettBehandlingOgBarn(behandling.copy(type = BehandlingType.FØRSTEGANGSBEHANDLING), barnBehandling1)
        innvilge(saksbehandling, listOf(utgiftsperiodeUtbetaling, utgiftsperiodeOpphør))
    }

    fun settBehandlingTilIverksatt(behandling: Behandling) {
        behandlingRepository.update(
            behandling.copy(
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.INNVILGET,
                vedtakstidspunkt = SporbarUtils.now(),
            ),
        )
    }

    private fun hentAndeler(behandlingId: UUID): List<AndelTilkjentYtelse> = tilkjentytelseRepository.findByBehandlingId(behandlingId)!!.andelerTilkjentYtelse.sortedBy { it.stønadFom }

    private fun opprettUtgiftsperiode(
        fra: YearMonth,
        til: YearMonth,
        barnId: List<UUID>,
        beløp: BigDecimal,
        periodetype: PeriodetypeBarnetilsyn = PeriodetypeBarnetilsyn.ORDINÆR,
        aktivitetstype: AktivitetstypeBarnetilsyn? = AktivitetstypeBarnetilsyn.I_ARBEID,
    ) = UtgiftsperiodeDto(
        fra,
        til,
        Månedsperiode(fra, til),
        barnId,
        beløp.toInt(),
        null,
        periodetype,
        aktivitetstype,
    )

    private fun innvilge(
        saksbehandling: Saksbehandling,
        utgiftsperioder: List<UtgiftsperiodeDto>,
    ) {
        val vedtak =
            InnvilgelseBarnetilsyn(
                perioder = utgiftsperioder,
                begrunnelse = null,
                perioderKontantstøtte = listOf(),
                tilleggsstønad =
                    TilleggsstønadDto(
                        harTilleggsstønad = false,
                        perioder = listOf(),
                        begrunnelse = null,
                    ),
            )
        beregnYtelseSteg.utførSteg(saksbehandling, vedtak)
    }

    private fun opprettBehandlingOgBarn(
        behandling: Behandling,
        barn: List<BehandlingBarn>,
    ) {
        behandlingRepository.insert(behandling)
        barnRepository.insertAll(barn)
    }
}
