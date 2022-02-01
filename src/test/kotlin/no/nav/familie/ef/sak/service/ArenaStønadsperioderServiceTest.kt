package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.ekstern.arena.ArenaStønadsperioderService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infotrygd.PeriodeService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad.Datakilde
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDate.of
import java.time.YearMonth

@Disabled // TODO fiks denne når man tar i bruk ny tolkning av arena-perioder
internal class ArenaStønadsperioderServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val personopplysningerIntergasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val fagsakService = mockk<FagsakService>()
    private val periodeService = PeriodeService(pdlClient,
                                                fagsakService,
                                                behandlingService,
                                                tilkjentYtelseService,
                                                InfotrygdService(infotrygdReplikaClient, pdlClient))

    private val service = ArenaStønadsperioderService(
            periodeService = periodeService,
            personopplysningerIntegrasjonerClient = personopplysningerIntergasjonerClient)

    private val ident = "01234567890"

    private val fagsakOvergangsstønad = fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD)
    private val behandlingOvergangsstønad = behandling(fagsakOvergangsstønad)

    @BeforeEach
    internal fun setUp() {
        every { personopplysningerIntergasjonerClient.hentInfotrygdPerioder(any()) } returns
                PerioderOvergangsstønadResponse(emptyList())
        every { infotrygdReplikaClient.hentPerioder(any()) } returns InfotrygdPeriodeResponse(emptyList(),
                                                                                              emptyList(),
                                                                                              emptyList())
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns null
        every { fagsakService.finnFagsak(any(), any()) } returns null
        every { fagsakService.finnFagsak(any(), Stønadstype.OVERGANGSSTØNAD) } returns fagsakOvergangsstønad
    }

    @Test
    internal fun `finner ikke perioder i infotrygd eller ny løsning`() {
        mockPdl()
        val perioder = service.hentPerioder(PerioderOvergangsstønadRequest(ident, now(), now()))
        assertThat(perioder.perioder.isEmpty())
    }

    @Test
    internal fun `finner kun perioder i infotrygd`() {
        val fom = YearMonth.of(2021, 1)
        val tom = YearMonth.of(2021, 3)
        mockPdl()
        mockInfotrygd(fom.atDay(1), tom.atEndOfMonth())
        val perioder = service.hentPerioder(PerioderOvergangsstønadRequest(ident, fom.atDay(1), tom.atEndOfMonth()))
        assertThat(perioder.perioder).hasSize(1)
        assertThat(perioder.perioder).containsExactly(lagResultatPeriode(fom, tom))
    }

    @Test
    internal fun `finner kun perioder i ny løsning`() {
        val fom = YearMonth.of(2021, 1)
        val tom = YearMonth.of(2021, 3)
        mockPdl()
        mockNyLøsning(fom.atDay(1), tom.atEndOfMonth())
        val perioder = service.hentPerioder(PerioderOvergangsstønadRequest(ident, fom.atDay(1), tom.atEndOfMonth()))
        assertThat(perioder.perioder).hasSize(1)
        assertThat(perioder.perioder).containsExactly(lagResultatPeriode(fom, tom))
    }

    @Test
    internal fun `finner sammenhengende perioder`() {
        val fom = YearMonth.of(2021, 1)
        val tom = YearMonth.of(2021, 3)
        mockPdl()
        mockInfotrygd(of(2021, 1, 1), of(2021, 1, 31))
        mockNyLøsning(of(2021, 2, 1), of(2021, 3, 31))
        val perioder = service.hentPerioder(PerioderOvergangsstønadRequest(ident, fom.atDay(1), tom.atEndOfMonth()))
        assertThat(perioder.perioder).hasSize(1)
        assertThat(perioder.perioder).containsExactly(lagResultatPeriode(fom, tom))
    }

    @Test
    internal fun `finner overlappende perioder`() {
        val fom = YearMonth.of(2021, 1)
        val tom = YearMonth.of(2021, 3)
        mockPdl()
        mockInfotrygd(fom.atDay(1), tom.atEndOfMonth())
        mockNyLøsning(fom.atDay(1), tom.atEndOfMonth())
        val perioder = service.hentPerioder(PerioderOvergangsstønadRequest(ident, fom.atDay(1), tom.atEndOfMonth()))
        assertThat(perioder.perioder).hasSize(1)
        assertThat(perioder.perioder).containsExactly(lagResultatPeriode(fom, tom))
    }

    private fun mockInfotrygd(stønadFom: LocalDate, stønadTom: LocalDate) {
        val infotrygdPeriode = lagInfotrygdPeriode(stønadFom = stønadFom, stønadTom = stønadTom)
        val infotrygdPeriodeResponse = InfotrygdPeriodeResponse(listOf(infotrygdPeriode), emptyList(), emptyList())
        every { infotrygdReplikaClient.hentPerioder(any()) } returns infotrygdPeriodeResponse
    }

    private fun mockNyLøsning(stønadFom: LocalDate, stønadTom: LocalDate) {
        every { behandlingService.finnSisteIverksatteBehandling(fagsakOvergangsstønad.id) } returns behandlingOvergangsstønad
        every { tilkjentYtelseService.hentForBehandling(behandlingOvergangsstønad.id) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(0, stønadFom, stønadTom, ident)))
    }

    private fun mockPdl() {
        every { pdlClient.hentPersonidenter(ident, true) } returns PdlIdenter(mutableListOf(PdlIdent(ident, false)))
    }

    private fun lagResultatPeriode(fom: YearMonth, tom: YearMonth) =
            PeriodeOvergangsstønad(
                    personIdent = ident,
                    fomDato = fom.atDay(1),
                    tomDato = tom.atEndOfMonth(),
                    datakilde = Datakilde.EF
            )

}