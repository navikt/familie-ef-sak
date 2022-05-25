package no.nav.familie.ef.sak.ekstern.bisys

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelMedGrunnlagDto
import no.nav.familie.ef.sak.vedtak.historikk.EndringType
import no.nav.familie.ef.sak.vedtak.historikk.HistorikkEndring
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.eksterne.kontrakter.bisys.Datakilde
import org.assertj.core.api.Assertions.assertThat

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class BisysBarnetilsynServiceTest {

    val personService: PersonService = mockk()
    val fagsakService: FagsakService = mockk()
    val barnService: BarnService = mockk()
    val tilkjentYtelseService: TilkjentYtelseService = mockk()
    val barnetilsynBisysService = BisysBarnetilsynService(personService, fagsakService, barnService, tilkjentYtelseService)

    val fagsak: Fagsak = fagsak()
    val personident = "12345678910"
    val behandlingBarn = listOf(BehandlingBarn(id = UUID.randomUUID(),
                                               behandlingId = UUID.randomUUID(),
                                               søknadBarnId = null,
                                               personIdent = "123"))

    @BeforeEach
    fun setup() {
        every { personService.hentPersonIdenter(any()) } returns PdlIdenter(listOf(PdlIdent(personident, false)))
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { barnService.hentBehandlingBarnForBarnIder(any()) } returns behandlingBarn
    }


    @Test
    fun `personident med ingen historikk, forvent tom liste med barntilsynBisysPerioder`() {

        every { tilkjentYtelseService.hentHistorikk(any(), any()) } returns emptyList()
        assertThat(barnetilsynBisysService.hentPerioderBarnetilsyn(personident,
                                                                   LocalDate.now()).barnetilsynBisysPerioder).isEmpty()

    }

    @Test
    fun `personident med andelhistorikk som har type ulik splittet, forvent tom liste med barntilsynBisysPerioder`() {
        val andelhistorikkDto =
                lagAndelHistorikkDto(tilOgMed = LocalDate.now(),
                                     behandlingBarn = behandlingBarn,
                                     endring = HistorikkEndring(EndringType.FJERNET, UUID.randomUUID(), LocalDateTime.now()))
        every { tilkjentYtelseService.hentHistorikk(any(), any()) } returns listOf(andelhistorikkDto)
        assertThat(barnetilsynBisysService.hentPerioderBarnetilsyn(personident,
                                                                   LocalDate.now()).barnetilsynBisysPerioder).isEmpty()

    }

    @Test
    fun `personident med andelhistorikk som har beløp lik null, forvent tom liste med barntilsynBisysPerioder`() {

        val andelhistorikkDto =
                lagAndelHistorikkDto(tilOgMed = LocalDate.now().plusDays(1), behandlingBarn = behandlingBarn, beløp = 0)
        every { tilkjentYtelseService.hentHistorikk(any(), any()) } returns listOf(andelhistorikkDto)
        assertThat(barnetilsynBisysService.hentPerioderBarnetilsyn(personident,
                                                                   LocalDate.now()).barnetilsynBisysPerioder).isEmpty()

    }

    @Test
    fun `personident med andelhistorikk som har tilOgMedDato før fomDato, forvent tom liste med barntilsynBisysPerioder`() {

        val andelhistorikkDto =
                lagAndelHistorikkDto(tilOgMed = LocalDate.now().minusYears(1), behandlingBarn = behandlingBarn)
        every { tilkjentYtelseService.hentHistorikk(any(), any()) } returns listOf(andelhistorikkDto)
        assertThat(barnetilsynBisysService.hentPerioderBarnetilsyn(personident,
                                                                   LocalDate.now()).barnetilsynBisysPerioder).isEmpty()

    }

    @Test
    fun `personident med gyldig andelhistorikk, forvent barnetilsynBisysResponse`() {

        val andelhistorikkDto =
                lagAndelHistorikkDto(tilOgMed = LocalDate.now(), behandlingBarn = behandlingBarn)

        every {
            tilkjentYtelseService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto)

        val fomDato = LocalDate.now()
        val bisysPeriode = barnetilsynBisysService.hentPerioderBarnetilsyn(personident, fomDato).barnetilsynBisysPerioder.first()
        assertThat(bisysPeriode.periode.fom).isEqualTo(andelhistorikkDto.andel.stønadFra)
        assertThat(bisysPeriode.periode.tom).isEqualTo(andelhistorikkDto.andel.stønadTil)
        assertThat(bisysPeriode.barnIdenter.first()).isEqualTo(behandlingBarn.first().personIdent)
        assertThat(bisysPeriode.totalbeløp).isEqualTo(andelhistorikkDto.andel.beløp)
        assertThat(bisysPeriode.datakilde).isEqualTo(Datakilde.EF)
    }

    @Test
    fun `personident med to andelshistorikker der den ene er før fomDato, forvent en andelshistorikk`() {

        val andelhistorikkDto =
                lagAndelHistorikkDto(tilOgMed = LocalDate.now(), behandlingBarn = behandlingBarn)
        val gammelAndelhistorikkDto =
                lagAndelHistorikkDto(tilOgMed = LocalDate.now().minusYears(1), behandlingBarn = behandlingBarn)

        every {
            tilkjentYtelseService.hentHistorikk(any(), any())
        } returns listOf(andelhistorikkDto, gammelAndelhistorikkDto)
        val fomDato = LocalDate.now()
        assertThat(barnetilsynBisysService.hentPerioderBarnetilsyn(personident, fomDato).barnetilsynBisysPerioder).hasSize(1)

    }

}

fun lagAndelHistorikkDto(
        fraOgMed: LocalDate = LocalDate.MIN,
        tilOgMed: LocalDate,
        behandlingBarn: List<BehandlingBarn> = emptyList(),
        beløp: Int = 1,
        endring: HistorikkEndring? = null,
        aktivitet: AktivitetType? = null,
        periodeType: VedtaksperiodeType? = null
): AndelHistorikkDto {
    return AndelHistorikkDto(
            behandlingId = UUID.randomUUID(),
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            vedtakstidspunkt = LocalDateTime.now(),
            saksbehandler = "",
            andel = AndelMedGrunnlagDto(lagAndelTilkjentYtelse(beløp = beløp,
                                                               fraOgMed = fraOgMed!!,
                                                               tilOgMed = tilOgMed),
                                        null).copy(barn = behandlingBarn.map { it.id }),

            aktivitet = aktivitet,
            aktivitetArbeid = null,
            periodeType = periodeType,
            erSanksjon = false,
            sanksjonsårsak = null,
            endring = endring

    )
}
