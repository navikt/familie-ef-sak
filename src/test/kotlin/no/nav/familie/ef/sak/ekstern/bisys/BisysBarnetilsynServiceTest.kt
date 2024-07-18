package no.nav.familie.ef.sak.ekstern.bisys

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.lagBehandlingerForSisteIverksatte
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelMedGrunnlagDto
import no.nav.familie.ef.sak.vedtak.historikk.EndringType
import no.nav.familie.ef.sak.vedtak.historikk.HistorikkEndring
import no.nav.familie.ef.sak.vedtak.historikk.VedtakshistorikkperiodeOvergangsstønad
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class BisysBarnetilsynServiceTest {
    val personService: PersonService = mockk()
    val fagsakService: FagsakService = mockk()
    val barnService: BarnService = mockk()
    val tilkjentYtelseService: TilkjentYtelseService = mockk()
    val andelsHistorikkService = mockk<AndelsHistorikkService>(relaxed = true)
    val infotrygdService: InfotrygdService = mockk()
    val behandlingService: BehandlingService = mockk()
    val barnetilsynBisysService =
        BisysBarnetilsynService(
            personService,
            fagsakService,
            behandlingService,
            barnService,
            tilkjentYtelseService,
            andelsHistorikkService,
            infotrygdService,
        )

    val fagsak: Fagsak = fagsak()
    val personident = "12345678910"
    val personIdentBarnAndelHistorikk = "1234567891"
    val behandlingBarn =
        listOf(
            BehandlingBarn(
                id = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                søknadBarnId = null,
                personIdent = personIdentBarnAndelHistorikk,
            ),
        )
    val januar2023 = YearMonth.of(2023, 1)

    @BeforeEach
    fun setup() {
        every { personService.hentPersonIdenter(any()) } returns PdlIdenter(listOf(PdlIdent(personident, false)))
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns lagBehandlingerForSisteIverksatte().first()
        every { barnService.hentBehandlingBarnForBarnIder(any()) } returns behandlingBarn
        every {
            infotrygdService.hentSammenslåtteBarnetilsynPerioderFraReplika(any())
        } returns emptyList()
    }

    @Test
    fun `personident med ingen historikk, forvent tom liste med barntilsynBisysPerioder`() {
        mockTilkjentYtelse()
        every { andelsHistorikkService.hentHistorikk(any(), any()) } returns emptyList()

        assertThat(
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.now(),
                ).barnetilsynBisysPerioder,
        ).isEmpty()
    }

    @Test
    fun `personident med andelhistorikk som har endringtype fjernet, forvent tom liste med barntilsynBisysPerioder`() {
        mockTilkjentYtelse()
        val andelhistorikkDto =
            lagAndelHistorikkDto(
                tilOgMed = LocalDate.now(),
                behandlingBarn = behandlingBarn,
                endring = HistorikkEndring(EndringType.FJERNET, UUID.randomUUID(), LocalDateTime.now()),
            )
        mockHentHistorikk(andelhistorikkDto)

        assertThat(
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.now(),
                ).barnetilsynBisysPerioder,
        ).isEmpty()
    }

    @Test
    fun `personident med andelhistorikk som har beløp lik null, forvent tom liste med barntilsynBisysPerioder`() {
        mockTilkjentYtelse()
        mockHistorikkMedEnAndel(efPeriodeTom = LocalDate.now().plusDays(1), beløp = 0)

        assertThat(
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.now(),
                ).barnetilsynBisysPerioder,
        ).isEmpty()
    }

    @Test
    fun `personident med andelhistorikk som har tilOgMedDato før fomDato, forvent tom liste med barntilsynBisysPerioder`() {
        mockTilkjentYtelse()
        mockHistorikkMedEnAndel(efPeriodeTom = LocalDate.now().minusYears(1))

        assertThat(
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.now(),
                ).barnetilsynBisysPerioder,
        ).isEmpty()
    }

    @Test
    fun `personident med gyldig andelhistorikk, forvent barnetilsynBisysResponse`() {
        mockTilkjentYtelse()
        val andelhistorikkDto =
            lagAndelHistorikkDto(tilOgMed = LocalDate.now(), behandlingBarn = behandlingBarn)

        mockHentHistorikk(andelhistorikkDto)

        val bisysPeriode =
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.now(),
                ).barnetilsynBisysPerioder
                .first()
        assertThat(bisysPeriode.periode.fom).isEqualTo(andelhistorikkDto.andel.periode.fomDato)
        assertThat(bisysPeriode.periode.tom).isEqualTo(andelhistorikkDto.andel.periode.tomDato)
        assertThat(bisysPeriode.barnIdenter.first()).isEqualTo(behandlingBarn.first().personIdent)
    }

    @Test
    fun `Skal ikke slå sammen perioder som ikke er sammenhengende`() {
        mockTilkjentYtelse()

        val andelhistorikkDto1 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now(),
                tilOgMed = LocalDate.now().plusMonths(1),
                behandlingBarn = behandlingBarn,
            )

        val andelhistorikkDto2 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now().plusMonths(3),
                tilOgMed = LocalDate.now().plusMonths(4),
                behandlingBarn = behandlingBarn,
            )

        val andelhistorikkDto3 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now().plusMonths(6),
                tilOgMed = LocalDate.now().plusMonths(7),
                behandlingBarn = behandlingBarn,
            )

        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto1, andelhistorikkDto2, andelhistorikkDto3)

        assertThat(
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.now(),
                ).barnetilsynBisysPerioder,
        ).hasSize(3)
    }

    @Test
    fun `Skal slå sammen perioder som er sammenhengende`() {
        mockTilkjentYtelse()

        val førsteDato = YearMonth.from(LocalDate.now()).atDay(1)
        val senesteDato = YearMonth.from(LocalDate.now().plusMonths(5)).atEndOfMonth()

        val andelhistorikkDto1 =
            lagAndelHistorikkDto(
                fraOgMed = førsteDato,
                tilOgMed = LocalDate.now().plusMonths(1),
                behandlingBarn = behandlingBarn,
            )

        val andelhistorikkDto2 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now().plusMonths(2),
                tilOgMed = LocalDate.now().plusMonths(3),
                behandlingBarn = behandlingBarn,
            )

        val andelhistorikkDto3 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now().plusMonths(4),
                tilOgMed = senesteDato,
                behandlingBarn = behandlingBarn,
            )

        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto1, andelhistorikkDto2, andelhistorikkDto3)

        val perioder =
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.now(),
                ).barnetilsynBisysPerioder

        assertThat(perioder).hasSize(1)
        assertThat(perioder.first().periode.fom).isEqualTo(førsteDato)
        assertThat(perioder.first().periode.tom).isEqualTo(senesteDato)
    }

    @Test
    fun `Skal ikke slå sammen perioder som er sammenhengende hvis antall barn er ulike`() {
        mockTilkjentYtelse()

        val førsteDato = YearMonth.from(LocalDate.now()).atDay(1)
        val senesteDato = YearMonth.from(LocalDate.now().plusMonths(5)).atEndOfMonth()

        val behandlingBarnListe =
            behandlingBarn +
                behandlingBarn
                    .first()
                    .copy(personIdent = "14041385481", søknadBarnId = UUID.randomUUID(), id = UUID.randomUUID())
        val andelhistorikkDto1 =
            lagAndelHistorikkDto(
                fraOgMed = førsteDato,
                tilOgMed = LocalDate.now().plusMonths(1),
                behandlingBarn = behandlingBarnListe,
            )

        val andelhistorikkDto2 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now().plusMonths(2),
                tilOgMed = LocalDate.now().plusMonths(3),
                behandlingBarn = behandlingBarn,
            )

        val andelhistorikkDto3 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now().plusMonths(4),
                tilOgMed = senesteDato,
                behandlingBarn = behandlingBarn,
            )

        every { barnService.hentBehandlingBarnForBarnIder(any()) } returns behandlingBarnListe andThen behandlingBarn

        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto1, andelhistorikkDto2, andelhistorikkDto3)

        val perioder =
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.now(),
                ).barnetilsynBisysPerioder

        assertThat(perioder).hasSize(2)
    }

    @Test
    fun `personident med to andelshistorikker der den ene er før fomDato, forvent en andelshistorikk`() {
        mockTilkjentYtelse()
        val andelhistorikkDto =
            lagAndelHistorikkDto(tilOgMed = LocalDate.now(), behandlingBarn = behandlingBarn)
        val gammelAndelhistorikkDto =
            lagAndelHistorikkDto(tilOgMed = LocalDate.now().minusYears(1), behandlingBarn = behandlingBarn)

        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto, gammelAndelhistorikkDto)
        assertThat(
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.now(),
                ).barnetilsynBisysPerioder,
        ).hasSize(1)
    }

    @Test
    fun `Perioder skal stoppe etter ef-perioder selv om infotrygdperioder varer lenger`() {
        val efPeriodeFom = januar2023.atDay(1)
        val erPeriodeTom = januar2023.plusMonths(4).atEndOfMonth()

        val infotrygdPeriodeFom = LocalDate.MIN
        val infotrygdPeriodeTom = LocalDate.MAX

        mockTilkjentYtelse(efPeriodeFom)
        mockHentEnInfotrygdperiode(infotrygdPeriodeFom, infotrygdPeriodeTom)
        mockHistorikkMedEnAndel(efPeriodeFom, erPeriodeTom)

        val perioder =
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.MIN,
                ).barnetilsynBisysPerioder

        assertThat(perioder.first().periode.fom).isEqualTo(infotrygdPeriodeFom)
        assertThat(perioder.first().periode.tom).isEqualTo(efPeriodeFom.minusDays(1))
        assertThat(perioder.last().periode.fom).isEqualTo(efPeriodeFom)
        assertThat(perioder.last().periode.tom).isEqualTo(erPeriodeTom)
    }

    @Test
    fun `en infotrygdperiode før, og en andelshistorikk etter fraOgMedDato i oppslag, forvent kun andelshistorikk`() {
        val efTilOgMed = LocalDate.now().plusMonths(2)
        val infotrygdPeriodeTom = LocalDate.now().minusMonths(1)

        mockTilkjentYtelse()
        mockHentEnInfotrygdperiode(infotrygdPeriodeTom = infotrygdPeriodeTom, beløp = 10)
        mockHistorikkMedEnAndel(efPeriodeTom = efTilOgMed, barn = behandlingBarn)

        val perioder =
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.now(),
                ).barnetilsynBisysPerioder

        assertThat(perioder).hasSize(1)
    }

    @Test
    fun `en infotrygdperiode, og ingen fagsak for person, forvent ingen feil og kun infotrygdperiode`() {
        mockTilkjentYtelse()
        every {
            fagsakService.finnFagsak(any(), StønadType.BARNETILSYN)
        } returns null
        mockHentEnInfotrygdperiode(infotrygdPeriodeTom = LocalDate.now().plusMonths(1))
        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns emptyList()

        val perioder =
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.now(),
                ).barnetilsynBisysPerioder

        assertThat(perioder).hasSize(1)
    }

    @Test
    fun `en infotrygdperiode med fom-dato lik startdato, forvent sletting av inf-periode`() {
        val startdato = LocalDate.MIN
        mockTilkjentYtelse(startdato)
        mockHentEnInfotrygdperiode(startdato, LocalDate.MAX, beløp = 10)
        mockHistorikkMedEnAndel(startdato, LocalDate.MAX, beløp = 1)

        val perioder =
            barnetilsynBisysService
                .hentBarnetilsynperioderFraEfOgInfotrygd(
                    personident,
                    LocalDate.MIN,
                ).barnetilsynBisysPerioder

        assertThat(perioder).hasSize(1)
    }

    private fun mockHentHistorikk(andelHistorikkDto: AndelHistorikkDto) {
        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns
            listOf(
                andelHistorikkDto,
            )
    }

    private fun mockTilkjentYtelse(startdato: LocalDate = LocalDate.now()) {
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
            lagTilkjentYtelse(
                startdato = startdato,
                andelerTilkjentYtelse = emptyList(),
            )
    }

    private fun mockHentEnInfotrygdperiode(
        infotrygdPeriodeFom: LocalDate = YearMonth.now().atDay(1),
        infotrygdPeriodeTom: LocalDate,
        beløp: Int = 1,
    ) {
        val infotrygdPeriode =
            lagInfotrygdPeriode(
                stønadFom = infotrygdPeriodeFom,
                stønadTom = infotrygdPeriodeTom,
                beløp = beløp,
            )
        mockHentInfotrygdPeriode(infotrygdPeriode)
    }

    private fun mockHentInfotrygdPeriode(infotrygdPeriode: InfotrygdPeriode) {
        every {
            infotrygdService.hentSammenslåtteBarnetilsynPerioderFraReplika(any())
        } returns
            listOf(
                infotrygdPeriode,
            )
    }

    private fun mockHistorikkMedEnAndel(
        efPeriodeFom: LocalDate = LocalDate.MIN,
        efPeriodeTom: LocalDate = LocalDate.MAX,
        barn: List<BehandlingBarn> = behandlingBarn,
        beløp: Int = 1,
    ) {
        val andelHistorikkDto =
            lagAndelHistorikkDto(
                fraOgMed = efPeriodeFom,
                tilOgMed = efPeriodeTom,
                behandlingBarn = barn,
                beløp = beløp,
            )
        mockHentHistorikk(andelHistorikkDto)
    }
}

