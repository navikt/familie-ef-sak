package no.nav.familie.ef.sak.behandling.revurdering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.amelding.InntektType
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.inntekt
import no.nav.familie.ef.sak.repository.inntektsmåneder
import no.nav.familie.ef.sak.repository.inntektsperiode
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.repository.vedtaksperiode
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import java.time.YearMonth
import java.util.UUID

class AutomatiskRevurderingServiceTest {
    val behandlingServiceMock = mockk<BehandlingService>(relaxed = true)
    val oppgaveServiceMock = mockk<OppgaveService>(relaxed = true)
    val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
    val automatiskRevurderingService = AutomatiskRevurderingService(mockk(relaxed = true), mockk(relaxed = true), behandlingServiceMock, oppgaveServiceMock, mockk(relaxed = true), mockk(relaxed = true), vedtakServiceMock, mockk(relaxed = true))

    @Test
    fun `person med behandling som kan automatisk revurderes`() {
        val vedtakMed1Periode =
            vedtak(
                behandlingId = UUID.randomUUID(),
                perioder = PeriodeWrapper(listOf(vedtaksperiode(2025))),
                inntekter = InntektWrapper(listOf(inntektsperiode(2025))),
            )
        every { vedtakServiceMock.hentVedtak(any()) } returns vedtakMed1Periode
        assertThat(automatiskRevurderingService.kanAutomatiskRevurderes("1")).isTrue()
    }

    @Test
    fun `kan ikke automatisk revurderes som følge av åpen behandling`() {
        every { behandlingServiceMock.finnesÅpenBehandling(any()) } returns true

        assertThat(automatiskRevurderingService.kanAutomatiskRevurderes("1")).isFalse()
    }

    @Test
    fun `kan ikke automatisk revurderes som følge av at behandle sak oppgave finnes`() {
        every { oppgaveServiceMock.hentOppgaver(any()) } returns
            FinnOppgaveResponseDto(
                1,
                listOf(
                    Oppgave(
                        id = 1,
                        aktoerId = "1",
                        identer = listOf(OppgaveIdentV2("14510161344", IdentGruppe.FOLKEREGISTERIDENT)),
                        tema = Tema.ENF,
                        oppgavetype = Oppgavetype.BehandleSak.toString(),
                        status = StatusEnum.AAPNET,
                        versjon = 2,
                    ),
                ),
            )

        assertThat(automatiskRevurderingService.kanAutomatiskRevurderes("1")).isFalse()
    }

    @Test
    fun `kan ikke automatisk revurderes som følge av at gjeldende behandling er en g-omregning hvor forrige behandling har flere vedtaksperioder`() {
        val forrigeBehandlingId = UUID.randomUUID()
        every { behandlingServiceMock.finnSisteIverksatteBehandling(any()) } returns behandling(status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET, forrigeBehandlingId = forrigeBehandlingId, årsak = BehandlingÅrsak.G_OMREGNING)

        val vedtakMedToPerioder =
            vedtak(
                behandlingId = forrigeBehandlingId,
                perioder = PeriodeWrapper(listOf(vedtaksperiode(YearMonth.now().year), vedtaksperiode(YearMonth.now().minusYears(1).year))),
                inntekter = InntektWrapper(listOf(inntektsperiode(2025))),
            )
        every { vedtakServiceMock.hentVedtak(forrigeBehandlingId) } returns vedtakMedToPerioder

        assertThat(automatiskRevurderingService.kanAutomatiskRevurderes("1")).isFalse()

        verify(exactly = 1) { vedtakServiceMock.hentVedtak(forrigeBehandlingId) }
    }

    @Test
    fun `skal kunne beregne totalinntekt fra og med x antall måneder tilbake i tid`() {
        val fraOgMedMåned = YearMonth.now().minusYears(1)
        val inntekterPerMåned = listOf(inntekt(1000.0), inntekt(1000.0), inntekt(1000.0))
        val inntektsmåneder = inntektsmåneder(fraOgMedMåned = fraOgMedMåned, inntektListe = inntekterPerMåned)
        val inntektResponse = InntektResponse(inntektsmåneder)

        val totalInntektFor12MånederTilbake = inntektResponse.totalInntektFraÅrMåned(fraOgMedMåned)
        val totalInntektFor6MånederTilbake = inntektResponse.totalInntektFraÅrMåned(YearMonth.now().minusMonths(6))
        val totalInntektFor1MånedTilbake = inntektResponse.totalInntektFraÅrMåned(YearMonth.now().minusMonths(1))

        assertThat(totalInntektFor12MånederTilbake).isEqualTo(36000)
        assertThat(totalInntektFor6MånederTilbake).isEqualTo(18000)
        assertThat(totalInntektFor1MånedTilbake).isEqualTo(3000)
    }

    @Test
    fun `skal fjerne ef overgangstønad og beregne forventet inntekt hvor den filtrerer bort ugyldige måneder`() {
        val inntekterSisteTreMånederOvergangsstønad = inntektsmåneder(YearMonth.now().minusMonths(3), YearMonth.now().plusMonths(1), inntektListe = listOf(inntekt(16000.0, InntektType.YTELSE_FRA_OFFENTLIGE, "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere")))
        val inntekterSisteTreMånederFastlønn = inntektsmåneder(YearMonth.now().minusMonths(3), YearMonth.now().plusMonths(1), inntektListe = listOf(inntekt(5000.0)))
        val inntekterSisteTreMånederFastlønn2 = inntektsmåneder(YearMonth.now().minusMonths(3), YearMonth.now().plusMonths(1), inntektListe = listOf(inntekt(1000.0)))
        val inntekterFraSeksMånederTilTreMånederSidenFastlønn = inntektsmåneder(YearMonth.now().minusMonths(6), inntektListe = listOf(inntekt(1400.0))).take(3)

        val inntekter = inntekterSisteTreMånederOvergangsstønad + inntekterSisteTreMånederFastlønn + inntekterSisteTreMånederFastlønn2 + inntekterFraSeksMånederTilTreMånederSidenFastlønn
        val inntektResponse = InntektResponse(inntekter)

        val inntekterUtenOvergangsstønad = inntektResponse.inntektsmånederFraOgMedÅrMåned(YearMonth.now().minusMonths(6))
        val forventetInntekt = inntektResponse.forventetMånedsinntekt()

        assertThat(inntekterUtenOvergangsstønad.size).isEqualTo(12)
        assertThat(forventetInntekt).isEqualTo(6000)
    }

    @Test
    fun `beregn forventetMånedsinntekt`() {
        val inntekterSisteTreMåneder = inntektsmåneder(YearMonth.now().minusMonths(3), inntektListe = listOf(inntekt(2000.0))).take(3)

        val inntektResponse = InntektResponse(inntekterSisteTreMåneder)

        assertThat(inntektResponse.forventetMånedsinntekt()).isEqualTo(2000)
    }
}
