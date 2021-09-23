package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.simulering.SimuleringsresultatDto
import no.nav.familie.ef.sak.simulering.SimuleringsresultatRepository
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.util.UUID

internal class SimuleringControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository
    @Autowired private lateinit var simuleringsresultatRepository: SimuleringsresultatRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal returnere 200 OK for simulering av behandling`() {
        val personIdent = "12345678901"
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(personIdent))))
        val behandling = behandlingRepository.insert(behandling(fagsak, aktiv = true))
        tilkjentYtelseRepository.insert(TilkjentYtelse(behandlingId = behandling.id,
                                                       personident = personIdent,
                                                       vedtakstidspunkt = LocalDate.of(2020, 5, 5).atStartOfDay(),
                                                       type = TilkjentYtelseType.FØRSTEGANGSBEHANDLING,
                                                       andelerTilkjentYtelse = listOf(AndelTilkjentYtelse(15000,
                                                                                                          LocalDate.of(2021,
                                                                                                                       1,
                                                                                                                       1),
                                                                                                          LocalDate.of(2021,
                                                                                                                       12,
                                                                                                                       31),
                                                                                                          personIdent,
                                                                                                          inntekt = 0,
                                                                                                          inntektsreduksjon = 0,
                                                                                                          samordningsfradrag = 0,
                                                                                                          kildeBehandlingId = behandling.id))
                                                       ))

        val respons: ResponseEntity<Ressurs<SimuleringsresultatDto>> = simulerForBehandling(behandling.id)

        Assertions.assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(respons.body.status).isEqualTo(Ressurs.Status.SUKSESS)
        Assertions.assertThat(respons.body.data).isNotNull()
        Assertions.assertThat(respons.body.data?.perioder).hasSize(8)
        val simuleringsresultat = simuleringsresultatRepository.findByIdOrThrow(behandling.id)

        // Verifiser at simuleringsresultatet er lagret
        Assertions.assertThat(simuleringsresultat.data.simuleringMottaker).hasSize(1)
        Assertions.assertThat(simuleringsresultat.data.simuleringMottaker.first().simulertPostering).hasSizeGreaterThan(1)
    }

    private fun simulerForBehandling(behandlingId: UUID): ResponseEntity<Ressurs<SimuleringsresultatDto>> {
        return restTemplate.exchange(localhost("/api/simulering/$behandlingId"),
                                     HttpMethod.GET,
                                     HttpEntity<Ressurs<BehandlingDto>>(headers))
    }

}