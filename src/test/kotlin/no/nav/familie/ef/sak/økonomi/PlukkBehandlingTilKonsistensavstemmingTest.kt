package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*

internal class PlukkBehandlingTilKonsistensavstemmingTest : OppslagSpringRunnerTest() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var tilkjentYtelseService: TilkjentYtelseService

    private lateinit var fagsak: Fagsak
    private lateinit var førstegangsbehandling: Behandling

    private lateinit var periode1: Andel
    private lateinit var periode2: Andel

    @BeforeEach
    internal fun setUp() {
        fagsak = fagsakRepository.insert(fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD))
        førstegangsbehandling = behandlingRepository.insert(behandling(fagsak = fagsak))
        logger.info("Første behandlingen: {}", førstegangsbehandling.id)
        periode1 =
                Andel(100, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), kildeBehandlingId = førstegangsbehandling.id)
        periode2 =
                Andel(200, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), kildeBehandlingId = førstegangsbehandling.id)
        opprettTilkjentYtelse(førstegangsbehandling, periode1, periode2)
    }

    @Test
    fun `Førstegangsbehandling skal konsistensavstemmer når avstemmingsdato er i perioden`() {
        assertKonsistensavstemming(datoForAvstemming = periode1.stønadFom,
                                   feilmelding = "Skal returnere førstegangsbehandling når avstemmingsdato er i perioden",
                                   1, 2)
    }

    /*
    Case 2:
    Behandling A: Utbetaling fom. [2020-01-01 - 2020-12-31, 2021-01-01 - 2021-12-31]
    Revurdering B: Endring på utbetaling i andra perioden, fom 2021-01-01, første behandlingen får ett opphørsdato fom 2021-01-01
    */
    @Test
    fun `Revurdering med nytt beløp fra mai`() {
        val revurderingBehandling = behandlingRepository.insert(behandling(fagsak = fagsak))
        logger.info("Oppretter revurdering: {}", revurderingBehandling.id)
        val revurderingPeriode2 =
                Andel(100, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 4, 30), kildeBehandlingId = revurderingBehandling.id)
        val revurderingPeriode3 =
                Andel(200, LocalDate.of(2021, 5, 1), LocalDate.of(2021, 12, 31), kildeBehandlingId = revurderingBehandling.id)
        opprettTilkjentYtelse(revurderingBehandling,
                              periode1,
                              revurderingPeriode2,
                              revurderingPeriode3)

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2019, 1, 1),
                                   feilmelding = "Skal returnere begge behandlingene når man konsistensavstemmer før periodene",
                                   1,3,4)

        assertKonsistensavstemming(datoForAvstemming = periode1.stønadFom,
                                   feilmelding = "Skal returnere begge behandlingene når man kosnsistensavstemmer i den første perioden",
                                   1,3,4)

        assertKonsistensavstemming(datoForAvstemming = revurderingPeriode2.stønadFom,
                                   feilmelding = "Skal returnere revurdering når man kosnsistensavstemmer i den andre perioden",
                                   3,4)

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2022, 1, 1),
                                   feilmelding = "Skal ikke returnere noe når man kosnsistensavstemmer etter periodene")
    }

    /*
    Case 4:
    Behandling A: Utbetaling fom. [2020-01-01 - 2020-12-31(1), 2021-01-01 - 2021-12-31(2)]
    Revurdering B: Opphør på utbetaling fom 2020-11-01 => [2020-01-01 - 2020-10-31(3), 2021-01-01 - 2021-12-31(4)]
        */
    @Test
    fun `Opphør i november i førsta perioden`() {
        val opphør = behandlingRepository.insert(behandling(fagsak = fagsak))
        val opphørsAndel = Andel(100, LocalDate.of(2020, 10, 1), LocalDate.of(2020, 10, 31), kildeBehandlingId = opphør.id)

        opprettTilkjentYtelse(opphør, opphørsAndel, periode2)

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2020, 2, 1),
                                   feilmelding = "Skal returnere den nyaste behandlingen når man konsistensavstemmer før periodene",
                                   3,4)

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2022, 1, 1),
                                   feilmelding = "Skal ikke returnere noe når man konsistensavstemmer etter periodene")
    }


    /*
    Case 4:
    Behandling A: Utbetaling fom. [2020-01-01 - 2020-12-31(1), 2021-01-01 - 2021-12-31(2)]
    Revurdering B: Ny utbetaling etter periodene fom. 2022-03-01 - 2023-03-31(3)
    Opphør C: Opphør på utbetaling fra A fom 2020-11-01 => [2020-01-01 - 2020-10-31(4), 2022-03-01 - 2023-03-31(5)]
    */
    @Test
    fun `En tredje behandling endrer på første perioden i den første behandlingen`() {
        val revurdering = behandlingRepository.insert(behandling(fagsak = fagsak))
        val nyAndel = Andel(50, LocalDate.of(2022, 3, 1), LocalDate.of(2023, 3, 31), kildeBehandlingId = revurdering.id)

        opprettTilkjentYtelse(revurdering, periode1, periode2, nyAndel)

        val opphørsBehandling = behandlingRepository.insert(behandling(fagsak = fagsak))

        opprettTilkjentYtelse(opphørsBehandling, periode1.copy(stønadTom = LocalDate.of(2020, 10, 31)), nyAndel)

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2020, 11, 1),
                                   feilmelding = "Skal returnere når man konsistensavstemmer etter periodene",
                                   5)
    }

    /*
    Case 5:
    Behandling A: Utbetaling fom. [2020-01-01 - 2020-12-31(1), 2021-01-01 - 2021-12-31(2)]
    Revurdering B: Opphør på andre periode fra A fom 2021-11-01 => [2020-01-01 - 2020-12-31(1), 2021-01-01 - 2021-11-01(3)] opphør 2021-01-01
    Revurdering C: Endrer den andre perioden på nytt, [2020-01-01 - 2020-12-31(1), 2021-01-01 - 2021-03-31(4)]
    */
    @Test
    fun `En tredje behandling endrer på andre perioden i den første behandlingen`() {
        val opphørPaAndraPeriodeBehandling1 = behandlingRepository.insert(behandling(fagsak = fagsak))

        opprettTilkjentYtelse(opphørPaAndraPeriodeBehandling1, periode1, periode2.copy(stønadTom = LocalDate.of(2021, 11, 30)))
        val opphørPaAndraPeriodeBehandling2 = behandlingRepository.insert(behandling(fagsak = fagsak))

        opprettTilkjentYtelse(opphørPaAndraPeriodeBehandling2, periode1, periode2.copy(stønadTom = LocalDate.of(2021, 3, 31)))

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2020, 11, 1),
                                   feilmelding = "Skal returnere når man konsistensavstemmer etter periodene",
                                   1,4)
    }

    data class Andel(val beløp: Int, val stønadFom: LocalDate, val stønadTom: LocalDate, val kildeBehandlingId: UUID)

    private fun opprettTilkjentYtelse(behandling: Behandling, vararg andel: Andel) {

        val andelerTilkjentYtelse = andel.map {
            AndelTilkjentYtelseDTO(beløp = it.beløp,
                                   stønadFom = it.stønadFom,
                                   stønadTom = it.stønadTom,
                                   personIdent = "1",
                                   inntektsreduksjon = 0,
                                   inntekt = 0,
                                   samordningsfradrag = 0,
                                   kildeBehandlingId = it.kildeBehandlingId)
        }
        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelseDTO("1",
                                                                      LocalDate.now(),
                                                                      UUID.randomUUID(),
                                                                      behandling.id,
                                                                      andelerTilkjentYtelse))
        tilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(behandling)
        behandlingRepository.update(behandling.copy(status = BehandlingStatus.FERDIGSTILT))
    }

    private fun assertKonsistensavstemming(datoForAvstemming: LocalDate, feilmelding: String, vararg periodeIdn: Long) {
        val stønadstype = Stønadstype.OVERGANGSSTØNAD
        val oppdragIdForFagsystem = tilkjentYtelseService.finnLøpendeUtbetalninger(datoForAvstemming = datoForAvstemming,
                                                                                    stønadstype = stønadstype)
        val faktiskePeriodeIdn = oppdragIdForFagsystem.flatMap { it.perioder }
        try {
            assertThat(faktiskePeriodeIdn)
                    .withFailMessage("$feilmelding - forventet:${periodeIdn.toList()}, faktisk:$faktiskePeriodeIdn")
                    .containsExactlyInAnyOrder(*periodeIdn.toTypedArray())
        } catch (e: Throwable) {
            tilkjentYtelseRepository.findAll()
                    .sortedByDescending { it.sporbar.opprettetTid }
                    .forEach {
                        logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(it))
                    }
            throw e
        }
    }
}