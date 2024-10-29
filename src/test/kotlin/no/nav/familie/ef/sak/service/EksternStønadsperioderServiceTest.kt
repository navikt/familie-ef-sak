package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.ekstern.stønadsperiode.EksternStønadsperioderService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infotrygd.PeriodeService
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.DelårsperiodeSkoleårSkolepenger
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerUtgift
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerWrapper
import no.nav.familie.ef.sak.vedtak.domain.SkoleårsperiodeSkolepenger
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternePerioderForStønadstyperRequest
import no.nav.familie.kontrakter.felles.ef.EksternePerioderRequest
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDate.of
import java.time.YearMonth

internal class EksternStønadsperioderServiceTest {
    private val personService = mockk<PersonService>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val fagsakService = mockk<FagsakService>()
    private val vedtakService = mockk<VedtakService>()
    private val periodeService =
        PeriodeService(
            personService,
            fagsakService,
            behandlingService,
            tilkjentYtelseService,
            InfotrygdService(infotrygdReplikaClient, personService),
            vedtakService,
        )

    private val service = EksternStønadsperioderService(periodeService = periodeService)

    private val ident = "01234567890"

    private val fagsakOvergangsstønad = fagsak(stønadstype = StønadType.OVERGANGSSTØNAD)
    private val behandlingOvergangsstønad = behandling(fagsakOvergangsstønad)

    private val fagsakSkolepenger = fagsak(stønadstype = StønadType.SKOLEPENGER)
    private val behandlingSkolepenger = behandling(fagsakSkolepenger)

    @BeforeEach
    internal fun setUp() {
        every { infotrygdReplikaClient.hentSammenslåttePerioder(any()) } returns
            InfotrygdPeriodeResponse(
                emptyList(),
                emptyList(),
                emptyList(),
            )
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns null
        every { fagsakService.finnFagsak(any(), any()) } returns null
        every { fagsakService.finnFagsak(any(), StønadType.OVERGANGSSTØNAD) } returns fagsakOvergangsstønad
        every { fagsakService.finnFagsak(any(), StønadType.SKOLEPENGER) } returns fagsakSkolepenger
    }

    @Test
    internal fun `finner ikke perioder i infotrygd eller ny løsning`() {
        mockPdl()
        val perioder = service.hentPerioderForAlleStønader(EksternePerioderRequest(ident, now(), now()))
        assertThat(perioder.perioder.isEmpty())
    }

    @Test
    internal fun `finner kun perioder i infotrygd`() {
        val fom = YearMonth.of(2021, 1)
        val tom = YearMonth.of(2021, 3)
        mockPdl()
        mockInfotrygd(fom.atDay(1), tom.atEndOfMonth())
        val perioder = service.hentPerioderForAlleStønader(EksternePerioderRequest(ident, fom.atDay(1), tom.atEndOfMonth()))
        assertThat(perioder.perioder).hasSize(1)
        assertThat(perioder.perioder).containsExactly(lagResultatPeriode(fom, tom))
    }

    @Test
    internal fun `finner kun perioder i ny løsning`() {
        val fom = YearMonth.of(2021, 1)
        val tom = YearMonth.of(2021, 3)
        mockPdl()
        mockNyLøsning(fom.atDay(1), tom.atEndOfMonth())
        val perioder = service.hentPerioderForAlleStønader(EksternePerioderRequest(ident, fom.atDay(1), tom.atEndOfMonth()))
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
        val perioder = service.hentPerioderForAlleStønader(EksternePerioderRequest(ident, fom.atDay(1), tom.atEndOfMonth()))
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
        val perioder = service.hentPerioderForAlleStønader(EksternePerioderRequest(ident, fom.atDay(1), tom.atEndOfMonth()))
        assertThat(perioder.perioder).hasSize(1)
        assertThat(perioder.perioder).containsExactly(lagResultatPeriode(fom, tom))
    }

    @Test
    internal fun `finner perioder med beløp`() {
        val infoTrygd = YearMonth.of(2021, 1)
        val nyLøsning = YearMonth.of(2021, 3)
        mockPdl()
        mockInfotrygd(infoTrygd.atDay(1), infoTrygd.atEndOfMonth())
        mockNyLøsning(nyLøsning.atDay(1), nyLøsning.atEndOfMonth())
        val perioder = service.hentPerioderForOvergangsstønadMedBeløp(EksternePerioderRequest(ident))
        assertThat(perioder).hasSize(2)

        assertThat(perioder.last().beløp).isEqualTo(1)
        assertThat(perioder.last().fomDato).isEqualTo(infoTrygd.atDay(1))
        assertThat(perioder.last().tomDato).isEqualTo(infoTrygd.atEndOfMonth())

        assertThat(perioder.first().beløp).isEqualTo(1000)
        assertThat(perioder.first().fomDato).isEqualTo(nyLøsning.atDay(1))
        assertThat(perioder.first().tomDato).isEqualTo(nyLøsning.atEndOfMonth())
    }

