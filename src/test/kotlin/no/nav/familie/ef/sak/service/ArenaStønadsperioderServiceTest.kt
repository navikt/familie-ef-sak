package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.ekstern.ArenaStønadsperioderService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infrastruktur.exception.PdlNotFoundException
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderArenaRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderResponse
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad.Datakilde
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDate.parse

internal class ArenaStønadsperioderServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>(relaxed = true)
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val fagsakService = mockk<FagsakService>()

    private val service =
            ArenaStønadsperioderService(infotrygdReplikaClient = infotrygdReplikaClient,
                                        behandlingRepository = behandlingRepository,
                                        pdlClient = pdlClient,
                                        tilkjentYtelseService = tilkjentYtelseService,
                                        familieIntegrasjonerClient = familieIntegrasjonerClient,
                                        fagsakService = fagsakService)

    private val ident = "01234567890"

    private val fagsakOvergangsstønad = fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD)
    private val fagsakBarnetilsyn = fagsak(stønadstype = Stønadstype.BARNETILSYN)
    private val behandlingOvergangsstønad = behandling(fagsakOvergangsstønad)
    private val behandlingBarnetilsyn = behandling(fagsakBarnetilsyn)

    @BeforeEach
    internal fun setUp() {
        every { behandlingRepository.finnSisteIverksatteBehandling(any()) } returns null
        every { fagsakService.finnFagsak(any(), Stønadstype.OVERGANGSSTØNAD) } returns fagsakOvergangsstønad
        every { fagsakService.finnFagsak(any(), Stønadstype.BARNETILSYN) } returns fagsakBarnetilsyn
        every { fagsakService.finnFagsak(any(), Stønadstype.SKOLEPENGER) } returns null
    }

    @Test
    internal fun `hentPerioder henter perioder fra infotrygd med alle identer fra pdl`() {
        val historiskIdent = "01234567890"
        val fomDato = LocalDate.MIN
        val tomDato = LocalDate.MAX
        val request = PerioderOvergangsstønadRequest(ident, fomDato, tomDato)

        mockPdl(historiskIdent)
        every { infotrygdReplikaClient.hentPerioderArena(any()) } returns
                infotrygdResponse(InfotrygdPeriode(ident, LocalDate.now(), LocalDate.now(), 10f))

        val hentPerioder = service.hentReplikaPerioder(request)

        assertThat(hentPerioder).hasSize(1)

        verify(exactly = 1) { pdlClient.hentPersonidenter(ident, true) }
        verify(exactly = 1) {
            val infotrygdRequest = InfotrygdPerioderArenaRequest(setOf(ident, historiskIdent), fomDato, tomDato)
            infotrygdReplikaClient.hentPerioderArena(infotrygdRequest)
        }
    }

    @Test
    internal fun `skal kalle infotrygd hvis pdl ikke finner personIdent med personIdent i requesten`() {
        every { pdlClient.hentPersonidenter(any(), true) } throws PdlNotFoundException()

        service.hentReplikaPerioder(PerioderOvergangsstønadRequest(ident))
        verify(exactly = 1) {
            infotrygdReplikaClient.hentPerioderArena(InfotrygdPerioderArenaRequest(setOf(ident)))
        }
    }

    @Test
    internal fun `skal returnere perioder fra både infotrygd og ef`() {
        mockPdl()
        every { behandlingRepository.finnSisteIverksatteBehandling(fagsakOvergangsstønad.id) } returns behandlingOvergangsstønad
        every { behandlingRepository.finnSisteIverksatteBehandling(fagsakBarnetilsyn.id) } returns behandlingBarnetilsyn

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

    private fun mockPdl(historiskIdent: String? = null) {
        val pdlIdenter = mutableListOf(PdlIdent(ident, false))
        if (historiskIdent != null) {
            pdlIdenter.add(PdlIdent(historiskIdent, true))
        }
        every { pdlClient.hentPersonidenter(ident, true) } returns PdlIdenter(pdlIdenter)
    }

    private fun infotrygdResponse(vararg infotrygdPeriodeOvergangsstønad: InfotrygdPeriode) =
            InfotrygdPerioderResponse(infotrygdPeriodeOvergangsstønad.toList())
}