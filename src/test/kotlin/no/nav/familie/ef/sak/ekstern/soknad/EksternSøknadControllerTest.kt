package no.nav.familie.ef.sak.ekstern.soknad

import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infrastruktur.config.InfotrygdReplikaMock
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.innvilgetOgFerdigstilt
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.økonomi.DataGenerator
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.Vedtakstreff
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
import java.time.LocalDate

class EksternSøknadControllerTest : OppslagSpringRunnerTest() {
    private val personident = "12345678901"
    private val fagsakOvergangsstønad =
        fagsak(stønadstype = StønadType.OVERGANGSSTØNAD, identer = fagsakpersoner(setOf(personident)))

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var infotrygdReplikaClient: InfotrygdReplikaClient

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(søkerToken(personident))
        InfotrygdReplikaMock.resetMock(infotrygdReplikaClient)
        every { infotrygdReplikaClient.hentInslagHosInfotrygd(any()) } returns
            InfotrygdFinnesResponse(vedtak = emptyList(), saker = emptyList())
    }

    @Test
    fun `skal returnere JA for innvilget vedtak med beløp 0`() {
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)
        val behandling = behandlingRepository.insert(behandling(fagsakOvergangsstønad).innvilgetOgFerdigstilt())
        val andelMedNullBeløp =
            lagAndelTilkjentYtelse(
                beløp = 0,
                fraOgMed = LocalDate.of(2023, 1, 1),
                tilOgMed = LocalDate.of(2023, 12, 31),
                kildeBehandlingId = behandling.id,
            )
        tilkjentYtelseRepository.insert(
            DataGenerator.tilfeldigTilkjentYtelse(behandling).copy(andelerTilkjentYtelse = listOf(andelMedNullBeløp)),
        )

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.JA)
    }

    @Test
    fun `skal returnere JA for opphørt sak som tidligere hadde andeler`() {
        testoppsettService.lagreFagsak(fagsakOvergangsstønad)

        val førsteBehandling = behandlingRepository.insert(behandling(fagsakOvergangsstønad).innvilgetOgFerdigstilt())
        val andelMedBeløp =
            lagAndelTilkjentYtelse(
                beløp = 5000,
                fraOgMed = LocalDate.of(2023, 1, 1),
                tilOgMed = LocalDate.of(2023, 6, 30),
                kildeBehandlingId = førsteBehandling.id,
            )
        tilkjentYtelseRepository.insert(
            DataGenerator.tilfeldigTilkjentYtelse(førsteBehandling).copy(andelerTilkjentYtelse = listOf(andelMedBeløp)),
        )

        val opphørtBehandling =
            behandlingRepository.insert(
                behandling(fagsakOvergangsstønad).copy(
                    resultat = BehandlingResultat.OPPHØRT,
                    status = BehandlingStatus.FERDIGSTILT,
                    vedtakstidspunkt = SporbarUtils.now(),
                ),
            )
        tilkjentYtelseRepository.insert(
            DataGenerator.tilfeldigTilkjentYtelse(opphørtBehandling).copy(
                andelerTilkjentYtelse = emptyList(),
                type = TilkjentYtelseType.OPPHØR,
                startdato = LocalDate.of(2023, 1, 1),
            ),
        )

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.JA)
    }

    @Test
    fun `skal returnere JA når person kun finnes i Infotrygd`() {
        every { infotrygdReplikaClient.hentInslagHosInfotrygd(any()) } returns
            InfotrygdFinnesResponse(
                vedtak = listOf(Vedtakstreff(personident, StønadType.OVERGANGSSTØNAD, false)),
                saker = emptyList(),
            )

        val response = hentHarTidligereInnvilgetVedtak()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isEqualTo(TidligereVedtakStatus.JA)
    }

    private fun hentHarTidligereInnvilgetVedtak() =
        testRestTemplate.exchange<Ressurs<TidligereVedtakStatus>>(
            localhost("/api/ekstern/soknad/har-tidligere-innvilget-vedtak"),
            HttpMethod.GET,
            HttpEntity<Ressurs<TidligereVedtakStatus>>(headers),
        )
}
