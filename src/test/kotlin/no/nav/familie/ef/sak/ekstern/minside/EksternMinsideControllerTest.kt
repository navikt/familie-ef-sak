package no.nav.familie.ef.sak.ekstern.minside

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.innvilgetOgFerdigstilt
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.økonomi.DataGenerator
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDate

class EksternMinsideControllerTest : OppslagSpringRunnerTest() {
    private val personident = "12345678901"
    private val fagsakOvergangsstønad =
        fagsak(stønadstype = StønadType.OVERGANGSSTØNAD, identer = fagsakpersoner(setOf(personident)))
    private val fagsakBarnetilsyn =
        fagsak(stønadstype = StønadType.BARNETILSYN, identer = fagsakpersoner(setOf(personident)))
    private lateinit var behandlingOvergangsstønad: Behandling
    private lateinit var behandlingBarnetilsyn: Behandling

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(søkerToken(personident))
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)
        testoppsettService.lagreFagsak(fagsakBarnetilsyn)
    }

    @Test
    fun `skal kunne hente ut stønader som sluttbruker som bare har fagsak uten behandlinger`() {
        val response = hentMineStønadsperioder()
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val ressursData = response.body?.data
        assertThat(ressursData).isNotNull
        assertThat(ressursData?.overgangsstønad).isEmpty()
        assertThat(ressursData?.skolepenger).isEmpty()
        assertThat(ressursData?.barnetilsyn).isEmpty()
    }

    @Test
    fun `skal kunne hente ut stønader som sluttbruker`() {
        behandlingOvergangsstønad = behandlingRepository.insert(behandling(fagsakOvergangsstønad).innvilgetOgFerdigstilt())
        behandlingBarnetilsyn = behandlingRepository.insert(behandling(fagsakBarnetilsyn).innvilgetOgFerdigstilt())
        opprettTilkjentYtelseMedAndeler(behandlingBarnetilsyn)
        opprettTilkjentYtelseMedAndeler(behandlingOvergangsstønad)

        val response = hentMineStønadsperioder()
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val ressursData = response.body?.data
        assertThat(ressursData).isNotNull

        assertThat(ressursData?.overgangsstønad).hasSize(2)
        assertThat(ressursData?.overgangsstønad?.first()?.beløp).isEqualTo(andel1.beløp)
        assertThat(ressursData?.overgangsstønad?.first()?.fraDato).isEqualTo(andel1.stønadFom)
        assertThat(ressursData?.overgangsstønad?.first()?.tilDato).isEqualTo(andel1.stønadTom)
        assertThat(ressursData?.overgangsstønad?.last()?.beløp).isEqualTo(andel2.beløp)
        assertThat(ressursData?.overgangsstønad?.last()?.fraDato).isEqualTo(andel2.stønadFom)
        assertThat(ressursData?.overgangsstønad?.last()?.tilDato).isEqualTo(andel2.stønadTom)
        assertThat(ressursData?.skolepenger).hasSize(0)
        assertThat(ressursData?.barnetilsyn).hasSize(2)
    }

    @Test
    fun `skal ikke kunne hente ut stønader som sluttbruker dersom token er ugyldig`() {
        headers.setBearerAuth("01010112345-UGYLDIG_tokEN")
        val response = hentMineStønadsperioder()
        assertThat(response.statusCode).isNotEqualTo(HttpStatus.OK)
    }

    @Test
    fun `skal ikke kunne hente ut stønader som sluttbruker dersom token har level 3 som er for lavt`() {
        val søkerToken = søkerToken(personident, "Level3")
        headers.setBearerAuth(søkerToken)
        val response = hentMineStønadsperioder()
        assertThat(response.statusCode).isNotEqualTo(HttpStatus.OK)
    }

    private fun hentMineStønadsperioder(): ResponseEntity<Ressurs<MineStønaderDto>> =
        restTemplate.exchange(
            localhost("/api/ekstern/minside/stonadsperioder"),
            HttpMethod.GET,
            HttpEntity<Ressurs<MineStønaderDto>>(headers),
        )

    private val andel1 =
        lagAndelTilkjentYtelse(
            beløp = 4321,
            fraOgMed = LocalDate.of(2023, 1, 1),
            tilOgMed = LocalDate.of(2023, 7, 31),
        )
    private val andel2 =
        lagAndelTilkjentYtelse(
            beløp = 1234,
            fraOgMed = LocalDate.of(2023, 8, 1),
            tilOgMed = LocalDate.of(2023, 12, 31),
        )

    private fun opprettTilkjentYtelseMedAndeler(behandling: Behandling) {
        val andelerTilkjentYtelse = listOf(andel1.copy(kildeBehandlingId = behandling.id), andel2.copy(kildeBehandlingId = behandling.id))
        tilkjentYtelseRepository.insert(
            DataGenerator.tilfeldigTilkjentYtelse(behandling).copy(andelerTilkjentYtelse = andelerTilkjentYtelse),
        )
    }
}
