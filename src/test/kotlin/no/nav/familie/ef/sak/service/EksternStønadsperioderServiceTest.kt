package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.ekstern.stønadsperiode.EksternStønadsperioderService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infotrygd.ArbeidsoppfølgingsPeriodeMedAktivitetOgBarn
import no.nav.familie.ef.sak.infotrygd.BehandlingsbarnMedOppfyltAleneomsorg
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
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.DelårsperiodeSkoleårSkolepenger
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerUtgift
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerWrapper
import no.nav.familie.ef.sak.vedtak.domain.SkoleårsperiodeSkolepenger
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
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
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val barnService = mockk<BarnService>()
    private val periodeServiceMock = mockk<PeriodeService>()
    private val periodeService =
        PeriodeService(
            personService,
            fagsakService,
            behandlingService,
            tilkjentYtelseService,
            InfotrygdService(infotrygdReplikaClient, personService),
            vedtakService,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            barnService = barnService,
        )

    private val service = EksternStønadsperioderService(periodeService = periodeService, personService = personService)
    private val serviceMedPeriodeserviceMock = EksternStønadsperioderService(periodeService = periodeServiceMock, personService = personService)


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

    @Test
    internal fun `skal slå sammen like perioder arbeidsoppfølging`() {

        mockPdl()
        val barn1 = BehandlingsbarnMedOppfyltAleneomsorg(null, now())
        val barn2 = BehandlingsbarnMedOppfyltAleneomsorg("9876543210", now().minusYears(2))
        val periode1 = lagArbeidsoppfølgingsperiode(barn1, barn2)
        val periode2= periode1.copy(stønadFraOgMed = now().plusMonths(1), stønadTilOgMed = now().plusMonths(2))
        every { periodeServiceMock.hentLøpendeOvergangsstønadPerioderMedAktivitetOgBehandlingsbarn(setOf(ident)) } returns listOf(periode1, periode2)

        val hentPerioderForOSMedAktivitet = serviceMedPeriodeserviceMock.hentPerioderForOSMedAktivitet(ident)

        assertThat(hentPerioderForOSMedAktivitet.internperioder).hasSize(1)
    }

    @Test
    internal fun `skal ikke slå sammen periodermed ulik aktivitet`() {

        mockPdl()
        val barn1 = BehandlingsbarnMedOppfyltAleneomsorg(null, now())
        val barn2 = BehandlingsbarnMedOppfyltAleneomsorg("9876543210", now().minusYears(2))
        val periode1 = lagArbeidsoppfølgingsperiode(barn1, barn2)
        val periode2= periode1.copy(stønadFraOgMed = now().plusMonths(1), stønadTilOgMed = now().plusMonths(2), aktivitet = AktivitetType.UTVIDELSE_FORSØRGER_I_UTDANNING)
        every { periodeServiceMock.hentLøpendeOvergangsstønadPerioderMedAktivitetOgBehandlingsbarn(setOf(ident)) } returns listOf(periode1, periode2)

        val hentPerioderForOSMedAktivitet = serviceMedPeriodeserviceMock.hentPerioderForOSMedAktivitet(ident)

        assertThat(hentPerioderForOSMedAktivitet.internperioder).hasSize(2)
    }

    @Test
    internal fun `skal ikke slå sammen perioder med ulike barn`() {

        mockPdl()
        val barn1 = BehandlingsbarnMedOppfyltAleneomsorg(null, now())
        val barn2 = BehandlingsbarnMedOppfyltAleneomsorg("9876543210", now().minusYears(2))
        val periode1 = lagArbeidsoppfølgingsperiode(barn1, barn2)
        val periode2= periode1.copy(stønadFraOgMed = now().plusMonths(1), stønadTilOgMed = now().plusMonths(2), barn = listOf(barn2))
        every { periodeServiceMock.hentLøpendeOvergangsstønadPerioderMedAktivitetOgBehandlingsbarn(setOf(ident)) } returns listOf(periode1, periode2)

        val hentPerioderForOSMedAktivitet = serviceMedPeriodeserviceMock.hentPerioderForOSMedAktivitet(ident)

        assertThat(hentPerioderForOSMedAktivitet.internperioder).hasSize(2)
        val enesteBarn = hentPerioderForOSMedAktivitet.internperioder.find { it.stønadFraOgMed === periode2.stønadFraOgMed }?.barn?.single()
        assertThat(enesteBarn).isEqualTo(barn2)
    }

    @Test
    internal fun `skal ikke slå sammen perioder med når ett av barne har fått personIdent`() {
        mockPdl()
        val barn1 = BehandlingsbarnMedOppfyltAleneomsorg(null, now())
        val barn2 = BehandlingsbarnMedOppfyltAleneomsorg("9876543210", now().minusYears(2))
        val periode1 = lagArbeidsoppfølgingsperiode(barn1, barn2)
        val periode2= periode1.copy(stønadFraOgMed = now().plusMonths(1), stønadTilOgMed = now().plusMonths(2), barn = listOf(barn1.copy(personIdent = "nyIdentHer"), barn2))
        every { periodeServiceMock.hentLøpendeOvergangsstønadPerioderMedAktivitetOgBehandlingsbarn(setOf(ident)) } returns listOf(periode1, periode2)

        val hentPerioderForOSMedAktivitet = serviceMedPeriodeserviceMock.hentPerioderForOSMedAktivitet(ident)

        assertThat(hentPerioderForOSMedAktivitet.internperioder).hasSize(2)
    }


    @Test
    internal fun `skal ikke slå sammen perioder med når periode har forskjellig behandling`() {
        mockPdl()
        val barn1 = BehandlingsbarnMedOppfyltAleneomsorg(null, now())
        val barn2 = BehandlingsbarnMedOppfyltAleneomsorg("9876543210", now().minusYears(2))
        val periode1 = lagArbeidsoppfølgingsperiode(barn1, barn2)
        val periode2= periode1.copy(stønadFraOgMed = now().plusMonths(1), stønadTilOgMed = now().plusMonths(2), behandlingId = 1234L)
        every { periodeServiceMock.hentLøpendeOvergangsstønadPerioderMedAktivitetOgBehandlingsbarn(setOf(ident)) } returns listOf(periode1, periode2)

        val hentPerioderForOSMedAktivitet = serviceMedPeriodeserviceMock.hentPerioderForOSMedAktivitet(ident)

        assertThat(hentPerioderForOSMedAktivitet.internperioder).hasSize(2)
    }


    private fun lagArbeidsoppfølgingsperiode(
        barn1: BehandlingsbarnMedOppfyltAleneomsorg,
        barn2: BehandlingsbarnMedOppfyltAleneomsorg
    ): ArbeidsoppfølgingsPeriodeMedAktivitetOgBarn = ArbeidsoppfølgingsPeriodeMedAktivitetOgBarn(
        stønadFraOgMed = now(),
        stønadTilOgMed = now(),
        aktivitet = AktivitetType.FORSØRGER_I_ARBEID,
        periodeType = VedtaksperiodeType.HOVEDPERIODE,
        barn = listOf(barn1, barn2),
        behandlingId = behandlingOvergangsstønad.eksternId,
        harAktivitetsplikt = true,
    )

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
