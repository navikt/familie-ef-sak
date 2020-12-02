package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
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


    /*
        Case 1:
        Førstegangsbehandling A: Utbetaling fom. 01.10.2020 - 30.09.2021
        Behandling A skal konsistensavstemmes.
     */
    @Test
    fun `Case 1`() {
        val fagsak = fagsakRepository.insert(fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD))
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))


        val oppdragIdForFagsystem = tilkjentYtelseRepository.finnAktiveBehandlinger(datoForAvstemming = LocalDate.now(),
                                                                                    stønadstype = Stønadstype.OVERGANGSSTØNAD)

        assertThat(oppdragIdForFagsystem.size).isEqualTo(1)
        assertThat(oppdragIdForFagsystem[0].behandlingsId).isEqualTo(behandling.eksternId.id)


    }

    /*
    Case 2:
    Behandling A: Utbetaling fom. [01.10.2020 - 30.09.2021, 1.11.2021 - 1.11-2022]
    Revurdering B: Endring på utbetaling fom 01.11.2020
    Behandling B skal konsistensavstemmes, behandling A skal ikke konsistensavstemmes.
    */
    @Test
    fun `Case 2`() {
        val fagsak = fagsakRepository.insert(fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD))
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))


        val revurdering = behandlingRepository.insert(behandling(fagsak = fagsak))


        val oppdragIdForFagsystem = tilkjentYtelseRepository.finnAktiveBehandlinger(datoForAvstemming = LocalDate.now(),
                                                                                    stønadstype = Stønadstype.OVERGANGSSTØNAD)
        assertThat(oppdragIdForFagsystem.size).isEqualTo(1)
        assertThat(oppdragIdForFagsystem[0].behandlingsId).isEqualTo(revurdering.eksternId.id)
    }

    /*
    Case 3:

    */
    @Test
    fun `Case 3`() {
        val fagsak = fagsakRepository.insert(fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD))
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))


        val revurdering = behandlingRepository.insert(behandling(fagsak = fagsak))


        val oppdragIdForFagsystem = tilkjentYtelseRepository.finnAktiveBehandlinger(datoForAvstemming = LocalDate.now(),
                                                                                    stønadstype = Stønadstype.OVERGANGSSTØNAD)
        assertThat(oppdragIdForFagsystem.size).isEqualTo(2)
        assertThat(oppdragIdForFagsystem[0].behandlingsId).isEqualTo(behandling.eksternId.id)
        assertThat(oppdragIdForFagsystem[1].behandlingsId).isEqualTo(revurdering.eksternId.id)
    }


    /*
    Case 4:
    Behandling A: Utbetaling fom. 01.10.2020 - 31.09.2021
    Revurdering B: Opphør på utbetaling fom 01.11.2020
    Behandling B skal ikke konsistensavstemmes, behandling A skal ikke konsistensavstemmes. (Behandling B har ingen egne utbetalingsperioder, og behandling A sine utbetalingsperioder er ikke lenger gjeldende)
    */
    @Test
    fun `Case 4`() {
        val fagsak = fagsakRepository.insert(fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD))
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))


        val revurdering = behandlingRepository.insert(behandling(fagsak = fagsak))

        val oppdragIdForFagsystem = tilkjentYtelseRepository.finnAktiveBehandlinger(datoForAvstemming = LocalDate.now(),
                                                                                    stønadstype = Stønadstype.OVERGANGSSTØNAD)
        assertThat(oppdragIdForFagsystem.size).isEqualTo(2)
        assertThat(oppdragIdForFagsystem[0].behandlingsId).isEqualTo(behandling.eksternId.id)
        assertThat(oppdragIdForFagsystem[1].behandlingsId).isEqualTo(revurdering.eksternId.id)
    }


}
