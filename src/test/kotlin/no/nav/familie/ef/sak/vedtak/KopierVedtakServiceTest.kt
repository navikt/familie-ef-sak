package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
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
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.erSammenhengende
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

    val behandlingService = mockk<BehandlingService>()
    val kopierVedtakService: KopierVedtakService =
        KopierVedtakService(
            barnRepository = barnRepository,
            vedtakService = vedtakService,
            vedtakHistorikkService = vedtakHistorikkService,
            behandlingService = behandlingService,
        )

    val fagsak = fagsak()
    val forrigeBehandling = behandling(fagsak)
    val revurdering = behandling(fagsak = fagsak, forrigeBehandlingId = forrigeBehandling.id, årsak = BehandlingÅrsak.SATSENDRING)

    val historiskBehandlingsbarn =
        behandlingBarn(
            id = UUID.randomUUID(),
            behandlingId = forrigeBehandling.id,
            søknadBarnId = UUID.randomUUID(),
            personIdent = "01010112345",
            navn = "Ola",
            fødselTermindato = LocalDate.now(),
        )

    val barn =
        behandlingBarn(
            id = UUID.randomUUID(),
            behandlingId = revurdering.id,
            søknadBarnId = UUID.randomUUID(),
            personIdent = "01010112345",
            navn = "Ola",
            fødselTermindato = LocalDate.now(),
        )

    val forventetFomYearMonth = YearMonth.from(BeregningBarnetilsynUtil.satserForBarnetilsyn.maxOf { it.periode.fom })
    val førsteAndelFraOgMedDato = forventetFomYearMonth.minusMonths(2)
    val førsteAndelTilOgMedDato = forventetFomYearMonth.plusMonths(6)
    val andreAndelFraOgMed = førsteAndelTilOgMedDato.plusMonths(1)
    val andreAndelTilOgMedDato = førsteAndelTilOgMedDato.plusMonths(3)
    val andelHistorikkDto = lagAndelHistorikkDto(fraOgMed = førsteAndelFraOgMedDato.atDay(1), tilOgMed = førsteAndelTilOgMedDato.atEndOfMonth(), behandlingBarn = listOf(historiskBehandlingsbarn), beløp = 0, endring = null)
    val andelHistorikkDto2 = lagAndelHistorikkDto(fraOgMed = andreAndelFraOgMed.atDay(1), tilOgMed = andreAndelTilOgMedDato.atEndOfMonth(), behandlingBarn = listOf(historiskBehandlingsbarn), beløp = 1, endring = null)

    @BeforeEach
    fun setup() {
        every { barnRepository.findByBehandlingId(revurdering.id) } returns listOf(barn)
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto)
        every { barnRepository.findAllById(listOf(historiskBehandlingsbarn.id)) } returns listOf(historiskBehandlingsbarn)
        every { vedtakService.lagreVedtak(any(), revurdering.id, StønadType.BARNETILSYN) } returns revurdering.id
        every { vedtakService.hentVedtak(forrigeBehandling.id) } returns vedtak(forrigeBehandling.id, ResultatType.INNVILGE).copy(tilleggsstønad = TilleggsstønadWrapper(listOf(), "Testbegrunnelse tilleggsstønad"))
        val barnetilsynperiode =
            Barnetilsynperiode(
                periode = Månedsperiode(YearMonth.now()),
                utgifter = 1000,
                barn = listOf(),
                periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
            )
        every { vedtakService.hentVedtak(not(forrigeBehandling.id)) } returns
            vedtak(UUID.randomUUID(), ResultatType.INNVILGE)
                .copy(barnetilsyn = BarnetilsynWrapper(listOf(barnetilsynperiode), "begrunnelse"))
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling hvis satsendring`() {
        val andelMedUtgift = andelHistorikkDto.andel.copy(utgifter = BigDecimal.valueOf(1000))
        val andelHistorikkDtos = listOf(andelHistorikkDto.copy(andel = andelMedUtgift))
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns andelHistorikkDtos
        every { behandlingService.hentBehandling(revurdering.id) } returns revurdering

        val vedtakDto =
            kopierVedtakService.lagVedtakDtoBasertPåTidligereVedtaksperioder(
                fagsakId = fagsak.id,
                revurderingId = revurdering.id,
                forrigeBehandlingId = forrigeBehandling.id,
            ) as InnvilgelseBarnetilsyn

        assertThat(vedtakDto.perioder).hasSize(1)
        assertThat(vedtakDto.perioder.first().utgifter).isEqualTo(1000)
        assertThat(
            vedtakDto.perioder
                .first()
                .periode.fom,
        ).isEqualTo(forventetFomYearMonth)
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
        assertThat(vedtakDto.perioder.find { it.periode.tom == YearMonth.from(andreAndelTilOgMedDato) }?.utgifter).isEqualTo(2000)
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling - sjekk kopiering av kontantstøtteperioder`() {
        val andelMedUtgift = andelHistorikkDto.andel.copy(kontantstøtte = 1000)
        val andelMedUtgift2 = andelHistorikkDto2.andel.copy(kontantstøtte = 2000)
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto.copy(andel = andelMedUtgift), andelHistorikkDto2.copy(andel = andelMedUtgift2))

        val vedtakDto = kopierVedtakService.mapTilBarnetilsynVedtak(fagsak.id, listOf(barn), forrigeBehandling.id) as InnvilgelseBarnetilsyn

        assertThat(vedtakDto.perioderKontantstøtte).hasSize(2)
        assertThat(vedtakDto.perioderKontantstøtte.find { it.periode.fom == YearMonth.from(forventetFomYearMonth) }?.beløp).isEqualTo(1000)
        assertThat(vedtakDto.perioderKontantstøtte.find { it.periode.tom == YearMonth.from(andreAndelTilOgMedDato) }?.beløp).isEqualTo(2000)
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling - sjekk kopiering av tilleggsstønadsperioder`() {
        val andelMedUtgift = andelHistorikkDto.andel.copy(tilleggsstønad = 1000)
        val andelMedUtgift2 = andelHistorikkDto2.andel.copy(tilleggsstønad = 2000)
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto.copy(andel = andelMedUtgift), andelHistorikkDto2.copy(andel = andelMedUtgift2))

        val vedtakDto = kopierVedtakService.mapTilBarnetilsynVedtak(fagsak.id, listOf(barn), forrigeBehandling.id) as InnvilgelseBarnetilsyn

        assertThat(vedtakDto.tilleggsstønad.perioder).hasSize(2)
        assertThat(
            vedtakDto.tilleggsstønad.perioder
                .find { it.periode.fom == YearMonth.from(forventetFomYearMonth) }
                ?.beløp,
        ).isEqualTo(1000)
        assertThat(
            vedtakDto.tilleggsstønad.perioder
                .find { it.periode.tom == YearMonth.from(andreAndelTilOgMedDato) }
                ?.beløp,
        ).isEqualTo(2000)
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling - legg til perioder uten stønad`() {
        val andelHistorikkDto = lagAndelHistorikkDto(fraOgMed = førsteAndelFraOgMedDato.atDay(1), tilOgMed = førsteAndelTilOgMedDato.atEndOfMonth(), behandlingBarn = listOf(historiskBehandlingsbarn), beløp = 7000, endring = null)
        val andelHistorikkDto2 = lagAndelHistorikkDto(fraOgMed = førsteAndelTilOgMedDato.plusMonths(2).atDay(1), tilOgMed = andreAndelTilOgMedDato.plusMonths(3).atEndOfMonth(), behandlingBarn = listOf(historiskBehandlingsbarn), beløp = 5000, endring = null)
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto, andelHistorikkDto2)

        val vedtakDto = kopierVedtakService.mapTilBarnetilsynVedtak(fagsak.id, listOf(barn), forrigeBehandling.id) as InnvilgelseBarnetilsyn

        assertThat(vedtakDto.perioder).hasSize(3)
        assertThat(vedtakDto.perioder[1].utgifter).isEqualTo(0)
        assertThat(vedtakDto.perioder[1].barn).hasSize(0)
        assertThat(vedtakDto.perioder[1].periodetype).isEqualTo(PeriodetypeBarnetilsyn.OPPHØR)
        assertThat(vedtakDto.perioder.erSammenhengende()).isTrue
    }
}
