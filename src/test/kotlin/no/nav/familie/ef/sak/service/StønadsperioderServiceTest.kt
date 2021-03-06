package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.exception.PdlNotFoundException
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdent
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdenter
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeOvergangsstønad
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad.Datakilde
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDate.parse

internal class StønadsperioderServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>(relaxed = true)
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()

    private val service =
            StønadsperioderService(infotrygdReplikaClient = infotrygdReplikaClient,
                                   behandlingRepository = behandlingRepository,
                                   pdlClient = pdlClient,
                                   tilkjentYtelseService = tilkjentYtelseService,
                                   familieIntegrasjonerClient = familieIntegrasjonerClient)

    private val ident = "01234567890"

    @BeforeEach
    internal fun setUp() {
        every { behandlingRepository.finnSisteIverksatteBehandling(any(), any()) } returns null
    }

    @Test
    internal fun `hentPerioder henter perioder fra infotrygd med alle identer fra pdl`() {
        val historiskIdent = "01234567890"
        val fomDato = LocalDate.MIN
        val tomDato = LocalDate.MAX
        val request = PerioderOvergangsstønadRequest(ident, fomDato, tomDato)

        mockPdl(historiskIdent)
        every { infotrygdReplikaClient.hentPerioderOvergangsstønad(any()) } returns
                infotrygdResponse(InfotrygdPeriodeOvergangsstønad(ident, LocalDate.now(), LocalDate.now(), 10f))

        val hentPerioder = service.hentReplikaPerioder(request)

        assertThat(hentPerioder).hasSize(1)

        verify(exactly = 1) { pdlClient.hentPersonidenter(ident, true) }
        verify(exactly = 1) {
            val infotrygdRequest = InfotrygdPerioderOvergangsstønadRequest(setOf(ident, historiskIdent), fomDato, tomDato)
            infotrygdReplikaClient.hentPerioderOvergangsstønad(infotrygdRequest)
        }
    }

    @Test
    internal fun `skal kalle infotrygd hvis pdl ikke finner personIdent med personIdent i requesten`() {
        every { pdlClient.hentPersonidenter(any(), true) } throws PdlNotFoundException()

        service.hentReplikaPerioder(PerioderOvergangsstønadRequest(ident))
        verify(exactly = 1) {
            infotrygdReplikaClient.hentPerioderOvergangsstønad(InfotrygdPerioderOvergangsstønadRequest(setOf(ident)))
        }
    }

    @Test
    internal fun `skal returnere perioder fra både infotrygd og ef`() {
        val behandlingOvergangsstønad = behandling(fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD))
        val behandlingBarnetilsyn = behandling(fagsak(stønadstype = Stønadstype.BARNETILSYN))

        mockPdl()
        every {
            behandlingRepository.finnSisteIverksatteBehandling(Stønadstype.OVERGANGSSTØNAD,
                                                               any())
        } returns behandlingOvergangsstønad
        every { behandlingRepository.finnSisteIverksatteBehandling(Stønadstype.BARNETILSYN, any()) } returns behandlingBarnetilsyn

        every { familieIntegrasjonerClient.hentInfotrygdPerioder(any()) } returns PerioderOvergangsstønadResponse(listOf(
                PeriodeOvergangsstønad(ident, parse("2021-01-01"), parse("2021-01-31"), Datakilde.INFOTRYGD)))
        every { tilkjentYtelseService.hentForBehandling(behandlingOvergangsstønad.id) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(1, parse("2021-01-01"), parse("2021-01-31"), ident)))
        every { tilkjentYtelseService.hentForBehandling(behandlingBarnetilsyn.id) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(2, parse("2021-02-01"), parse("2021-03-31"), ident)))

        val perioder = service.hentPerioder(PerioderOvergangsstønadRequest(ident, parse("2020-01-01"), parse("2022-01-01")))
        assertThat(perioder.perioder).containsExactly(
                PeriodeOvergangsstønad(ident, parse("2021-01-01"), parse("2021-01-31"), Datakilde.INFOTRYGD),
                PeriodeOvergangsstønad(ident, parse("2021-01-01"), parse("2021-03-31"), Datakilde.EF),
        )

    }

    private fun periode(fomDato: LocalDate, tomDato: LocalDate, beløp: Float, opphørsdato: LocalDate? = null) =
            InfotrygdPeriodeOvergangsstønad(ident, fomDato, tomDato, beløp, opphørsdato)

    private fun mockPdl(historiskIdent: String? = null) {
        val pdlIdenter = mutableListOf(PdlIdent(ident, false))
        if (historiskIdent != null) {
            pdlIdenter.add(PdlIdent(historiskIdent, true))
        }
        every { pdlClient.hentPersonidenter(ident, true) } returns PdlIdenter(pdlIdenter)
    }

    private fun infotrygdResponse(vararg infotrygdPeriodeOvergangsstønad: InfotrygdPeriodeOvergangsstønad) =
            InfotrygdPerioderOvergangsstønadResponse(infotrygdPeriodeOvergangsstønad.toList())
}