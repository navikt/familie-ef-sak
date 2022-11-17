package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.behandling.RevurderingService
import no.nav.familie.ef.sak.ekstern.bisys.lagAndelHistorikkDto
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
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

internal class RevurderingServiceTest {

    val barnRepository = mockk<BarnRepository>()
    val vedtakHistorikkService = mockk<VedtakHistorikkService>()
    val vedtakService = mockk<VedtakService>()
    val revurderingService: RevurderingService = RevurderingService(
        søknadService = mockk(),
        behandlingService = mockk(),
        oppgaveService = mockk(),
        vurderingService = mockk(),
        grunnlagsdataService = mockk(),
        taskRepository = mockk(),
        barnService = mockk(),
        fagsakService = mockk(),
        vedtakService = vedtakService,
        vedtakHistorikkService = vedtakHistorikkService,
        barnRepository = barnRepository
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

    val førsteAndelFraOgMedDato = LocalDate.now().minusMonths(2)
    val andelHistorikkDto = lagAndelHistorikkDto(fraOgMed = førsteAndelFraOgMedDato, tilOgMed = LocalDate.now(), behandlingBarn = listOf(historiskBehandlingsbarn), beløp = 0, endring = null)
    val sisteAndelTilOgMed = LocalDate.now().plusMonths(3)
    val andelHistorikkDto2 = lagAndelHistorikkDto(fraOgMed = LocalDate.now().plusMonths(1), tilOgMed = sisteAndelTilOgMed, behandlingBarn = listOf(historiskBehandlingsbarn), beløp = 1, endring = null)

    @BeforeEach
    fun setup() {
        every { barnRepository.findByBehandlingId(revurdering.id) } returns listOf(barn)
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto)
        every { barnRepository.findAllById(listOf(historiskBehandlingsbarn.id)) } returns listOf(historiskBehandlingsbarn)
        every { vedtakService.lagreVedtak(any(), revurdering.id, StønadType.BARNETILSYN) } returns revurdering.id
        every { vedtakService.hentVedtak(forrigeBehandling.id) } returns vedtak(forrigeBehandling.id, ResultatType.INNVILGE).copy(tilleggsstønad = TilleggsstønadWrapper(false, listOf(), "Testbegrunnelse tilleggsstønad"))
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling hvis satsendring `() {
        revurderingService.kopierVedtakHvisSatsendring(BehandlingÅrsak.SATSENDRING, fagsak = fagsak, revurdering = revurdering, forrigeBehandling.id)

        val expectedUtgiftsperiodeDto = UtgiftsperiodeDto(
            årMånedFra = YearMonth.from(førsteAndelFraOgMedDato),
            årMånedTil = YearMonth.now(),
            periode = Månedsperiode(førsteAndelFraOgMedDato, LocalDate.now()),
            barn = listOf(barn.id),
            utgifter = 0,
            erMidlertidigOpphør = false
        )
        val expectedVedtakDto = InnvilgelseBarnetilsyn(
            begrunnelse = "Satsendring barnetilsyn",
            perioder = listOf(expectedUtgiftsperiodeDto),
            perioderKontantstøtte = listOf(),
            tilleggsstønad = TilleggsstønadDto(
                harTilleggsstønad = false,
                perioder = listOf(),
                begrunnelse = "Testbegrunnelse tilleggsstønad"
            ),
            resultatType = ResultatType.INNVILGE,
            _type = "InnvilgelseBarnetilsyn"
        )

        verify { vedtakService.lagreVedtak(expectedVedtakDto, revurdering.id, StønadType.BARNETILSYN) }
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling - sjekk kopiering av utgiftsbeløp`() {
        val andelMedUtgift = andelHistorikkDto.andel.copy(utgifter = BigDecimal.valueOf(1000))
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto.copy(andel = andelMedUtgift))

        val vedtakDto = revurderingService.mapTilBarnetilsynVedtak(fagsak.id, listOf(barn), forrigeBehandling.id) as InnvilgelseBarnetilsyn

        assertThat(vedtakDto.perioder.first().utgifter).isEqualTo(1000)
        assertThat(vedtakDto.perioder).hasSize(1)
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling - sjekk kopiering av flere perioder med forskjellig utgift`() {
        val andelMedUtgift = andelHistorikkDto.andel.copy(utgifter = BigDecimal.valueOf(1000))
        val andelMedUtgift2 = andelHistorikkDto2.andel.copy(utgifter = BigDecimal.valueOf(2000))
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto.copy(andel = andelMedUtgift), andelHistorikkDto2.copy(andel = andelMedUtgift2))

        val vedtakDto = revurderingService.mapTilBarnetilsynVedtak(fagsak.id, listOf(barn), forrigeBehandling.id) as InnvilgelseBarnetilsyn

        assertThat(vedtakDto.perioder).hasSize(2)
        assertThat(vedtakDto.perioder.find { it.periode.fom == YearMonth.from(førsteAndelFraOgMedDato) }?.utgifter).isEqualTo(1000)
        assertThat(vedtakDto.perioder.find { it.periode.tom == YearMonth.from(sisteAndelTilOgMed) }?.utgifter).isEqualTo(2000)
    }

    @Test
    fun `Skal kopiere vedtak innhold til ny behandling - sjekk kopiering av kontantstøtteperioder`() {
        val andelMedUtgift = andelHistorikkDto.andel.copy(kontantstøtte = 1000)
        val andelMedUtgift2 = andelHistorikkDto2.andel.copy(kontantstøtte = 2000)
        every { vedtakHistorikkService.hentAktivHistorikk(any()) } returns listOf(andelHistorikkDto.copy(andel = andelMedUtgift), andelHistorikkDto2.copy(andel = andelMedUtgift2))

        val vedtakDto = revurderingService.mapTilBarnetilsynVedtak(fagsak.id, listOf(barn), forrigeBehandling.id) as InnvilgelseBarnetilsyn

        assertThat(vedtakDto.perioderKontantstøtte).hasSize(2)
        assertThat(vedtakDto.perioderKontantstøtte.find { it.periode.fom == YearMonth.from(førsteAndelFraOgMedDato) }?.beløp).isEqualTo(1000)
        assertThat(vedtakDto.perioderKontantstøtte.find { it.periode.tom == YearMonth.from(sisteAndelTilOgMed) }?.beløp).isEqualTo(2000)
    }
}
