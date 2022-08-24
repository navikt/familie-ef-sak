package no.nav.familie.ef.sak.ekstern

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class EksternBehandlingControllerTest {

    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val eksternBehandlingService = EksternBehandlingService(tilkjentYtelseService, behandlingService, fagsakService)
    private val eksternBehandlingController = EksternBehandlingController(eksternBehandlingService)

    @Test
    internal fun `send tom liste med personidenter, forvent HttpStatus 400`() {
        val finnesBehandlingForPerson =
            eksternBehandlingController.harAktivStønad(emptySet())
        assertThat(finnesBehandlingForPerson.status).isEqualTo(Ressurs.Status.FEILET)
    }

    @Test
    internal fun `opprett en ikke-utdatert og en utdatert andelsliste, forvent at en stønad for det siste året finnes`() {
        mockOpprettTilkjenteYtelser(opprettIkkeUtdatertTilkjentYtelse(), opprettUtdatertTilkjentYtelse())
        assertThat(eksternBehandlingController.harAktivStønad(setOf("12345678910")).data).isEqualTo(true)
    }

    @Test
    internal fun `opprett bare utdaterte andeler, forvent at stønad for det siste året ikke finnes`() {
        mockOpprettTilkjenteYtelser(opprettUtdatertTilkjentYtelse(), opprettUtdatertTilkjentYtelse())
        assertThat(eksternBehandlingController.harAktivStønad(setOf("12345678910")).data).isEqualTo(false)
    }

    @Test
    internal fun `tomme lister med andeler, forvent at stønad for det siste året ikke finnes`() {
        mockOpprettTilkjenteYtelser(
            lagTilkjentYtelse(andelerTilkjentYtelse = emptyList()),
            lagTilkjentYtelse(andelerTilkjentYtelse = emptyList())
        )
        assertThat(eksternBehandlingController.harAktivStønad(setOf("12345678910")).data).isEqualTo(false)
    }

    private fun opprettIkkeUtdatertTilkjentYtelse(): TilkjentYtelse {
        return lagTilkjentYtelse(
            andelerTilkjentYtelse = listOf(
                lagAndelTilkjentYtelse(
                    beløp = 1,
                    fraOgMed = YearMonth.of(2019, 1),
                    tilOgMed = YearMonth.of(2019, 2)
                ),
                lagAndelTilkjentYtelse(
                    beløp = 1,
                    fraOgMed = YearMonth.of(2020, 1),
                    tilOgMed = YearMonth.now().plusMonths(11)
                )
            )
        )
    }

    private fun opprettUtdatertTilkjentYtelse(): TilkjentYtelse {
        return lagTilkjentYtelse(
            andelerTilkjentYtelse = listOf(
                lagAndelTilkjentYtelse(
                    beløp = 1,
                    fraOgMed = YearMonth.of(2019, 1),
                    tilOgMed = YearMonth.of(2019, 2)
                ),
                lagAndelTilkjentYtelse(
                    beløp = 1,
                    fraOgMed = YearMonth.now().minusMonths(14),
                    tilOgMed = YearMonth.now().minusYears(1).minusMonths(1)
                )
            )
        )
    }

    private fun mockOpprettTilkjenteYtelser(tilkjentYtelse: TilkjentYtelse, annenTilkjentYtelse: TilkjentYtelse) {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val behandling1 = behandling(id = uuid1)
        val behandling2 = behandling(id = uuid2)

        every { fagsakService.finnFagsak(any(), any()) } returns fagsak()
        every {
            behandlingService.finnSisteIverksatteBehandling(any())
        } returns behandling1 andThen behandling2 andThen null

        every { tilkjentYtelseService.hentForBehandling(uuid1) } returns tilkjentYtelse
        every { tilkjentYtelseService.hentForBehandling(uuid2) } returns annenTilkjentYtelse
    }
}
