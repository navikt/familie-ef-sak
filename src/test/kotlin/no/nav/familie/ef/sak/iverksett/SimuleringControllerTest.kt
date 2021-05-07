package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.BehandlingDto
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
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
                                                       utbetalingsoppdrag = null,
                                                       vedtaksdato = LocalDate.of(2020, 5, 5),
                                                       status = TilkjentYtelseStatus.OPPRETTET,
                                                       type = TilkjentYtelseType.FØRSTEGANGSBEHANDLING,
                                                       andelerTilkjentYtelse = listOf(AndelTilkjentYtelse(15000,
                                                                                                          LocalDate.of(2021,
                                                                                                                       1,
                                                                                                                       1),
                                                                                                          LocalDate.of(2021,
                                                                                                                       12,
                                                                                                                       31),
                                                                                                          personIdent,
                                                                                                          periodeId = 1L,
                                                                                                          kildeBehandlingId = behandling.id))
                                                       ))

        val respons: ResponseEntity<Ressurs<DetaljertSimuleringResultat>> = simulerForBehandling(behandling.id)

        Assertions.assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(respons.body.status).isEqualTo(Ressurs.Status.SUKSESS)
        Assertions.assertThat(respons.body.data).isNotNull()
        Assertions.assertThat(respons.body.data?.simuleringMottaker).hasSize(1)
    }

    private fun simulerForBehandling(behandlingId: UUID): ResponseEntity<Ressurs<DetaljertSimuleringResultat>> {
        return restTemplate.exchange(localhost("/api/simulering/$behandlingId"),
                                     HttpMethod.GET,
                                     HttpEntity<Ressurs<BehandlingDto>>(headers))
    }

}