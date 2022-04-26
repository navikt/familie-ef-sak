package no.nav.familie.ef.sak.infotrygd

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PeriodeServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val replikaClient = mockk<InfotrygdReplikaClient>()

    private val service = PeriodeService(
            pdlClient = pdlClient,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            tilkjentYtelseService = tilkjentYtelseService,
            infotrygdService = InfotrygdService(replikaClient, pdlClient)
    )

    private val personIdent = "123"
    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        mockFagsak(fagsak)
        every {
            pdlClient.hentPersonidenter(personIdent, true)
        } returns PdlIdenter(listOf(PdlIdent(personIdent, false)))
        every { replikaClient.hentPerioder(any()) } returns InfotrygdPeriodeResponse(emptyList(), emptyList(), emptyList())
    }

    @Test
    internal fun `skal returnere tom liste hvis det ikke finnes en fagsak for personen`() {
        mockFagsak(null)
        assertThat(service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)).isEmpty()
    }

    @Test
    internal fun `skal returnere tom liste hvis det ikke finnes en behandling for personen`() {
        mockBehandling(null)
        assertThat(service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)).isEmpty()
    }

    @Test
    internal fun `perioder overlapper ikke - skal returnere perioder fra infotrygd og ef`() {
        mockBehandling()
        val infotrygdFom = LocalDate.of(2021, 1, 1)
        val infotrygdTom = LocalDate.of(2021, 1, 31)
        val efFom = LocalDate.of(2021, 2, 1)
        val efTom = LocalDate.of(2021, 3, 31)
        mockTilkjentYtelse(lagAndelTilkjentYtelse(1, efFom, efTom))
        mockReplika(listOf(lagInfotrygdPeriode(stønadFom = infotrygdFom, stønadTom = infotrygdTom)))
        val perioder = service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)

        assertThat(perioder).hasSize(2)
        assertThat(perioder[0].datakilde).isEqualTo(PeriodeOvergangsstønad.Datakilde.EF)
        assertThat(perioder[0].stønadFom).isEqualTo(efFom)
        assertThat(perioder[0].stønadTom).isEqualTo(efTom)

        assertThat(perioder[1].datakilde).isEqualTo(PeriodeOvergangsstønad.Datakilde.INFOTRYGD)
        assertThat(perioder[1].stønadFom).isEqualTo(infotrygdFom)
        assertThat(perioder[1].stønadTom).isEqualTo(infotrygdTom)
    }

    @Test
    internal fun `perioden fra EF avkorter periode fra infotrygd`() {
        mockBehandling()
        val infotrygdFom = LocalDate.of(2021, 1, 1)
        val infotrygdTom = LocalDate.of(2021, 3, 31)
        val efFom = LocalDate.of(2021, 2, 1)
        val efTom = LocalDate.of(2021, 3, 31)
        mockTilkjentYtelse(lagAndelTilkjentYtelse(1, efFom, efTom))
        mockReplika(listOf(lagInfotrygdPeriode(stønadFom = infotrygdFom, stønadTom = infotrygdTom)))
        val perioder = service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)

        assertThat(perioder).hasSize(2)
        assertThat(perioder[0].datakilde).isEqualTo(PeriodeOvergangsstønad.Datakilde.EF)
        assertThat(perioder[0].stønadFom).isEqualTo(efFom)
        assertThat(perioder[0].stønadTom).isEqualTo(efTom)

        assertThat(perioder[1].datakilde).isEqualTo(PeriodeOvergangsstønad.Datakilde.INFOTRYGD)
        assertThat(perioder[1].stønadFom).isEqualTo(infotrygdFom)
        assertThat(perioder[1].stønadTom).isEqualTo(efFom.minusDays(1))
    }

    @Test
    internal fun `tilkjent ytelse uten andeler fra EF avkorter periode fra infotrygd`() {
        mockBehandling()
        val infotrygdFom = LocalDate.of(2021, 1, 1)
        val infotrygdTom = LocalDate.of(2021, 3, 31)
        val efFom = LocalDate.of(2021, 2, 1)
        mockTilkjentYtelse(efFom)
        mockReplika(listOf(lagInfotrygdPeriode(stønadFom = infotrygdFom, stønadTom = infotrygdTom)))
        val perioder = service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].datakilde).isEqualTo(PeriodeOvergangsstønad.Datakilde.INFOTRYGD)
        assertThat(perioder[0].stønadFom).isEqualTo(infotrygdFom)
        assertThat(perioder[0].stønadTom).isEqualTo(efFom.minusDays(1))
    }

    @Test
    internal fun `hvis en periode fra ef overlapper perioder fra infotrygd så er det perioden fra EF som har høyere presidens`() {
        mockBehandling()
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusDays(9)
        mockTilkjentYtelse(lagAndelTilkjentYtelse(1, fom, tom))
        mockReplika(listOf(lagInfotrygdPeriode(beløp = 2, stønadFom = fom, stønadTom = tom)))
        val perioder = service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].datakilde).isEqualTo(PeriodeOvergangsstønad.Datakilde.EF)
        assertThat(perioder[0].stønadFom).isEqualTo(fom)
        assertThat(perioder[0].stønadTom).isEqualTo(tom)
        assertThat(perioder[0].månedsbeløp).isEqualTo(1)
    }

    @Test
    internal fun `skal filtrere vekk perioder som er annulert eller uaktuelle`() {
        mockBehandling()
        mockTilkjentYtelse(LocalDate.MAX)
        mockReplika(listOf(lagInfotrygdPeriode(stønadFom = LocalDate.parse("2021-01-01"),
                                               stønadTom = LocalDate.parse("2021-01-02"),
                                               beløp = 1,
                                               kode = InfotrygdEndringKode.ANNULERT),
                           lagInfotrygdPeriode(stønadFom = LocalDate.parse("2021-02-01"),
                                               stønadTom = LocalDate.parse("2021-02-02"),
                                               beløp = 2),
                           lagInfotrygdPeriode(stønadFom = LocalDate.parse("2021-03-01"),
                                               stønadTom = LocalDate.parse("2021-03-02"),
                                               beløp = 3,
                                               kode = InfotrygdEndringKode.UAKTUELL)))
        val perioder = service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].månedsbeløp).isEqualTo(2)
    }

    @Test
    internal fun `skal endre tom-datoer på overlappende perioder tvers fagsystem`() {
        val periode1fom = LocalDate.of(2021, 1, 1)
        val periode1tom = LocalDate.of(2021, 1, 31)
        val periode2fom = LocalDate.of(2021, 2, 1)
        val periode2tom = LocalDate.of(2021, 3, 31)
        val efFra = LocalDate.of(2021, 3, 1)
        val efTil = LocalDate.of(2021, 3, 31)

        mockBehandling()
        mockTilkjentYtelse(lagAndelTilkjentYtelse(100, fraOgMed = efFra, tilOgMed = efTil))
        mockReplika(listOf(lagInfotrygdPeriode(stønadFom = periode1fom, stønadTom = periode1tom, beløp = 1),
                           lagInfotrygdPeriode(stønadFom = periode2fom, stønadTom = periode2tom, beløp = 2)))
        val perioder = service.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)

        assertThat(perioder).hasSize(3)
        assertThat(perioder[0].stønadFom).isEqualTo(efFra)
        assertThat(perioder[0].stønadTom).isEqualTo(efTil)

        assertThat(perioder[1].stønadFom).isEqualTo(periode2fom)
        assertThat(perioder[1].stønadTom).isNotEqualTo(periode2tom)
        assertThat(perioder[1].stønadTom).isEqualTo(efFra.minusDays(1))

        assertThat(perioder[2].stønadFom).isEqualTo(periode1fom)
        assertThat(perioder[2].stønadTom).isEqualTo(periode1tom)
    }

    private fun mockReplika(overgangsstønad: List<InfotrygdPeriode>) {
        every { replikaClient.hentPerioder(any()) } returns InfotrygdPeriodeResponse(overgangsstønad, emptyList(), emptyList())
    }

    private fun mockFagsak(fagsak: Fagsak? = this.fagsak) {
        every { fagsakService.finnFagsak(setOf(personIdent), StønadType.OVERGANGSSTØNAD) } returns fagsak
    }

    private fun mockBehandling(behandling: Behandling? = this.behandling) {
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
    }

    private fun mockTilkjentYtelse(vararg andelTilkjentYtelse: AndelTilkjentYtelse) {
        if (andelTilkjentYtelse.isEmpty()) error("Må sette startdato hvis man har en tom liste med andeler")
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(andelTilkjentYtelse.toList(), behandlingId = behandling.id)
    }

    private fun mockTilkjentYtelse(startdato: LocalDate) {
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(andelerTilkjentYtelse = emptyList(), behandlingId = behandling.id, startdato = startdato)
    }
}
