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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*

internal class PlukkBehandlingTilKonsistensavstemmingTest : OppslagSpringRunnerTest() {

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

    private val periode1 = Andel(100, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
    private val periode2 = Andel(100, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))



    /*TODO MYCKET VIKTIG NOE MED OPPDATERA BEHANDLINGS_ID PÅ ALLA EFTERFØLJANDE ANDELAR?*/

    @BeforeEach
    internal fun setUp() {
        fagsak = fagsakRepository.insert(fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD))
        førstegangsbehandling = opprettTilkjentYtelse(fagsak, periode1, periode2)
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
        val revurderingPeriode2 = Andel(100, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 4, 30))
        val revurderingPeriode3 = Andel(200, LocalDate.of(2021, 5, 1), LocalDate.of(2021, 12, 31))
        val revurderingBehandling = opprettTilkjentYtelse(fagsak,
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

    @Test
    fun `hejhej` () {
        val nyAndel = Andel(50, LocalDate.of(2022, 3, 1), LocalDate.of(2023, 3, 31))

        opprettTilkjentYtelse(fagsak, periode1, periode2, nyAndel)

        opprettTilkjentYtelse(fagsak, periode1, periode2.copy(stønadTom = periode2.stønadTom.minusDays(30)), nyAndel)

        val tralal = tilkjentYtelseRepository.finnNyesteTilkjentYtelse(fagsakId = fagsak.id)
        assertThat
    }

    /*
    Case 4:
    Behandling A: Utbetaling fom. [2020-01-01 - 2020-12-31, 2021-01-01 - 2021-12-31]
    Revurdering B: Opphør på utbetaling fom 2020-11-01
        */
    @Test
    fun `Opphør i november i førsta perioden`() {
        val opphørsAndel = Andel(100, LocalDate.of(2020, 10, 1), LocalDate.of(2020, 10, 31))

        val behandlingB = opprettTilkjentYtelse(fagsak, opphørsAndel, periode2)


        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2020, 2, 1),
                                   feilmelding = "Skal returnere den nyaste behandlingen når man konsistensavstemmer før periodene",
                                   behandlingB.eksternId.id)

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2020, 11, 1),
                                   feilmelding = "Skal ikke returnere noe når man konsistensavstemmer etter periodene")
    }


    /*
    Case 4:
    Behandling A: Utbetaling fom. [2020-01-01 - 2020-12-31, 2021-01-01 - 2021-12-31]
    Revurdering B: Ny utbetaling fom. 2021-03-01 - 2023-03-31
    Opphør C: Opphør på utbetaling fra A fom 2020-11-01 => [2020-01-01 - 2020-10-31, 2022-03-01 - 2023-03-31]
    */
    @Test
    fun `En tredje behandling endrer på første perioden i den første behandlingen`() {
        val nyAndel = Andel(50, LocalDate.of(2022, 3, 1), LocalDate.of(2023, 3, 31))

        opprettTilkjentYtelse(fagsak, periode1, periode2, nyAndel)

        val tilkjenteYtelserSomFårOpphørsdato = tilkjentYtelseRepository.findAll()

        val opphør = opprettTilkjentYtelse(fagsak, periode1.copy(stønadTom = LocalDate.of(2020, 10, 31)), nyAndel)

        tilkjenteYtelserSomFårOpphørsdato.forEach {
            assertThat(it.opphørFom)
                    .withFailMessage("Tidligere tilkjentYtelser skal oppdateres med opphørsdato")
                    .isEqualTo(periode1.stønadFom)
        }

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2020, 11, 1),
                                   feilmelding = "Skal returnere når man konsistensavstemmer etter periodene",
                                   opphør.eksternId.id)
    }

    /*
    Case 5:
    Behandling A: Utbetaling fom. [2020-01-01 - 2020-12-31, 2021-01-01 - 2021-12-31]
    Revurdering B: Opphør på andre periode fra A fom 2021-11-01 => [2020-01-01 - 2020-12-31, 2021-01-01 - 2021-11-01] opphør 2021-01-01
    Revurdering C: Endrer den andre perioden på nytt, [2020-01-01 - 2020-12-31, 2021-01-01 - 2021-03-31]
    */
    @Test
    fun `En tredje behandling endrer på andre perioden i den første behandlingen`() {
        val behandlingB = opprettTilkjentYtelse(fagsak, periode1, periode2.copy(stønadTom = LocalDate.of(2021, 11, 30)))

        val behandlingC = opprettTilkjentYtelse(fagsak, periode1, periode2.copy(stønadTom = LocalDate.of(2021, 3, 31)))

        assertKonsistensavstemming(datoForAvstemming = LocalDate.of(2020, 11, 1),
                                   feilmelding = "Skal returnere når man konsistensavstemmer etter periodene",
                                   førstegangsbehandling.eksternId.id, behandlingC.eksternId.id)
    }

    data class Andel(val beløp: Int, val stønadFom: LocalDate, val stønadTom: LocalDate)

    private fun opprettTilkjentYtelse(fagsak: Fagsak, vararg andel: Andel): Behandling {
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))

        val andelerTilkjentYtelse = andel.map {
            AndelTilkjentYtelseDTO(beløp = it.beløp,
                                   stønadFom = it.stønadFom,
                                   stønadTom = it.stønadTom,
                                   personIdent = "1",
                                   ursprungsbehandlingId = null)
        }
        tilkjentYtelseService.opprettTilkjentYtelse(TilkjentYtelseDTO("1",
                                                                      LocalDate.now(),
                                                                      UUID.randomUUID(),
                                                                      behandling.id,
                                                                      andelerTilkjentYtelse))
        behandlingRepository.update(behandling.copy(status = BehandlingStatus.FERDIGSTILT))
        return behandling
    }

    private fun assertKonsistensavstemming(datoForAvstemming: LocalDate, feilmelding: String, vararg externBehandlingId: Long) {
        val oppdragIdForFagsystem = tilkjentYtelseRepository.finnAktiveBehandlinger(datoForAvstemming = datoForAvstemming,
                                                                                    stønadstype = Stønadstype.OVERGANGSSTØNAD)
        val faktiskBehandlingIdn = oppdragIdForFagsystem.map { it.behandlingsId }
        assertThat(faktiskBehandlingIdn)
                .withFailMessage("$feilmelding - forventet:${externBehandlingId.toList()}, faktisk:$faktiskBehandlingIdn")
                .containsExactlyInAnyOrder(*externBehandlingId.toTypedArray())
    }

}
