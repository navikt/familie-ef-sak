package no.nav.familie.ef.sak.opplysninger.personopplysninger

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsaker
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.config.InfotrygdReplikaMock
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakPerson
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.folkeregisteridentifikator
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType.HOVEDPERIODE
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelMedGrunnlagDto
import no.nav.familie.ef.sak.vedtak.historikk.EndringType
import no.nav.familie.ef.sak.vedtak.historikk.HistorikkEndring
import no.nav.familie.ef.sak.vedtak.historikk.VedtakshistorikkperiodeOvergangsstønad
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class TidligereVedtaksperioderServiceTest {

    private val andelsHistorikkService = mockk<AndelsHistorikkService>(relaxed = true)
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val personService = mockk<PersonService>()
    private val historiskPensjonService = mockk<HistoriskPensjonService>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>()
    private val infotrygdService = InfotrygdService(infotrygdReplikaClient, personService)

    private val service = TidligereVedtaksperioderService(
        fagsakPersonService,
        fagsakService,
        behandlingService,
        tilkjentYtelseService,
        infotrygdService,
        historiskPensjonService,
        andelsHistorikkService,
    )

    private val infotrygdPeriodeRequestSlot = slot<InfotrygdPeriodeRequest>()

    private val personIdent = folkeregisteridentifikator("1")
    private val identAnnenForelder = folkeregisteridentifikator("2")

    private val fagsakPerson = fagsakPerson(fagsakpersoner(identAnnenForelder.ident))
    private val fagsak = fagsak(person = fagsakPerson)
    private val fagsaker = Fagsaker(fagsak, null, null)
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        every {
            infotrygdReplikaClient.hentPerioder(capture(infotrygdPeriodeRequestSlot))
        } answers { InfotrygdReplikaMock.hentPerioderDefaultResponse(firstArg()) }
        every { personService.hentPersonIdenter(personIdent.ident) } returns
            PdlIdenter(listOf(PdlIdent(personIdent.ident, false)))
        every { historiskPensjonService.hentHistoriskPensjon(any(), any()) } returns
            HistoriskPensjonResponse(false, "")
    }

    @Test
    internal fun `skal sjekke om annen forelder har historikk i ef-sak og infotrygd`() {
        mockTidligereVedtakEfSak(harAndeler = true)

        val tidligereVedtaksperioder = service.hentTidligereVedtaksperioder(listOf(personIdent))

        assertThat(tidligereVedtaksperioder.infotrygd.harTidligereOvergangsstønad).isTrue
        assertThat(tidligereVedtaksperioder.infotrygd.harTidligereBarnetilsyn).isTrue
        assertThat(tidligereVedtaksperioder.infotrygd.harTidligereSkolepenger).isFalse

        val sak = tidligereVedtaksperioder.sak ?: error("Forventet at sak ikke er null")
        assertThat(sak.harTidligereOvergangsstønad).isTrue
        assertThat(sak.harTidligereBarnetilsyn).isFalse
        assertThat(sak.harTidligereSkolepenger).isFalse

        assertThat(tidligereVedtaksperioder.historiskPensjon).isFalse

        verify(exactly = 1) { infotrygdReplikaClient.hentPerioder(any()) }
        verify(exactly = 1) { tilkjentYtelseService.hentForBehandling(behandling.id) }
        verify(exactly = 1) {
            historiskPensjonService.hentHistoriskPensjon(personIdent.ident, setOf(personIdent.ident))
        }

        assertThat(infotrygdPeriodeRequestSlot.captured.personIdenter).containsExactly(personIdent.ident)
    }

    @Test
    internal fun `hvis en person ikke har noen aktive andeler så har man ikke tidligere vedtaksperioder i ef`() {
        mockTidligereVedtakEfSak(harAndeler = false)

        val tidligereVedtaksperioder = service.hentTidligereVedtaksperioder(listOf(personIdent))

        val sak = tidligereVedtaksperioder.sak ?: error("Forventet at sak ikke er null")
        assertThat(sak.harTidligereOvergangsstønad).isFalse
        assertThat(sak.harTidligereBarnetilsyn).isFalse
        assertThat(sak.harTidligereSkolepenger).isFalse
    }

    @Test
    internal fun `Skal filtrere bort opphør`() {
        val andel2 = andel.copy(erOpphør = true)
        every { andelsHistorikkService.hentHistorikk(fagsaker.overgangsstønad!!.id, null) } returns
            listOf(andel, andel2, andel)

        val overgangstønadsperioder = service.hentOvergangstønadsperioder(fagsaker)

        assertThat(overgangstønadsperioder).hasSize(2)
    }

    @Test
    internal fun `Skal fjerne uvesentlige perioder`() {
        val historikkEndring = HistorikkEndring(
            type = EndringType.ERSTATTET,
            behandlingId = UUID.randomUUID(),
            vedtakstidspunkt = LocalDateTime.now(),
        )
        val historikkEndring2 = HistorikkEndring(
            type = EndringType.FJERNET,
            behandlingId = UUID.randomUUID(),
            vedtakstidspunkt = LocalDateTime.now(),
        )
        val andel2 = andel.copy(endring = historikkEndring)
        val andel3 = andel.copy(endring = historikkEndring2)

        every { andelsHistorikkService.hentHistorikk(fagsaker.overgangsstønad!!.id, null) } returns
            listOf(andel, andel2, andel3)

        val overgangstønadsperioder = service.hentOvergangstønadsperioder(fagsaker)

        assertThat(overgangstønadsperioder).hasSize(1)
    }

    @Test
    internal fun `Skal ha en periode med null utbetaling`() {
        val andel2 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 0))
        every { andelsHistorikkService.hentHistorikk(fagsaker.overgangsstønad!!.id, null) } returns
            listOf(andel2)

        val overgangstønadsperioder = service.hentOvergangstønadsperioder(fagsaker)

        assertThat(overgangstønadsperioder.first().harPeriodeUtenUtbetaling).isTrue()
    }

    @Test
    internal fun `Skal slå sammen tre andeler hvor bare en har null beløp og returnerer harPeriodeUtenUtbetaling lik true`() {
        val periode1 = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12))
        val periode2 = Månedsperiode(YearMonth.of(2023, 1), YearMonth.of(2023, 12))
        val periode3 = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))

        val andel1 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 500, periode = periode1))
        val andel2 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 0, periode = periode2))
        val andel3 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 100, periode = periode3))

        every { andelsHistorikkService.hentHistorikk(fagsaker.overgangsstønad!!.id, null) } returns
            listOf(andel1, andel2, andel3)

        val overgangstønadsperioder = service.hentOvergangstønadsperioder(fagsaker)

        assertThat(overgangstønadsperioder).hasSize(1)
        assertThat(overgangstønadsperioder.first().harPeriodeUtenUtbetaling).isTrue()
    }

    @Test
    internal fun `Skal slå sammen tre andeler hvor ingen har null beløp og returnerer harPeriodeUtenUtbetaling lik false`() {
        val periode1 = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12))
        val periode2 = Månedsperiode(YearMonth.of(2023, 1), YearMonth.of(2023, 12))
        val periode3 = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))

        val andel1 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 500, periode = periode1))
        val andel2 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 200, periode = periode2))
        val andel3 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 100, periode = periode3))

        every { andelsHistorikkService.hentHistorikk(fagsaker.overgangsstønad!!.id, null) } returns
            listOf(andel1, andel2, andel3)

        val overgangstønadsperioder = service.hentOvergangstønadsperioder(fagsaker)

        assertThat(overgangstønadsperioder).hasSize(1)
        assertThat(overgangstønadsperioder.first().harPeriodeUtenUtbetaling).isFalse()
    }

    @Test
    internal fun `Skal ikke feile hvis det ikke finnes noen perioder`() {
        val overgangstønadsperioder = service.hentOvergangstønadsperioder(fagsaker)

        assertThat(overgangstønadsperioder).hasSize(0)
    }

    @Test
    internal fun `Skal ikke slå sammen perioder med ulik periodetype`() {
        val periode1 = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12))
        val periode2 = Månedsperiode(YearMonth.of(2023, 1), YearMonth.of(2023, 12))
        val periode3 = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))
        val historikk1 = grunnlagsdataPeriodeHistorikk(false, periode1.fomDato, periode1.tomDato)
        val historikk2 = grunnlagsdataPeriodeHistorikk(
            false,
            periode2.fomDato,
            periode2.tomDato,
            periodeType = VedtaksperiodeType.FORLENGELSE,
        )
        val historikk3 = grunnlagsdataPeriodeHistorikk(false, periode3.fomDato, periode3.tomDato)
        val perioderMedLikPeriodetype =
            listOf(historikk2, historikk3, historikk1).slåSammenPåfølgendePerioderMedLikPeriodetype()
        assertThat(perioderMedLikPeriodetype).hasSize(3)
    }

    @Test
    internal fun `Skal slå sammen perioder med lik periodetype`() {
        val periode1 = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12))
        val periode2 = Månedsperiode(YearMonth.of(2023, 1), YearMonth.of(2023, 12))
        val periode3 = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))
        val historikk1 = grunnlagsdataPeriodeHistorikk(false, periode1.fomDato, periode1.tomDato)
        val historikk2 = grunnlagsdataPeriodeHistorikk(false, periode2.fomDato, periode2.tomDato)
        val historikk3 = grunnlagsdataPeriodeHistorikk(false, periode3.fomDato, periode3.tomDato)
        val perioderMedLikPeriodetype =
            listOf(historikk2, historikk3, historikk1).slåSammenPåfølgendePerioderMedLikPeriodetype()
        assertThat(perioderMedLikPeriodetype).hasSize(1)
    }

    @Test
    internal fun `Skal slå sammen perioder og sette harNullbeløp hvis en periode som merges har nullbeløp`() {
        val periode1 = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12))
        val periode2 = Månedsperiode(YearMonth.of(2023, 1), YearMonth.of(2023, 12))
        val periode3 = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))
        val historikk1 = grunnlagsdataPeriodeHistorikk(false, periode1.fomDato, periode1.tomDato)
        val historikk2 = grunnlagsdataPeriodeHistorikk(true, periode2.fomDato, periode2.tomDato)
        val historikk3 = grunnlagsdataPeriodeHistorikk(false, periode3.fomDato, periode3.tomDato)
        val perioderMedLikPeriodetype =
            listOf(historikk2, historikk3, historikk1).slåSammenPåfølgendePerioderMedLikPeriodetype()
        assertThat(perioderMedLikPeriodetype).hasSize(1)
        assertThat(perioderMedLikPeriodetype.first().harPeriodeUtenUtbetaling).isTrue
    }

    private fun grunnlagsdataPeriodeHistorikk(
        harNullbeløp: Boolean = false,
        fom: LocalDate,
        tom: LocalDate,
        periodeType: VedtaksperiodeType = HOVEDPERIODE,
    ) =
        GrunnlagsdataPeriodeHistorikk(periodeType = periodeType, fom = fom, tom = tom, harNullbeløp)

    private fun mockTidligereVedtakEfSak(harAndeler: Boolean = false) {
        every { fagsakPersonService.finnPerson(any()) } returns fagsakPerson
        every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
        val andelerTilkjentYtelse =
            if (harAndeler) listOf(lagAndelTilkjentYtelse(100, LocalDate.now(), LocalDate.now())) else emptyList()
        every { tilkjentYtelseService.hentForBehandling(behandling.id) } returns lagTilkjentYtelse(andelerTilkjentYtelse)
    }

    val andel = AndelHistorikkDto(
        behandlingId = UUID.randomUUID(),
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
        vedtakstidspunkt = LocalDateTime.now(),
        saksbehandler = "",
        vedtaksperiode = VedtakshistorikkperiodeOvergangsstønad(
            periode = Månedsperiode(YearMonth.now()),
            aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT,
            periodeType = HOVEDPERIODE,
            inntekt = Inntekt(YearMonth.now(), BigDecimal.ZERO, BigDecimal.ZERO),
        ),
        andel = andelMedGrunnlagDto(),
        aktivitet = null,
        aktivitetArbeid = null,
        periodeType = HOVEDPERIODE,
        erSanksjon = false,
        sanksjonsårsak = null,
        erOpphør = false,
        periodetypeBarnetilsyn = null,
        aktivitetBarnetilsyn = null,
        endring = null,
    )

    private fun andelMedGrunnlagDto() = AndelMedGrunnlagDto(
        beløp = 0,
        periode = Månedsperiode(YearMonth.now()),
        inntekt = 0,
        inntektsreduksjon = 0,
        samordningsfradrag = 0,
        kontantstøtte = 0,
        tilleggsstønad = 0,
        antallBarn = 0,
        barn = emptyList(),
        sats = 0,
        beløpFørFratrekkOgSatsJustering = 0,
    )
}