fun lagAndelHistorikkDto(
    fraOgMed: LocalDate = LocalDate.MIN,
    tilOgMed: LocalDate,
    behandlingBarn: List<BehandlingBarn> = emptyList(),
    beløp: Int = 1,
    endring: HistorikkEndring? = null,
    aktivitet: AktivitetType? = null,
    periodeType: VedtaksperiodeType? = null,
): AndelHistorikkDto =
    AndelHistorikkDto(
        behandlingId = UUID.randomUUID(),
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        vedtakstidspunkt = LocalDateTime.now(),
        saksbehandler = "",
        vedtaksperiode =
            VedtakshistorikkperiodeOvergangsstønad(
                periode = Månedsperiode(fraOgMed, tilOgMed),
                aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT,
                periodeType = VedtaksperiodeType.HOVEDPERIODE,
                inntekt = Inntekt(YearMonth.from(fraOgMed), BigDecimal.ZERO, BigDecimal.ZERO),
            ),
        andel =
            AndelMedGrunnlagDto(
                lagAndelTilkjentYtelse(
                    beløp = beløp,
                    fraOgMed = fraOgMed,
                    tilOgMed = tilOgMed,
                ),
                null,
            ).copy(barn = behandlingBarn.map { it.id }),
        aktivitet = aktivitet,
        aktivitetArbeid = null,
        periodeType = periodeType,
        erSanksjon = false,
        sanksjonsårsak = null,
        endring = endring,
        behandlingÅrsak = BehandlingÅrsak.SØKNAD,
        erOpphør = false,
        periodetypeBarnetilsyn = PeriodetypeBarnetilsyn.ORDINÆR,
        aktivitetBarnetilsyn = AktivitetstypeBarnetilsyn.I_ARBEID,
    )
