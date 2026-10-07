package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn.FORBIGÅENDE_SYKDOM
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn.I_ARBEID
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.historikk.EndringType.ERSTATTET
import no.nav.familie.ef.sak.vedtak.historikk.EndringType.FJERNET
import no.nav.familie.ef.sak.vedtak.historikk.EndringType.SPLITTET
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Tester andelshistorikken fra vedtak til ferdig historikk
 * vedtak -> BeregnYtelseSteg (lagrer vedtak + tilkjent ytelse) -> AndelsHistorikkService.hentHistorikk
 */
internal class AndelHistorikkBarnetilsynIntegrationTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var barnRepository: BarnRepository

    @Autowired
    private lateinit var beregnYtelseSteg: BeregnYtelseSteg

    @Autowired
    private lateinit var andelsHistorikkService: AndelsHistorikkService

    private val fagsak = fagsak(fagsakpersoner(setOf("1")), StønadType.BARNETILSYN)
    private val behandlingsnummer = mutableMapOf<UUID, Int>()
    private val barnPerBehandling = mutableMapOf<UUID, List<UUID>>()
    private var forrigeBehandling: Behandling? = null

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
    }

    @Test
    internal fun `Revurdering fra samme dato med samme innhold gir FJERNET og ny rad`() {
        vedta(periode(januar(2024), mars(2024), utgifter = 2000))
        vedta(periode(januar(2024), mars(2024), utgifter = 2000))

        assertThat(historikk()).containsExactly(
            Rad(1, januar(2024), mars(2024), FJERNET, endretI = 2, I_ARBEID),
            Rad(2, januar(2024), mars(2024), null, endretI = null, I_ARBEID),
        )
    }

    @Test
    internal fun `Endret aktivitet i første måned av revurderingen gir ERSTATTET`() {
        vedta(
            periode(februar(2024), februar(2024), utgifter = 2001),
            periode(mars(2024), desember(2024), utgifter = 2000),
        )
        vedta(
            periode(mars(2024), mars(2024), utgifter = 2000, aktivitet = FORBIGÅENDE_SYKDOM),
            periode(april(2024), desember(2024), utgifter = 2000),
        )

        assertThat(historikk()).containsExactly(
            Rad(1, februar(2024), februar(2024), null, endretI = null, I_ARBEID),
            Rad(1, mars(2024), desember(2024), ERSTATTET, endretI = 2, I_ARBEID),
            Rad(2, mars(2024), mars(2024), null, endretI = null, FORBIGÅENDE_SYKDOM),
            Rad(2, april(2024), desember(2024), null, endretI = null, I_ARBEID),
        )
    }

    @Test
    internal fun `Kun aktiv periode for måneden med endret aktivitet er den nye`() {
        vedta(
            periode(februar(2024), februar(2024), utgifter = 2001),
            periode(mars(2024), desember(2024), utgifter = 2000),
        )
        vedta(
            periode(mars(2024), mars(2024), utgifter = 2000, aktivitet = FORBIGÅENDE_SYKDOM),
            periode(april(2024), desember(2024), utgifter = 2000),
        )

        val aktiveForMars =
            andelsHistorikkService
                .hentHistorikk(fagsak.id, null)
                .filter { it.erAktivVedtaksperiode() }
                .filter { it.andel.periode.inneholder(mars(2024)) }

        assertThat(aktiveForMars).hasSize(1)
        assertThat(aktiveForMars.single().aktivitetBarnetilsyn).isEqualTo(FORBIGÅENDE_SYKDOM)
    }

    @Test
    internal fun `Endret aktivitet senere i revurderingen gir SPLITTET, FJERNET og nye rader`() {
        vedta(periode(januar(2024), desember(2024), utgifter = 2000))
        vedta(
            periode(mars(2024), mars(2024), utgifter = 2000, aktivitet = FORBIGÅENDE_SYKDOM),
            periode(april(2024), desember(2024), utgifter = 2000),
        )

        assertThat(historikk()).containsExactly(
            Rad(1, januar(2024), februar(2024), SPLITTET, endretI = 2, I_ARBEID),
            Rad(1, mars(2024), desember(2024), FJERNET, endretI = 2, I_ARBEID),
            Rad(2, mars(2024), mars(2024), null, endretI = null, FORBIGÅENDE_SYKDOM),
            Rad(2, april(2024), desember(2024), null, endretI = null, I_ARBEID),
        )
    }

    @Test
    internal fun `Endret aktivitet og kortere periode fra samme dato gir ERSTATTET på hele den gamle perioden`() {
        vedta(periode(januar(2024), desember(2024), utgifter = 2000))
        vedta(periode(januar(2024), mars(2024), utgifter = 2000, aktivitet = FORBIGÅENDE_SYKDOM))

        assertThat(historikk()).containsExactly(
            Rad(1, januar(2024), desember(2024), ERSTATTET, endretI = 2, I_ARBEID),
            Rad(2, januar(2024), mars(2024), null, endretI = null, FORBIGÅENDE_SYKDOM),
        )
    }

    @Test
    internal fun `Samme aktivitet og kortere periode fra samme dato gir SPLITTET og FJERNET`() {
        vedta(periode(januar(2024), desember(2024), utgifter = 2000))
        vedta(periode(januar(2024), mars(2024), utgifter = 2000))

        assertThat(historikk()).containsExactly(
            Rad(1, januar(2024), mars(2024), SPLITTET, endretI = 2, I_ARBEID),
            Rad(1, april(2024), desember(2024), FJERNET, endretI = 2, I_ARBEID),
        )
    }

    /**
     * Én rad i historikken, med behandlinger som nummer (1, 2, …) slik at forventningene er lette å lese.
     */
    private data class Rad(
        val behandling: Int,
        val fom: YearMonth,
        val tom: YearMonth,
        val endring: EndringType?,
        val endretI: Int?,
        val aktivitet: AktivitetstypeBarnetilsyn?,
    )

    private fun historikk(): List<Rad> =
        andelsHistorikkService.hentHistorikk(fagsak.id, null).map {
            Rad(
                behandling = behandlingsnummer.getValue(it.behandlingId),
                fom = it.andel.periode.fom,
                tom = it.andel.periode.tom,
                endring = it.endring?.type,
                endretI = it.endring?.let { endring -> behandlingsnummer.getValue(endring.behandlingId) },
                aktivitet = it.aktivitetBarnetilsyn,
            )
        }

    private data class Periode(
        val fom: YearMonth,
        val tom: YearMonth,
        val utgifter: Int,
        val aktivitet: AktivitetstypeBarnetilsyn,
    )

    private fun periode(
        fom: YearMonth,
        tom: YearMonth,
        utgifter: Int,
        aktivitet: AktivitetstypeBarnetilsyn = I_ARBEID,
    ) = Periode(fom, tom, utgifter, aktivitet)

    /**
     * Oppretter en ny behandling (førstegangsbehandling første gang, ellers revurdering av forrige),
     * vedtar innvilgelse med gitte perioder via BeregnYtelseSteg, og setter behandlingen til iverksatt.
     */
    private fun vedta(vararg perioder: Periode) {
        val behandling = opprettBehandlingMedBarn()
        val barn = barnPerBehandling.getValue(behandling.id)
        val vedtak =
            InnvilgelseBarnetilsyn(
                perioder = perioder.map { it.tilUtgiftsperiode(barn) },
                begrunnelse = null,
                perioderKontantstøtte = listOf(),
                kontantstøtteBegrunnelse = null,
                tilleggsstønad = TilleggsstønadDto(perioder = listOf(), begrunnelse = null),
            )
        beregnYtelseSteg.utførSteg(saksbehandling(fagsak, behandling), vedtak)
        settTilIverksatt(behandling)
    }

    private fun opprettBehandlingMedBarn(): Behandling {
        val forrige = forrigeBehandling
        val behandling =
            behandling(
                fagsak,
                type = if (forrige == null) BehandlingType.FØRSTEGANGSBEHANDLING else BehandlingType.REVURDERING,
                forrigeBehandlingId = forrige?.id,
            )
        behandlingRepository.insert(behandling)
        val barn = lagBarn(behandling.id)
        barnRepository.insertAll(listOf(barn))

        behandlingsnummer[behandling.id] = behandlingsnummer.size + 1
        barnPerBehandling[behandling.id] = listOf(barn.id)
        return behandling
    }

    private fun lagBarn(behandlingId: UUID): BehandlingBarn =
        behandlingBarn(
            id = UUID.randomUUID(),
            behandlingId = behandlingId,
            søknadBarnId = UUID.randomUUID(),
            personIdent = "01010112345",
            navn = "Ola",
            fødselTermindato = LocalDate.of(2020, 1, 1),
        )

    private fun settTilIverksatt(behandling: Behandling) {
        val iverksatt =
            behandling.copy(
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.INNVILGET,
                vedtakstidspunkt = SporbarUtils.now(),
            )
        behandlingRepository.update(iverksatt)
        forrigeBehandling = iverksatt
    }

    private fun Periode.tilUtgiftsperiode(barn: List<UUID>) =
        UtgiftsperiodeDto(
            årMånedFra = fom,
            årMånedTil = tom,
            periode = Månedsperiode(fom, tom),
            barn = barn,
            utgifter = utgifter,
            sanksjonsårsak = null,
            periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
            aktivitetstype = aktivitet,
        )

    private fun januar(år: Int) = YearMonth.of(år, 1)

    private fun februar(år: Int) = YearMonth.of(år, 2)

    private fun mars(år: Int) = YearMonth.of(år, 3)

    private fun april(år: Int) = YearMonth.of(år, 4)

    private fun desember(år: Int) = YearMonth.of(år, 12)
}
