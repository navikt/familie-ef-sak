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
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonStatus
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
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType.SANKSJON
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelMedGrunnlagDto
import no.nav.familie.ef.sak.vedtak.historikk.EndringType
import no.nav.familie.ef.sak.vedtak.historikk.HistorikkEndring
import no.nav.familie.ef.sak.vedtak.historikk.Sanksjonsperiode
import no.nav.familie.ef.sak.vedtak.historikk.VedtakshistorikkperiodeOvergangsstønad
import no.nav.familie.ef.sak.vilkår.dto.tilDto
import no.nav.familie.ef.sak.vilkår.dto.tilSistePeriodeDto
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
import java.util.UUID

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

    private val service =
        TidligereVedtaksperioderService(
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
            HistoriskPensjonDto(HistoriskPensjonStatus.HAR_IKKE_HISTORIKK, "")
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
            listOf(andel, andel2, andel) // ikke påfølgende, men like perioder - vil ikke slås sammen

        mockTidligereVedtakEfSak(harAndeler = false)
        val tidligereVedtaksperioder = service.hentTidligereVedtaksperioder(listOf(personIdent)).tilDto()

        assertThat(tidligereVedtaksperioder.sak!!.periodeHistorikkOvergangsstønad).hasSize(2)
    }

    @Test
    internal fun `Skal fjerne alle uvesentlige perioder som er overskrevet av nye revurderinger`() {
        val andel1 = andel.copy(erOpphør = true)
        val andel2 = andel.copy(endring = historikkEndring(EndringType.ERSTATTET))
        val andel3 = andel.copy(endring = historikkEndring(EndringType.FJERNET))

        every { andelsHistorikkService.hentHistorikk(fagsaker.overgangsstønad!!.id, null) } returns
            listOf(andel1, andel2, andel3)

        mockTidligereVedtakEfSak(harAndeler = false)
        val tidligereVedtaksperioder = service.hentTidligereVedtaksperioder(listOf(personIdent)).tilDto()

        assertThat(tidligereVedtaksperioder.sak!!.periodeHistorikkOvergangsstønad).hasSize(0)
    }

    @Test
    internal fun `Skal ha en periode med null utbetaling`() {
        val andel2 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 0))
        every { andelsHistorikkService.hentHistorikk(fagsaker.overgangsstønad!!.id, null) } returns
            listOf(andel2)

        mockTidligereVedtakEfSak(harAndeler = false)

        val tidligereVedtaksperioder = service.hentTidligereVedtaksperioder(listOf(personIdent)).tilDto()
        val overgangstønadsperioder = tidligereVedtaksperioder.sak!!.periodeHistorikkOvergangsstønad

        assertThat(overgangstønadsperioder.first().antallMånederUtenBeløp).isEqualTo(1)
    }

    @Test
    internal fun `Skal slå sammen tre andeler hvor en på 12 mnd har null beløp `() {
        val periode1 = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12))
        val periode2 = Månedsperiode(YearMonth.of(2023, 1), YearMonth.of(2023, 12))
        val periode3 = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))

        val andel1 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 500, periode = periode1))
        val andel2 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 0, periode = periode2))
        val andel3 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 100, periode = periode3))

        every { andelsHistorikkService.hentHistorikk(fagsaker.overgangsstønad!!.id, null) } returns
            listOf(andel1, andel2, andel3)

        mockTidligereVedtakEfSak(harAndeler = false)
        val tidligereVedtaksperioder = service.hentTidligereVedtaksperioder(listOf(personIdent)).tilDto()
        val dto = tidligereVedtaksperioder.sak!!.periodeHistorikkOvergangsstønad

        assertThat(dto).hasSize(1)
        assertThat(dto.first().antallMånederUtenBeløp).isEqualTo(12)
        assertThat(dto.first().antallMåneder).isEqualTo(24)
    }

    @Test
    internal fun `Skal slå sammen tre andeler hvor ingen har null beløp`() {
        val periode1 = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12))
        val periode2 = Månedsperiode(YearMonth.of(2023, 1), YearMonth.of(2023, 12))
        val periode3 = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))

        val andel1 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 500, periode = periode1))
        val andel2 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 200, periode = periode2))
        val andel3 = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 100, periode = periode3))

        every { andelsHistorikkService.hentHistorikk(fagsaker.overgangsstønad!!.id, null) } returns
            listOf(andel1, andel2, andel3)

        mockTidligereVedtakEfSak(harAndeler = false)
        val tidligereVedtaksperioder = service.hentTidligereVedtaksperioder(listOf(personIdent))
        val overgangstønadsperioder = tidligereVedtaksperioder.tilDto().sak!!.periodeHistorikkOvergangsstønad

        assertThat(overgangstønadsperioder).hasSize(1)
        assertThat(overgangstønadsperioder.first().antallMånederUtenBeløp).isEqualTo(0)
        assertThat(overgangstønadsperioder.first().antallMåneder).isEqualTo(36)
    }

    @Test
    internal fun `Skal ikke telle mnd med sanksjon som antall uten beløp`() {
        val periode1 = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12))
        val periode2 = Månedsperiode(YearMonth.of(2023, 1))
        val sanksjonsperiode = Sanksjonsperiode(periode = periode2, sanksjonsårsak = Sanksjonsårsak.SAGT_OPP_STILLING)

        val hovedAndel = andel.copy(andel = andelMedGrunnlagDto().copy(beløp = 500, periode = periode1))
        val sanksjonAndel = andel.copy(periodeType = SANKSJON, vedtaksperiode = sanksjonsperiode, erSanksjon = true, andel = andelMedGrunnlagDto().copy(beløp = 0, periode = periode2))

        every { andelsHistorikkService.hentHistorikk(fagsaker.overgangsstønad!!.id, null) } returns
            listOf(hovedAndel, sanksjonAndel)

        mockTidligereVedtakEfSak(harAndeler = false)
        val tidligereVedtaksperioder = service.hentTidligereVedtaksperioder(listOf(personIdent))
        val overgangstønadsperioder = tidligereVedtaksperioder.tilDto().sak!!.periodeHistorikkOvergangsstønad

        assertThat(overgangstønadsperioder).hasSize(2)

        val hovedperiodeDto = overgangstønadsperioder.find { it.vedtaksperiodeType == "HOVEDPERIODE" }!!
        assertThat(hovedperiodeDto.antallMånederUtenBeløp).isEqualTo(0)
        assertThat(hovedperiodeDto.antallMåneder).isEqualTo(12)

        val sanksjonsperiodeDto = overgangstønadsperioder.find { it.vedtaksperiodeType == "SANKSJON" }!!
        assertThat(sanksjonsperiodeDto.antallMåneder).isEqualTo(1)
        assertThat(sanksjonsperiodeDto.antallMånederUtenBeløp).isEqualTo(0)
    }

    @Test
    internal fun `Skal ikke feile hvis det ikke finnes noen perioder`() {
        mockTidligereVedtakEfSak(harAndeler = false)
        val tidligereVedtaksperioder = service.hentTidligereVedtaksperioder(listOf(personIdent)).tilDto()

        val overgangstønadsperioder = tidligereVedtaksperioder.sak!!.periodeHistorikkOvergangsstønad

        assertThat(overgangstønadsperioder).hasSize(0)
    }

    @Test
    internal fun `Skal ikke slå sammen perioder med ulik periodetype`() {
        val periode1 = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12))
        val periode2 = Månedsperiode(YearMonth.of(2023, 1), YearMonth.of(2023, 12))
        val periode3 = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))
        val historikk1 = grunnlagsdataPeriodeHistorikk(123, periode1.fomDato, periode1.tomDato)
        val historikk2 =
            grunnlagsdataPeriodeHistorikk(
                123,
                periode2.fomDato,
                periode2.tomDato,
                periodeType = VedtaksperiodeType.FORLENGELSE,
            )
        val historikk3 = grunnlagsdataPeriodeHistorikk(123, periode3.fomDato, periode3.tomDato)

        val perioderMedLikPeriodetype =
            listOf(historikk2, historikk3, historikk1)

        val tidligereVedtaksperioder = TidligereInnvilgetVedtak(periodeHistorikkOvergangsstønad = perioderMedLikPeriodetype).tilDto()
        val overgangstønadsperioder = tidligereVedtaksperioder.periodeHistorikkOvergangsstønad

        assertThat(overgangstønadsperioder).hasSize(3)
    }

    @Test
    internal fun `Skal slå sammen perioder med lik periodetype`() {
        val periode1 = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12))
        val periode2 = Månedsperiode(YearMonth.of(2023, 1), YearMonth.of(2023, 12))
        val periode3 = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))
        val historikk1 = grunnlagsdataPeriodeHistorikk(123, periode1.fomDato, periode1.tomDato)
        val historikk2 = grunnlagsdataPeriodeHistorikk(123, periode2.fomDato, periode2.tomDato)
        val historikk3 = grunnlagsdataPeriodeHistorikk(123, periode3.fomDato, periode3.tomDato)
        val perioderMedLikPeriodetype =
            listOf(historikk2, historikk3, historikk1)

        val tidligereInnvilgetVedtak =
            TidligereInnvilgetVedtak(periodeHistorikkOvergangsstønad = perioderMedLikPeriodetype)

        assertThat(tidligereInnvilgetVedtak.tilDto().periodeHistorikkOvergangsstønad).hasSize(1)
    }

    @Test
    internal fun `Skal slå sammen perioder og sette harNullbeløp hvis en periode som merges har nullbeløp`() {
        val periode1 = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12))
        val periode2 = Månedsperiode(YearMonth.of(2023, 1), YearMonth.of(2023, 12))
        val periode3 = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))
        val periode4 = Månedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 12))
        val historikk1 = grunnlagsdataPeriodeHistorikk(100, periode1.fomDato, periode1.tomDato)
        val historikk2 = grunnlagsdataPeriodeHistorikk(0, periode2.fomDato, periode2.tomDato)
        val historikk3 = grunnlagsdataPeriodeHistorikk(100, periode3.fomDato, periode3.tomDato)
        val historikk4 = grunnlagsdataPeriodeHistorikk(0, periode4.fomDato, periode4.tomDato)

        val perioderMedLikPeriodetype =
            listOf(historikk2, historikk3, historikk1, historikk4)

        val tidligereVedtaksperioder = TidligereInnvilgetVedtak(periodeHistorikkOvergangsstønad = perioderMedLikPeriodetype).tilDto()

        val periodeHistorikkDtos = tidligereVedtaksperioder.periodeHistorikkOvergangsstønad
        assertThat(periodeHistorikkDtos).hasSize(1)
        assertThat(periodeHistorikkDtos.first().antallMånederUtenBeløp).isEqualTo(24)
        assertThat(periodeHistorikkDtos.first().antallMåneder).isEqualTo(24)
    }

    @Test
    fun `tilSistePeriodeDto skal sortere på fom, slik at nyeste periode blir brukt`() {
        val perioder =
            listOf(
                GrunnlagsdataPeriodeHistorikkOvergangsstønad(
                    fom = LocalDate.of(2022, 5, 1),
                    tom = LocalDate.of(2022, 9, 30),
                    periodeType = VedtaksperiodeType.SANKSJON,
                    aktivitet = AktivitetType.FORSØRGER_I_ARBEID,
                    beløp = 5555,
                    inntekt = 5555,
                    samordningsfradrag = 5555,
                ),
                GrunnlagsdataPeriodeHistorikkOvergangsstønad(
                    fom = LocalDate.of(2024, 2, 1),
                    tom = LocalDate.of(2024, 3, 31),
                    periodeType = VedtaksperiodeType.SANKSJON,
                    aktivitet = AktivitetType.FORSØRGER_I_ARBEID,
                    beløp = 4444,
                    inntekt = 4444,
                    samordningsfradrag = 4444,
                ),
                GrunnlagsdataPeriodeHistorikkOvergangsstønad(
                    fom = LocalDate.of(2023, 3, 1),
                    tom = LocalDate.of(2023, 7, 31),
                    periodeType = VedtaksperiodeType.SANKSJON,
                    aktivitet = AktivitetType.FORSØRGER_I_ARBEID,
                    beløp = 3333,
                    inntekt = 3333,
                    samordningsfradrag = 3333,
                ),
            )

        val sistePeriode = perioder.tilSistePeriodeDto()

        assertThat(sistePeriode).isNotNull
        assertThat(sistePeriode?.fom).isEqualTo(LocalDate.of(2024, 2, 1))
        assertThat(sistePeriode?.tom).isEqualTo(LocalDate.of(2024, 3, 31))
        assertThat(sistePeriode?.inntekt).isEqualTo(4444)
        assertThat(sistePeriode?.samordningsfradrag).isEqualTo(4444)
    }

    private fun grunnlagsdataPeriodeHistorikk(
        beløp: Int = 0,
        fom: LocalDate,
        tom: LocalDate,
        periodeType: VedtaksperiodeType = HOVEDPERIODE,
        aktivitet: AktivitetType = AktivitetType.FORSØRGER_I_ARBEID,
        inntekt: Int? = null,
        samordningsfradrag: Int? = null,
    ) = GrunnlagsdataPeriodeHistorikkOvergangsstønad(periodeType = periodeType, fom = fom, tom = tom, aktivitet, beløp, inntekt, samordningsfradrag)

    private fun mockTidligereVedtakEfSak(harAndeler: Boolean = false) {
        every { fagsakPersonService.finnPerson(any()) } returns fagsakPerson
        every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
        val andelerTilkjentYtelse =
            if (harAndeler) listOf(lagAndelTilkjentYtelse(100, LocalDate.now(), LocalDate.now())) else emptyList()
        every { tilkjentYtelseService.hentForBehandling(behandling.id) } returns lagTilkjentYtelse(andelerTilkjentYtelse)
    }

    val andel =
        AndelHistorikkDto(
            behandlingId = UUID.randomUUID(),
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            vedtakstidspunkt = LocalDateTime.now(),
            saksbehandler = "",
            vedtaksperiode =
                VedtakshistorikkperiodeOvergangsstønad(
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

    private fun andelMedGrunnlagDto() =
        AndelMedGrunnlagDto(
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

    private fun historikkEndring(type: EndringType) =
        HistorikkEndring(
            type = type,
            behandlingId = UUID.randomUUID(),
            vedtakstidspunkt = LocalDateTime.now(),
        )
}
