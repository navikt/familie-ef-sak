package no.nav.familie.ef.sak.ekstern

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PerioderForBarnetrygdServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()

    private val service = PerioderForBarnetrygdService(pdlClient, behandlingService, tilkjentYtelseService)

    private val personIdent = "123"
    private val behandling = behandling(fagsak())

    @BeforeEach
    internal fun setUp() {
        every {
            pdlClient.hentPersonidenter(personIdent, true)
        } returns PdlIdenter(listOf(PdlIdent(personIdent, false)))
    }

    @Test
    internal fun `skal returnere tom liste hvis det ikke finnes en behandling for personen`() {
        mockBehandling(null)
        assertThat(service.hentPerioder(PersonIdent(personIdent)).perioder).isEmpty()
    }

    @Test
    internal fun `skal kun returnere perioder med full overgangsstønad`() {
        mockBehandling()
        mockTilkjentYtelse()
        val perioder = service.hentPerioder(PersonIdent(personIdent)).perioder

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].datakilde).isEqualTo(PeriodeOvergangsstønad.Datakilde.EF)
        assertThat(perioder[0].fomDato).isEqualTo(LocalDate.now())
        assertThat(perioder[0].tomDato).isEqualTo(LocalDate.now().plusDays(1))
    }

    private fun mockBehandling(behandling: Behandling? = this.behandling) {
        every {
            behandlingService.finnSisteIverksatteBehandling(Stønadstype.OVERGANGSSTØNAD, setOf(personIdent))
        } returns behandling
    }

    private fun mockTilkjentYtelse() {
        val fraOgMed = LocalDate.now().minusYears(1)
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
                tilkjentYtelse(behandling.id, personIdent).copy(andelerTilkjentYtelse = listOf(
                        lagAndelTilkjentYtelse(1, fraOgMed, fraOgMed, inntektsreduksjon = 1),
                        lagAndelTilkjentYtelse(2, fraOgMed, fraOgMed, samordningsfradrag = 1),
                        lagAndelTilkjentYtelse(1000, LocalDate.now(), LocalDate.now().plusDays(1))
                ))
    }
}