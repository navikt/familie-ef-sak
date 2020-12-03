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


    /* TODO MYCKET VIKTIG NOE MED OPPDATERA BEHANDLINGS_ID PÅ ALLA EFTERFØLJANDE ANDELAR?*/

    @BeforeEach
    internal fun setUp() {
        fagsak = fagsakRepository.insert(fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD))
        førstegangsbehandling = behandlingRepository.insert(behandling(fagsak = fagsak))
        logger.info("Første behandlingen: {}", førstegangsbehandling.id)
        periode1 =
                Andel(100, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), ursprungsbehandlingId = førstegangsbehandling.id)
        periode2 =
                Andel(200, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), ursprungsbehandlingId = førstegangsbehandling.id)
        opprettTilkjentYtelse(førstegangsbehandling, periode1, periode2)
    }

    @Test
    fun `Førstegangsbehandling skal konsistensavstemmer når avstemmingsdato er i perioden`() {
        assertKonsistensavstemming(datoForAvstemming = periode1.stønadFom,
                                   feilmelding = "Skal returnere førstegangsbehandling når avstemmingsdato er i perioden",
                                   førstegangsbehandling.eksternId.id)
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
                Andel(100, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 4, 30), ursprungsbehandlingId = revurderingBehandling.id)
        val revurderingPeriode3 =
                Andel(200, LocalDate.of(2021, 5, 1), LocalDate.of(2021, 12, 31), ursprungsbehandlingId = revurderingBehandling.id)
        opprettTilkjentYtelse(revurderingBehandling,
                              periode1,
                              revurderingPeriode2,
                              revurderingPeriode3)

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2019, 1, 1),
                                   feilmelding = "Skal returnere begge behandlingene når man konsistensavstemmer før periodene",
                                   førstegangsbehandling.eksternId.id, revurderingBehandling.eksternId.id)

        assertKonsistensavstemming(datoForAvstemming = periode1.stønadFom,
                                   feilmelding = "Skal returnere begge behandlingene når man kosnsistensavstemmer i den første perioden",
                                   førstegangsbehandling.eksternId.id, revurderingBehandling.eksternId.id)

        assertKonsistensavstemming(datoForAvstemming = revurderingPeriode2.stønadFom,
                                   feilmelding = "Skal returnere revurdering når man kosnsistensavstemmer i den andre perioden",
                                   revurderingBehandling.eksternId.id)

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2022, 1, 1),
                                   feilmelding = "Skal ikke returnere noe når man kosnsistensavstemmer etter periodene")
    }

    /*
    Case 4:
    Behandling A: Utbetaling fom. [2020-01-01 - 2020-12-31, 2021-01-01 - 2021-12-31]
    Revurdering B: Opphør på utbetaling fom 2020-11-01
        */
    @Test
    fun `Opphør i november i førsta perioden`() {
        val opphør = behandlingRepository.insert(behandling(fagsak = fagsak))
        val opphørsAndel = Andel(100, LocalDate.of(2020, 10, 1), LocalDate.of(2020, 10, 31), ursprungsbehandlingId = opphør.id)

        opprettTilkjentYtelse(opphør, opphørsAndel, periode2)

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2020, 2, 1),
                                   feilmelding = "Skal returnere den nyaste behandlingen når man konsistensavstemmer før periodene",
                                   opphør.eksternId.id)

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2022, 1, 1),
                                   feilmelding = "Skal ikke returnere noe når man konsistensavstemmer etter periodene")
    }


    /*
    Case 4:
    Behandling A: Utbetaling fom. [2020-01-01 - 2020-12-31, 2021-01-01 - 2021-12-31]
    Revurdering B: Ny utbetaling etter periodene fom. 2021-03-01 - 2023-03-31
    Opphør C: Opphør på utbetaling fra A fom 2020-11-01 => [2020-01-01 - 2020-10-31, 2022-03-01 - 2023-03-31]
    */
    @Test
    fun `En tredje behandling endrer på første perioden i den første behandlingen`() {
        val revurdering = behandlingRepository.insert(behandling(fagsak = fagsak))
        val nyAndel = Andel(50, LocalDate.of(2022, 3, 1), LocalDate.of(2023, 3, 31), ursprungsbehandlingId = revurdering.id)

        opprettTilkjentYtelse(revurdering, periode1, periode2, nyAndel)

        val tilkjenteYtelserSomFårOpphørsdato = tilkjentYtelseRepository.findAll()
        val opphørsBehandling = behandlingRepository.insert(behandling(fagsak = fagsak))

        opprettTilkjentYtelse(opphørsBehandling, periode1.copy(stønadTom = LocalDate.of(2020, 10, 31)), nyAndel)

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2020, 11, 1),
                                   feilmelding = "Skal returnere når man konsistensavstemmer etter periodene",
                                   opphørsBehandling.eksternId.id)
    }

    /*
    Case 5:
    Behandling A: Utbetaling fom. [2020-01-01 - 2020-12-31, 2021-01-01 - 2021-12-31]
    Revurdering B: Opphør på andre periode fra A fom 2021-11-01 => [2020-01-01 - 2020-12-31, 2021-01-01 - 2021-11-01] opphør 2021-01-01
    Revurdering C: Endrer den andre perioden på nytt, [2020-01-01 - 2020-12-31, 2021-01-01 - 2021-03-31]
    */
    @Test
    fun `En tredje behandling endrer på andre perioden i den første behandlingen`() {
        val opphørPaAndraPeriodeBehandling1 = behandlingRepository.insert(behandling(fagsak = fagsak))

        opprettTilkjentYtelse(opphørPaAndraPeriodeBehandling1, periode1, periode2.copy(stønadTom = LocalDate.of(2021, 11, 30)))
        val opphørPaAndraPeriodeBehandling2 = behandlingRepository.insert(behandling(fagsak = fagsak))

        opprettTilkjentYtelse(opphørPaAndraPeriodeBehandling2, periode1, periode2.copy(stønadTom = LocalDate.of(2021, 3, 31)))

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2020, 11, 1),
                                   feilmelding = "Skal returnere når man konsistensavstemmer etter periodene",
                                   førstegangsbehandling.eksternId.id, opphørPaAndraPeriodeBehandling2.eksternId.id)
    }

    data class Andel(val beløp: Int, val stønadFom: LocalDate, val stønadTom: LocalDate, val ursprungsbehandlingId: UUID)

    private fun opprettTilkjentYtelse(behandling: Behandling, vararg andel: Andel) {

        val andelerTilkjentYtelse = andel.map {
            AndelTilkjentYtelseDTO(beløp = it.beløp,
                                   stønadFom = it.stønadFom,
                                   stønadTom = it.stønadTom,
                                   personIdent = "1",
                                   ursprungsbehandlingId = it.ursprungsbehandlingId)
        }
        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelseDTO("1",
                                                                      LocalDate.now(),
                                                                      UUID.randomUUID(),
                                                                      behandling.id,
                                                                      andelerTilkjentYtelse))
        behandlingRepository.update(behandling.copy(status = BehandlingStatus.FERDIGSTILT))
    }

    private fun assertKonsistensavstemming(datoForAvstemming: LocalDate, feilmelding: String, vararg externBehandlingId: Long) {
        val stønadstype = Stønadstype.OVERGANGSSTØNAD
        val oppdragIdForFagsystem = tilkjentYtelseRepository.finnAktiveBehandlinger(datoForAvstemming = datoForAvstemming,
                                                                                    stønadstype = stønadstype)
        val faktiskBehandlingIdn = oppdragIdForFagsystem.map { it.behandlingsId }
        try {
            assertThat(faktiskBehandlingIdn)
                    .withFailMessage("$feilmelding - forventet:${externBehandlingId.toList()}, faktisk:$faktiskBehandlingIdn")
                    .containsExactlyInAnyOrder(*externBehandlingId.toTypedArray())
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
