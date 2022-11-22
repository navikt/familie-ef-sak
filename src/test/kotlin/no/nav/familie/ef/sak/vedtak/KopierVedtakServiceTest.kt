package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynUtil
import no.nav.familie.ef.sak.ekstern.bisys.lagAndelHistorikkDto
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.vedtak.KopierVedtakService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.BarnetilsynWrapper
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class KopierVedtakServiceTest {

    val barnRepository = mockk<BarnRepository>()
    val vedtakHistorikkService = mockk<VedtakHistorikkService>()
    val vedtakService = mockk<VedtakService>()

    val kopierVedtakService: KopierVedtakService = KopierVedtakService(
        barnRepository = barnRepository,
        vedtakService = vedtakService,
        vedtakHistorikkService = vedtakHistorikkService
    )

    val fagsak = fagsak()
    val forrigeBehandling = behandling(fagsak)
    val revurdering = behandling(fagsak = fagsak, forrigeBehandlingId = forrigeBehandling.id)

    val historiskBehandlingsbarn = behandlingBarn(
        id = UUID.randomUUID(),
        behandlingId = forrigeBehandling.id,
        søknadBarnId = UUID.randomUUID(),
        personIdent = "01010112345",
        navn = "Ola",
        fødselTermindato = LocalDate.now()
    )

    val barn = behandlingBarn(
        id = UUID.randomUUID(),
        behandlingId = revurdering.id,
        søknadBarnId = UUID.randomUUID(),
        personIdent = "01010112345",
        navn = "Ola",
        fødselTermindato = LocalDate.now()
    )
    val forventetFomYearMonth = YearMonth.from(BeregningBarnetilsynUtil.ikkeVedtatteSatserForBarnetilsyn.maxOf { it.periode.fom })
    val år = if (YearMonth.now().month.value > 6) YearMonth.now().year else YearMonth.now().year - 1
    val førsteAndelFraOgMedDato = LocalDate.of(år, 11, 1)
    val førsteAndelTilOgMedDato = LocalDate.of(år + 1, 6, 30)
    val sisteAndelTilOgMedDato = førsteAndelTilOgMedDato.plusMonths(3)
    val andelHistorikkDto = lagAndelHistorikkDto(fraOgMed = førsteAndelFraOgMedDato, tilOgMed = førsteAndelTilOgMedDato, behandlingBarn = listOf(historiskBehandlingsbarn), beløp = 0, endring = null)
    val andelHistorikkDto2 = lagAndelHistorikkDto(fraOgMed = førsteAndelTilOgMedDato.plusMonths(1), tilOgMed = sisteAndelTilOgMedDato, behandlingBarn = listOf(historiskBehandlingsbarn), beløp = 1, endring = null)

    @BeforeEach
    fun setup() {
        every { barnRepository.findByBehandlingId(revurdering.id) } returns listOf(barn)
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto)
        every { barnRepository.findAllById(listOf(historiskBehandlingsbarn.id)) } returns listOf(historiskBehandlingsbarn)
        every { vedtakService.lagreVedtak(any(), revurdering.id, StønadType.BARNETILSYN) } returns revurdering.id
        every { vedtakService.hentVedtak(forrigeBehandling.id) } returns vedtak(forrigeBehandling.id, ResultatType.INNVILGE).copy(tilleggsstønad = TilleggsstønadWrapper(false, listOf(), "Testbegrunnelse tilleggsstønad"))
        every { vedtakService.hentVedtak(not(forrigeBehandling.id)) } returns vedtak(UUID.randomUUID(), ResultatType.INNVILGE).copy(
            barnetilsyn = BarnetilsynWrapper(
                listOf(Barnetilsynperiode(periode = Månedsperiode(YearMonth.now()), erMidlertidigOpphør = false, utgifter = 1000, barn = listOf())),
                "begrunnelse"
            )
        )
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling hvis satsendring`() {
        val andelMedUtgift = andelHistorikkDto.andel.copy(utgifter = BigDecimal.valueOf(1000))
        val andelHistorikkDtos = listOf(andelHistorikkDto.copy(andel = andelMedUtgift))
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns andelHistorikkDtos
        val vedtakSlot = slot<InnvilgelseBarnetilsyn>()
        every { vedtakService.lagreVedtak(capture(vedtakSlot), revurdering.id, StønadType.BARNETILSYN) } answers { UUID.randomUUID() }

        kopierVedtakService.kopierOgLagreForrigeVedtakTilNyRevurderingHvisSatsendring(BehandlingÅrsak.SATSENDRING, fagsak = fagsak, revurderingId = revurdering.id, forrigeBehandlingId = forrigeBehandling.id)

        assertThat(vedtakSlot.captured.perioder).hasSize(1)
        assertThat(vedtakSlot.captured.perioder.first().utgifter).isEqualTo(1000)
        assertThat(vedtakSlot.captured.perioder.first().periode.fom).isEqualTo(forventetFomYearMonth)
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling - sjekk kopiering av utgiftsbeløp`() {
        val andelMedUtgift = andelHistorikkDto.andel.copy(utgifter = BigDecimal.valueOf(1000))
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto.copy(andel = andelMedUtgift))

        val vedtakDto = kopierVedtakService.mapTilBarnetilsynVedtak(fagsak.id, listOf(barn), forrigeBehandling.id) as InnvilgelseBarnetilsyn

        assertThat(vedtakDto.perioder.first().utgifter).isEqualTo(1000)
        assertThat(vedtakDto.perioder).hasSize(1)
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling - sjekk kopiering av flere perioder med forskjellig utgift`() {
        val andelMedUtgift = andelHistorikkDto.andel.copy(utgifter = BigDecimal.valueOf(1000))
        val andelMedUtgift2 = andelHistorikkDto2.andel.copy(utgifter = BigDecimal.valueOf(2000))
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto.copy(andel = andelMedUtgift), andelHistorikkDto2.copy(andel = andelMedUtgift2))

        val vedtakDto = kopierVedtakService.mapTilBarnetilsynVedtak(fagsak.id, listOf(barn), forrigeBehandling.id) as InnvilgelseBarnetilsyn

        assertThat(vedtakDto.perioder).hasSize(2)
        assertThat(vedtakDto.perioder.find { it.periode.fom == YearMonth.from(forventetFomYearMonth) }?.utgifter).isEqualTo(1000)
        assertThat(vedtakDto.perioder.find { it.periode.tom == YearMonth.from(sisteAndelTilOgMedDato) }?.utgifter).isEqualTo(2000)
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling - sjekk kopiering av kontantstøtteperioder`() {
        val andelMedUtgift = andelHistorikkDto.andel.copy(kontantstøtte = 1000)
        val andelMedUtgift2 = andelHistorikkDto2.andel.copy(kontantstøtte = 2000)
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto.copy(andel = andelMedUtgift), andelHistorikkDto2.copy(andel = andelMedUtgift2))

        val vedtakDto = kopierVedtakService.mapTilBarnetilsynVedtak(fagsak.id, listOf(barn), forrigeBehandling.id) as InnvilgelseBarnetilsyn

        assertThat(vedtakDto.perioderKontantstøtte).hasSize(2)
        assertThat(vedtakDto.perioderKontantstøtte.find { it.periode.fom == YearMonth.from(forventetFomYearMonth) }?.beløp).isEqualTo(1000)
        assertThat(vedtakDto.perioderKontantstøtte.find { it.periode.tom == YearMonth.from(sisteAndelTilOgMedDato) }?.beløp).isEqualTo(2000)
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling - sjekk kopiering av tilleggsstønadsperioder`() {
        val andelMedUtgift = andelHistorikkDto.andel.copy(tilleggsstønad = 1000)
        val andelMedUtgift2 = andelHistorikkDto2.andel.copy(tilleggsstønad = 2000)
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto.copy(andel = andelMedUtgift), andelHistorikkDto2.copy(andel = andelMedUtgift2))

        val vedtakDto = kopierVedtakService.mapTilBarnetilsynVedtak(fagsak.id, listOf(barn), forrigeBehandling.id) as InnvilgelseBarnetilsyn

        assertThat(vedtakDto.tilleggsstønad.perioder).hasSize(2)
        assertThat(vedtakDto.tilleggsstønad.perioder.find { it.periode.fom == YearMonth.from(forventetFomYearMonth) }?.beløp).isEqualTo(1000)
        assertThat(vedtakDto.tilleggsstønad.perioder.find { it.periode.tom == YearMonth.from(sisteAndelTilOgMedDato) }?.beløp).isEqualTo(2000)
    }
}
