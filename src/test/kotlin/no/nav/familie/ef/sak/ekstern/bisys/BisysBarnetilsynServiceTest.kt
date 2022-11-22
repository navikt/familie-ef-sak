package no.nav.familie.ef.sak.ekstern.bisys

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
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
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelMedGrunnlagDto
import no.nav.familie.ef.sak.vedtak.historikk.EndringType
import no.nav.familie.ef.sak.vedtak.historikk.HistorikkEndring
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
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
    val barnetilsynBisysService = BisysBarnetilsynService(
        personService,
        fagsakService,
        behandlingService,
        barnService,
        tilkjentYtelseService,
        andelsHistorikkService,
        infotrygdService
    )

    val fagsak: Fagsak = fagsak()
    val personident = "12345678910"
    val personIdentBarnAndelHistorikk = "1234567891"
    val behandlingBarn = listOf(
        BehandlingBarn(
            id = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            søknadBarnId = null,
            personIdent = personIdentBarnAndelHistorikk
        )
    )

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
            barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
                personident,
                LocalDate.now()
            ).barnetilsynBisysPerioder
        ).isEmpty()
    }

    @Test
    fun `personident med andelhistorikk som har endringtype fjernet, forvent tom liste med barntilsynBisysPerioder`() {
        mockTilkjentYtelse()
        val andelhistorikkDto =
            lagAndelHistorikkDto(
                tilOgMed = LocalDate.now(),
                behandlingBarn = behandlingBarn,
                endring = HistorikkEndring(EndringType.FJERNET, UUID.randomUUID(), LocalDateTime.now())
            )
        every { andelsHistorikkService.hentHistorikk(any(), any()) } returns listOf(andelhistorikkDto)
        assertThat(
            barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
                personident,
                LocalDate.now()
            ).barnetilsynBisysPerioder
        ).isEmpty()
    }

    @Test
    fun `personident med andelhistorikk som har beløp lik null, forvent tom liste med barntilsynBisysPerioder`() {
        mockTilkjentYtelse()
        val andelhistorikkDto =
            lagAndelHistorikkDto(tilOgMed = LocalDate.now().plusDays(1), behandlingBarn = behandlingBarn, beløp = 0)
        every { andelsHistorikkService.hentHistorikk(any(), any()) } returns listOf(andelhistorikkDto)
        assertThat(
            barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
                personident,
                LocalDate.now()
            ).barnetilsynBisysPerioder
        ).isEmpty()
    }

    @Test
    fun `personident med andelhistorikk som har tilOgMedDato før fomDato, forvent tom liste med barntilsynBisysPerioder`() {
        mockTilkjentYtelse()
        val andelhistorikkDto =
            lagAndelHistorikkDto(tilOgMed = LocalDate.now().minusYears(1), behandlingBarn = behandlingBarn)
        every { andelsHistorikkService.hentHistorikk(any(), any()) } returns listOf(andelhistorikkDto)
        assertThat(
            barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
                personident,
                LocalDate.now()
            ).barnetilsynBisysPerioder
        ).isEmpty()
    }

    @Test
    fun `personident med gyldig andelhistorikk, forvent barnetilsynBisysResponse`() {
        mockTilkjentYtelse()
        val andelhistorikkDto =
            lagAndelHistorikkDto(tilOgMed = LocalDate.now(), behandlingBarn = behandlingBarn)

        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto)

        val fomDato = LocalDate.now()
        val bisysPeriode = barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
            personident,
            fomDato
        ).barnetilsynBisysPerioder.first()
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
                behandlingBarn = behandlingBarn
            )

        val andelhistorikkDto2 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now().plusMonths(3),
                tilOgMed = LocalDate.now().plusMonths(4),
                behandlingBarn = behandlingBarn
            )

        val andelhistorikkDto3 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now().plusMonths(6),
                tilOgMed = LocalDate.now().plusMonths(7),
                behandlingBarn = behandlingBarn
            )

        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto1, andelhistorikkDto2, andelhistorikkDto3)

        assertThat(
            barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
                personident,
                LocalDate.now()
            ).barnetilsynBisysPerioder
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
                behandlingBarn = behandlingBarn
            )

        val andelhistorikkDto2 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now().plusMonths(2),
                tilOgMed = LocalDate.now().plusMonths(3),
                behandlingBarn = behandlingBarn
            )

        val andelhistorikkDto3 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now().plusMonths(4),
                tilOgMed = senesteDato,
                behandlingBarn = behandlingBarn
            )

        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto1, andelhistorikkDto2, andelhistorikkDto3)

        val perioder = barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
            personident,
            LocalDate.now()
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

        val behandlingBarnListe = behandlingBarn + behandlingBarn.first()
            .copy(personIdent = "14041385481", søknadBarnId = UUID.randomUUID(), id = UUID.randomUUID())
        val andelhistorikkDto1 =
            lagAndelHistorikkDto(
                fraOgMed = førsteDato,
                tilOgMed = LocalDate.now().plusMonths(1),
                behandlingBarn = behandlingBarnListe
            )

        val andelhistorikkDto2 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now().plusMonths(2),
                tilOgMed = LocalDate.now().plusMonths(3),
                behandlingBarn = behandlingBarn
            )

        val andelhistorikkDto3 =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.now().plusMonths(4),
                tilOgMed = senesteDato,
                behandlingBarn = behandlingBarn
            )

        every { barnService.hentBehandlingBarnForBarnIder(any()) } returns behandlingBarnListe andThen behandlingBarn

        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto1, andelhistorikkDto2, andelhistorikkDto3)

        val perioder = barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
            personident,
            LocalDate.now()
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
        val fomDato = LocalDate.now()
        assertThat(
            barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
                personident,
                fomDato
            ).barnetilsynBisysPerioder
        ).hasSize(1)
    }

    @Test
    fun `en infotrygdperiode og en andelshistorikk etter fraOgMedDato i oppslag, forvent sammenslåtte perioder`() {
        mockTilkjentYtelse()
        val andelhistorikkDto =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.MIN.plusDays(1),
                tilOgMed = LocalDate.now().plusMonths(2),
                behandlingBarn = behandlingBarn
            )
        every {
            infotrygdService.hentSammenslåtteBarnetilsynPerioderFraReplika(any())
        } returns listOf(
            lagInfotrygdPeriode(
                vedtakId = 1,
                stønadFom = LocalDate.MIN,
                stønadTom = LocalDate.now().plusMonths(1),
                beløp = 10
            )
        )
        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto)

        val fomDato = LocalDate.now()
        assertThat(
            barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
                personident,
                fomDato
            ).barnetilsynBisysPerioder
        ).hasSize(2)
    }

    @Test
    fun `en infotrygdperiode før, og en andelshistorikk etter fraOgMedDato i oppslag, forvent kun andelshistorikk`() {
        mockTilkjentYtelse()
        val andelhistorikkDto =
            lagAndelHistorikkDto(tilOgMed = LocalDate.now().plusMonths(2), behandlingBarn = behandlingBarn)
        every {
            infotrygdService.hentSammenslåtteBarnetilsynPerioderFraReplika(any())
        } returns listOf(
            lagInfotrygdPeriode(
                vedtakId = 1,
                stønadTom = LocalDate.now().minusMonths(1),
                beløp = 10
            )
        )
        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto)

        val fomDato = LocalDate.now()
        val perioder =
            barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
                personident,
                fomDato
            ).barnetilsynBisysPerioder
        assertThat(perioder).hasSize(1)
    }

    /**
     * Denne testen er ikke gyldig og skjønner ikke hvordan den har virket tidligere
     * * Vi har aldri noen perioder i infotrygd som er gyldige etter perioder i ny løsning?
     * * Anbefaler å sette opp periodene sånn at de gir et reellt case, her så er det en andel med fom=MIN,
     *    mens startdato er satt til dagens dato
     */
    @Disabled
    @Test
    fun `en infotrygdperiode etter, og en andelshistorikk før fraOgMedDato i oppslag, forvent kun infotrygdperiode`() {
        mockTilkjentYtelse()
        val andelhistorikkDto =
            lagAndelHistorikkDto(tilOgMed = LocalDate.now().minusMonths(1), behandlingBarn = behandlingBarn)
        every {
            infotrygdService.hentSammenslåtteBarnetilsynPerioderFraReplika(any())
        } returns listOf(
            lagInfotrygdPeriode(
                vedtakId = 1,
                stønadTom = LocalDate.now().minusMonths(1),
                beløp = 10
            )
        )
        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto)

        val fomDato = LocalDate.now()
        val perioder =
            barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
                personident,
                fomDato
            ).barnetilsynBisysPerioder
        assertThat(perioder).hasSize(1)

    }

    @Test
    fun `en infotrygdperiode, og ingen fagsak for person, forvent ingen feil og kun infotrygdperiode`() {
        mockTilkjentYtelse()
        every {
            fagsakService.finnFagsak(any(), StønadType.BARNETILSYN)
        } returns null
        every {
            infotrygdService.hentSammenslåtteBarnetilsynPerioderFraReplika(any())
        } returns listOf(
            lagInfotrygdPeriode(
                vedtakId = 1,
                stønadTom = LocalDate.now().plusMonths(1),
                beløp = 10
            )
        )
        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns emptyList()

        val fomDato = LocalDate.now()
        val perioder =
            barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
                personident,
                fomDato
            ).barnetilsynBisysPerioder
        assertThat(perioder).hasSize(1)
    }

    @Test
    fun `en infotrygdperiode med tom dato som overskyter startdato, forvent avkortning av inf-tom dato`() {
        val startdato = LocalDate.MIN.plusDays(2)
        mockTilkjentYtelse(startdato)
        val efFom = YearMonth.now().atDay(1)
        val efTom = LocalDate.MAX
        val andelhistorikkDto =
            lagAndelHistorikkDto(fraOgMed = efFom, tilOgMed = efTom, behandlingBarn = behandlingBarn)
        every {
            infotrygdService.hentSammenslåtteBarnetilsynPerioderFraReplika(any())
        } returns listOf(
            lagInfotrygdPeriode(
                vedtakId = 1,
                stønadFom = LocalDate.MIN,
                stønadTom = LocalDate.MAX,
                beløp = 10
            )
        )
        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto)

        val fomDato = LocalDate.now()
        val perioder =
            barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
                personident,
                fomDato
            ).barnetilsynBisysPerioder
        val infotrygdPeriode = perioder.first()
        val efPeriode = perioder.get(1)
        assertThat(infotrygdPeriode.periode.fom).isEqualTo(LocalDate.MIN)
        assertThat(infotrygdPeriode.periode.tom).isEqualTo(startdato.minusDays(1))
        assertThat(efPeriode.periode.fom).isEqualTo(efFom)
        assertThat(efPeriode.periode.tom).isEqualTo(efTom)
    }

    @Test
    fun `en infotrygdperiode med fom-dato lik startdato, forvent sletting av inf-periode`() {
        val startdato = LocalDate.MIN
        mockTilkjentYtelse(startdato)
        val andelhistorikkDto =
            lagAndelHistorikkDto(
                fraOgMed = LocalDate.MIN,
                tilOgMed = LocalDate.MAX,
                behandlingBarn = behandlingBarn
            )
        every {
            infotrygdService.hentSammenslåtteBarnetilsynPerioderFraReplika(any())
        } returns listOf(
            lagInfotrygdPeriode(
                vedtakId = 1,
                stønadFom = startdato,
                stønadTom = LocalDate.MAX,
                beløp = 10
            )
        )
        every {
            andelsHistorikkService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto)

        val fomDato = LocalDate.MIN
        val perioder =
            barnetilsynBisysService.hentBarnetilsynperioderFraEfOgInfotrygd(
                personident,
                fomDato
            ).barnetilsynBisysPerioder
        assertThat(perioder).hasSize(1)
    }

    private fun mockTilkjentYtelse(startdato: LocalDate = LocalDate.now()) {
        every { tilkjentYtelseService.hentForBehandling(any()) } returns lagTilkjentYtelse(
            startdato = startdato,
            andelerTilkjentYtelse = emptyList()
        )
    }
}

fun lagAndelHistorikkDto(
    fraOgMed: LocalDate = LocalDate.MIN,
    tilOgMed: LocalDate,
    behandlingBarn: List<BehandlingBarn>,
    beløp: Int = 1,
    endring: HistorikkEndring? = null
): AndelHistorikkDto {
    return AndelHistorikkDto(
        behandlingId = UUID.randomUUID(),
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        vedtakstidspunkt = LocalDateTime.now(),
        saksbehandler = "",
        andel = AndelMedGrunnlagDto(
            lagAndelTilkjentYtelse(
                beløp = beløp,
                fraOgMed = fraOgMed,
                tilOgMed = tilOgMed
            ),
            null
        ).copy(barn = behandlingBarn.map { it.id }),

        aktivitet = null,
        aktivitetArbeid = null,
        periodeType = null,
        erSanksjon = false,
        sanksjonsårsak = null,
        endring = endring,
        behandlingÅrsak = BehandlingÅrsak.SØKNAD
    )
}