    @Test
    internal fun `finn perioder gitt OS som stønadstype`() {
        val efSakPeriode = YearMonth.of(2021, 3)
        mockPdl()
        mockNyLøsning(efSakPeriode.atDay(1), efSakPeriode.atEndOfMonth())
        val perioder = service.hentPerioderForYtelser(EksternePerioderForStønadstyperRequest(ident, stønadstyper = listOf(StønadType.OVERGANGSSTØNAD))).perioder

        assertThat(perioder).hasSize(1)
        assertThat(perioder.first().fomDato).isEqualTo(efSakPeriode.atDay(1))
        assertThat(perioder.first().tomDato).isEqualTo(efSakPeriode.atEndOfMonth())
        assertThat(perioder.first().stønadstype).isEqualTo(StønadType.OVERGANGSSTØNAD)
    }

    @Test
    internal fun `finn perioder gitt stønadstype uten ytelser gitt skal returnere alle`() {
        val efSakPeriode = YearMonth.of(2021, 3)
        mockPdl()
        mockNyLøsning(efSakPeriode.atDay(1), efSakPeriode.atEndOfMonth())
        val perioder = service.hentPerioderForYtelser(EksternePerioderForStønadstyperRequest(ident, stønadstyper = emptyList())).perioder

        assertThat(perioder).hasSize(2)
        assertThat(perioder.first().fomDato).isEqualTo(efSakPeriode.atDay(1))
        assertThat(perioder.first().tomDato).isEqualTo(efSakPeriode.atEndOfMonth())
        assertThat(perioder.first().stønadstype).isEqualTo(StønadType.OVERGANGSSTØNAD)

        assertThat(perioder.last().fomDato).isEqualTo(of(2023, 8, 1))
        assertThat(perioder.last().tomDato).isEqualTo(of(2024, 6, 30))
        assertThat(perioder.last().stønadstype).isEqualTo(StønadType.SKOLEPENGER)
    }

    private fun mockInfotrygd(
        stønadFom: LocalDate,
        stønadTom: LocalDate,
    ) {
        val infotrygdPeriode = lagInfotrygdPeriode(stønadFom = stønadFom, stønadTom = stønadTom)
        val infotrygdPeriodeResponse = InfotrygdPeriodeResponse(listOf(infotrygdPeriode), emptyList(), emptyList())
        every { infotrygdReplikaClient.hentSammenslåttePerioder(any()) } returns infotrygdPeriodeResponse
    }

    private fun mockNyLøsning(
        stønadFom: LocalDate,
        stønadTom: LocalDate,
    ) {
        every { behandlingService.finnSisteIverksatteBehandling(fagsakOvergangsstønad.id) } returns behandlingOvergangsstønad
        every { tilkjentYtelseService.hentForBehandling(behandlingOvergangsstønad.id) } returns
            lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(1000, stønadFom, stønadTom, ident)))

        every { behandlingService.finnSisteIverksatteBehandling(fagsakSkolepenger.id) } returns behandlingSkolepenger
        every { tilkjentYtelseService.hentForBehandling(behandlingSkolepenger.id) } returns
            lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(1000, stønadFom, stønadTom, ident)))

        val skolepengerWrapper =
            SkolepengerWrapper(
                skoleårsperioder =
                    listOf(
                        SkoleårsperiodeSkolepenger(
                            perioder =
                                listOf(
                                    DelårsperiodeSkoleårSkolepenger(
                                        studietype = SkolepengerStudietype.HØGSKOLE_UNIVERSITET,
                                        periode = Månedsperiode(YearMonth.of(2023, 8), YearMonth.of(2024, 6)),
                                        100,
                                    ),
                                ),
                            utgiftsperioder = listOf<SkolepengerUtgift>(),
                        ),
                    ),
                begrunnelse = "begrunnelse",
            )

        every { vedtakService.hentVedtak(behandlingSkolepenger.id) } returns vedtak(behandlingId = behandlingSkolepenger.id, skolepenger = skolepengerWrapper)
    }

    private fun mockPdl() {
        every { personService.hentPersonIdenter(ident) } returns PdlIdenter(mutableListOf(PdlIdent(ident, false)))
    }

    private fun lagResultatPeriode(
        fom: YearMonth,
        tom: YearMonth,
    ) = EksternPeriode(
        personIdent = ident,
        fomDato = fom.atDay(1),
        tomDato = tom.atEndOfMonth(),
        datakilde = Datakilde.EF,
    )
}
