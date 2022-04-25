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
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class BeregnYtelseStegBarnetilsynIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var beregnYtelseSteg: BeregnYtelseSteg
    @Autowired private lateinit var barnRepository: BarnRepository
    @Autowired private lateinit var tilkjentytelseRepository: TilkjentYtelseRepository

    private val fagsak = fagsak(fagsakpersoner(setOf("1")), StønadType.BARNETILSYN)
    private val behandling = behandling(fagsak)
    val barn = behandlingBarn(id = UUID.randomUUID(),
                              behandlingId = behandling.id,
                              søknadBarnId = UUID.randomUUID(),
                              personIdent = "01010112345",
                              navn = "Ola",
                              fødselTermindato = LocalDate.now())
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
        innvilg(saksbehandling, listOf(utgiftsperiode))
        settBehandlingTilIverksatt(behandling)
        opprettBehandlingOgBarn(behandling2, barnBehandling2)
        innvilg(saksbehandling2, listOf(utgiftsperiode2))
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
        innvilg(saksbehandling, listOf(utgiftsperiode1))
        settBehandlingTilIverksatt(behandling)
        opprettBehandlingOgBarn(behandling2, barnBehandling2)
        innvilg(saksbehandling2, listOf(utgiftsperiode2))
        settBehandlingTilIverksatt(behandling2)

        assertThat(hentAndeler(behandling.id)).hasSize(1)
        val andeler = hentAndeler(behandling2.id)
        assertThat(andeler).hasSize(1)
        assertThat(andeler[0].kildeBehandlingId).isEqualTo(behandling2.id)
    }

    @Test
    internal fun `skal kunne midlertidig opphøre en periode ved å legge inn 0 i utgifter`() {
        val utgiftsperiode = opprettUtgiftsperiode(januar, mars, barnBehandling1.map { it.id }, BigDecimal(2000))
        val utgiftsperiode2 = opprettUtgiftsperiode(januar, januar, barnBehandling2.map { it.id }, BigDecimal(0))
        val utgiftsperiode3 = opprettUtgiftsperiode(februar, mars, barnBehandling2.map { it.id }, BigDecimal(3000))
        opprettBehandlingOgBarn(behandling, barnBehandling1)
        innvilg(saksbehandling, listOf(utgiftsperiode))
        settBehandlingTilIverksatt(behandling)
        opprettBehandlingOgBarn(behandling2, barnBehandling2)
        innvilg(saksbehandling2, listOf(utgiftsperiode2, utgiftsperiode3))
        settBehandlingTilIverksatt(behandling2)

        val andelerFørstegangsbehandling = hentAndeler(behandling.id)
        assertThat(andelerFørstegangsbehandling).hasSize(1)
        assertThat(andelerFørstegangsbehandling.first().stønadFom).isEqualTo(januar.atDay(1))
        assertThat(andelerFørstegangsbehandling.first().stønadTom).isEqualTo(mars.atEndOfMonth())
        assertThat(andelerFørstegangsbehandling.first().beløp).isBetween(1, 2000)

        val andelerRevurdering = hentAndeler(behandling2.id)
        assertThat(andelerRevurdering).hasSize(1)
        assertThat(andelerRevurdering.first().stønadFom).isEqualTo(februar.atDay(1))
        assertThat(andelerRevurdering.first().stønadTom).isEqualTo(mars.atEndOfMonth())
        assertThat(andelerRevurdering.first().beløp).isBetween(1, 3000)
    }

    @Test
    internal fun `skal kunne midlertidig opphøre en periode ved å legge inn 0 i utgifter samt ha hull til neste periode`() {
        val utgiftsperiode = opprettUtgiftsperiode(januar, mars, barnBehandling1.map { it.id }, BigDecimal(2000))
        val utgiftsperiode2 = opprettUtgiftsperiode(februar, februar, barnBehandling2.map { it.id }, BigDecimal(0))
        val utgiftsperiode3 = opprettUtgiftsperiode(april, april, barnBehandling2.map { it.id }, BigDecimal(3000))
        opprettBehandlingOgBarn(behandling, barnBehandling1)
        innvilg(saksbehandling, listOf(utgiftsperiode))
        settBehandlingTilIverksatt(behandling)
        opprettBehandlingOgBarn(behandling2, barnBehandling2)
        innvilg(saksbehandling2, listOf(utgiftsperiode2, utgiftsperiode3))
        settBehandlingTilIverksatt(behandling2)

        val andelerFørstegangsbehandling = hentAndeler(behandling.id)
        assertThat(andelerFørstegangsbehandling).hasSize(1)
        assertThat(andelerFørstegangsbehandling.first().stønadFom).isEqualTo(januar.atDay(1))
        assertThat(andelerFørstegangsbehandling.first().stønadTom).isEqualTo(mars.atEndOfMonth())
        assertThat(andelerFørstegangsbehandling.first().beløp).isBetween(1, 2000)

        val andelerRevurdering = hentAndeler(behandling2.id)
        assertThat(andelerRevurdering).hasSize(2)

        assertThat(andelerRevurdering.first().stønadFom).isEqualTo(januar.atDay(1))
        assertThat(andelerRevurdering.first().stønadTom).isEqualTo(januar.atEndOfMonth())
        assertThat(andelerRevurdering.first().beløp).isBetween(1, 2000)

        assertThat(andelerRevurdering.last().stønadFom).isEqualTo(april.atDay(1))
        assertThat(andelerRevurdering.last().stønadTom).isEqualTo(april.atEndOfMonth())
        assertThat(andelerRevurdering.last().beløp).isBetween(1, 3000)
    }

    fun settBehandlingTilIverksatt(behandling: Behandling) {
        behandlingRepository.update(behandling.copy(status = BehandlingStatus.FERDIGSTILT,
                                                    resultat = BehandlingResultat.INNVILGET))
    }

    private fun hentAndeler(behandlingId: UUID): List<AndelTilkjentYtelse> =
            tilkjentytelseRepository.findByBehandlingId(behandlingId)!!.andelerTilkjentYtelse.sortedBy { it.stønadFom }

    private fun opprettUtgiftsperiode(fra: YearMonth, til: YearMonth, barnId: List<UUID>, beløp: BigDecimal) =
            UtgiftsperiodeDto(fra, til, barnId, beløp.toInt())

    private fun innvilg(saksbehandling: Saksbehandling,
                        utgiftsperioder: List<UtgiftsperiodeDto>) {
        val vedtak = InnvilgelseBarnetilsyn(
                perioder = utgiftsperioder,
                begrunnelse = null,
                perioderKontantstøtte = listOf(),
                tilleggsstønad = TilleggsstønadDto(harTilleggsstønad = false,
                                                   perioder = listOf(),
                                                   begrunnelse = null),
        )
        beregnYtelseSteg.utførSteg(saksbehandling, vedtak)
    }

    private fun opprettBehandlingOgBarn(behandling: Behandling, barn: List<BehandlingBarn>) {
        behandlingRepository.insert(behandling)
        barnRepository.insertAll(barn)
    }
}