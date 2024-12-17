package no.nav.familie.ef.sak.selvstendig

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.slot
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.sigrun.ekstern.PensjonsgivendeInntektForSkatteordning
import no.nav.familie.ef.sak.sigrun.ekstern.PensjonsgivendeInntektResponse
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import no.nav.familie.ef.sak.sigrun.ekstern.Skatteordning
import no.nav.familie.ef.sak.testutil.kjørSomLeader
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class NæringsinntektKontrollServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var næringsinntektKontrollService: NæringsinntektKontrollService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var oppgaveClient: OppgaveClient

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var sigrunClient: SigrunClient

    @Autowired
    private lateinit var kafkaMeldingSlot: CapturingSlot<String>

    private val personIdent = "11111111111"
    private val fagsakTilknyttetPersonIdent = fagsak(setOf(PersonIdent(personIdent)))

    private val opprettOppgaveRequestSlot = slot<OpprettOppgaveRequest>()
    private val oppdaterOppgaveSlot = slot<Oppgave>()
    private val behandlingIds = mutableListOf<UUID>()

    @BeforeEach
    fun setup() {
        kafkaMeldingSlot.clear()
        behandlingIds.clear()
        val finnOppgaveRequest =
            FinnOppgaveRequest(
                tema = Tema.ENF,
                behandlingstema = Behandlingstema.Overgangsstønad,
                oppgavetype = Oppgavetype.Fremlegg,
                fristTomDato = LocalDate.of(YearMonth.now().year, 12, 15),
                mappeId = 107,
            )
        every { oppgaveClient.hentOppgaver(finnOppgaveRequest) } returns FinnOppgaveResponseDto(1, listOf(lagEksternTestOppgave()))
        every { oppgaveClient.oppdaterOppgave(capture(oppdaterOppgaveSlot)) } returns 2
        every { oppgaveClient.opprettOppgave(capture(opprettOppgaveRequestSlot)) } returns 1

        testoppsettService.lagreFagsak(fagsakTilknyttetPersonIdent)

        for (i in 0..3) {
            val behandling = behandling(id = UUID.randomUUID(), fagsak = fagsakTilknyttetPersonIdent, status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET)
            val behandlingId = behandlingRepository.insert(behandling).id
            behandlingIds.add(behandlingId)
            val vedtak = vedtak(behandlingId = behandlingId, perioder = PeriodeWrapper(objectMapper.readValue<List<Vedtaksperiode>>(vedtaksperiodeJsonList[i])), inntekter = InntektWrapper(objectMapper.readValue<List<Inntektsperiode>>(inntektsperiodeJsonList[i])))
            vedtakRepository.insert(vedtak)
        }
    }

    private fun lagEksternTestOppgave(tilordnetRessurs: String? = null): Oppgave = Oppgave(id = 1, tilordnetRessurs = tilordnetRessurs, oppgavetype = Oppgavetype.Fremlegg.toString(), fristFerdigstillelse = LocalDate.of(YearMonth.now().year, 12, 15).toString(), mappeId = 107, identer = listOf(OppgaveIdentV2(personIdent, IdentGruppe.FOLKEREGISTERIDENT)))

    @Test
    fun `Bruker har 10 prosent endring i inntekt - virkelighetsnært eksempel med andeler`() {
        val pensjonsgivendeInntektForSkatteordning = PensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND, LocalDate.now(), 0, 0, 460_000, 0)
        every { sigrunClient.hentPensjonsgivendeInntekt(any(), any()) } answers {
            PensjonsgivendeInntektResponse(firstArg(), secondArg(), listOf(pensjonsgivendeInntektForSkatteordning))
        }

        lagreAndelerTilkjentYtelse()

        kjørSomLeader {
            val fagsakIds = næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende()
            assertThat(fagsakIds.first()).isEqualTo(fagsakTilknyttetPersonIdent.id)
            assertThat(opprettOppgaveRequestSlot.captured.oppgavetype).isEqualTo(Oppgavetype.Fremlegg)
        }
    }

    @Test
    fun `Bruker har under 10 prosent endring i inntekt - virkelighetsnært eksempel med andeler`() {
        val pensjonsgivendeInntektForSkatteordning = PensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND, LocalDate.now(), 0, 0, 90_000, 0)
        every { sigrunClient.hentPensjonsgivendeInntekt(any(), any()) } answers {
            PensjonsgivendeInntektResponse(firstArg(), secondArg(), listOf(pensjonsgivendeInntektForSkatteordning))
        }

        lagreAndelerTilkjentYtelse()

        kjørSomLeader {
            val fagsakIds = næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende()
            assertThat(fagsakIds).isEmpty()
            assertThat(kafkaMeldingSlot.isCaptured).isFalse
            assertThat(oppdaterOppgaveSlot.isCaptured).isTrue
        }
    }

    @Test
    fun `Bruker har under 10 prosent endring i inntekt med løpende stønad - skal få beskjed`() {
        val pensjonsgivendeInntektForSkatteordning = PensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND, LocalDate.now(), 0, 0, 60_000, 0)
        every { sigrunClient.hentPensjonsgivendeInntekt(any(), any()) } answers {
            PensjonsgivendeInntektResponse(firstArg(), secondArg(), listOf(pensjonsgivendeInntektForSkatteordning))
        }
        val fom = LocalDate.of(YearMonth.now().year - 1, 1, 1)
        val tom = LocalDate.of(YearMonth.now().year + 1, 6, 30)
        val andelTilkjentYtelse = (lagAndelTilkjentYtelse(22761, fom, tom, personIdent, behandlingIds[3], 60_000, 0, 494))
        val tilkjentYtelse = lagTilkjentYtelse(andelerTilkjentYtelse = listOf(andelTilkjentYtelse), behandlingId = behandlingIds[3], personident = personIdent, startdato = LocalDate.of(2022, 9, 1), grunnbeløpsmåned = YearMonth.of(2024, 5))
        tilkjentYtelseRepository.insert(tilkjentYtelse)

        kjørSomLeader {
            val fagsakIds = næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende()
            assertThat(fagsakIds).isEmpty()
            assertThat(kafkaMeldingSlot.isCaptured).isTrue
            assertThat(oppdaterOppgaveSlot.isCaptured).isTrue
        }
    }

    @Test
    fun `Bruker har innvilget overgangsstønad midt i fjoråret og forventet inntekt må beregnes riktig`() {
        val pensjonsgivendeInntektForSkatteordning = PensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND, LocalDate.now(), 0, 0, 100_000, 0)
        every { sigrunClient.hentPensjonsgivendeInntekt(any(), any()) } answers {
            PensjonsgivendeInntektResponse(firstArg(), secondArg(), listOf(pensjonsgivendeInntektForSkatteordning))
        }
        val fom = LocalDate.of(YearMonth.now().year - 1, 5, 1)
        val tom = LocalDate.of(YearMonth.now().year + 1, 6, 30)
        val andelTilkjentYtelse = (lagAndelTilkjentYtelse(22761, fom, tom, personIdent, behandlingIds[3], 100_000, 0, 494))
        val tilkjentYtelse = lagTilkjentYtelse(andelerTilkjentYtelse = listOf(andelTilkjentYtelse), behandlingId = behandlingIds[3], personident = personIdent, startdato = LocalDate.of(2022, 9, 1), grunnbeløpsmåned = YearMonth.of(2024, 5))
        tilkjentYtelseRepository.insert(tilkjentYtelse)

        kjørSomLeader {
            val fagsakIds = næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende()
            assertThat(fagsakIds).isEmpty()
            assertThat(kafkaMeldingSlot.isCaptured).isTrue
            assertThat(oppdaterOppgaveSlot.isCaptured).isTrue
        }
    }

    private fun lagreAndelerTilkjentYtelse() {
        val andelerTilkjentYtelse = mutableListOf<AndelTilkjentYtelse>()
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(17517, LocalDate.of(2022, 9, 1), LocalDate.of(2023, 1, 31), personIdent, behandlingIds[0], 146000, 0, 3385))
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(19392, LocalDate.of(2023, 2, 1), LocalDate.of(2023, 4, 30), personIdent, behandlingIds[1], 96000, 0, 1510))
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(20865, LocalDate.of(2023, 5, 1), LocalDate.of(2023, 7, 31), personIdent, behandlingIds[1], 96000, 0, 1376))
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(21765, LocalDate.of(2023, 8, 1), LocalDate.of(2024, 4, 30), personIdent, behandlingIds[2], 72000, 0, 476))
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(22761, LocalDate.of(2024, 5, 1), LocalDate.of(2024, 7, 31), personIdent, behandlingIds[3], 75200, 0, 494))
        val tilkjentYtelse = lagTilkjentYtelse(andelerTilkjentYtelse = andelerTilkjentYtelse, behandlingId = behandlingIds[3], personident = personIdent, startdato = LocalDate.of(2022, 9, 1), grunnbeløpsmåned = YearMonth.of(2024, 5))
        tilkjentYtelseRepository.insert(tilkjentYtelse)
    }

    @Test
    fun `Bruker har under 4 mnd med overgangsstønad for fjoråret, og skal dermed ikke kontrolleres`() {
        val fom = LocalDate.of(YearMonth.now().year - 1, 5, 1)
        val tom = LocalDate.of(YearMonth.now().year - 1, 6, 30)
        val andelTilkjentYtelse = (lagAndelTilkjentYtelse(22761, fom, tom, personIdent, behandlingIds[3], 75200, 0, 494))
        val tilkjentYtelse = lagTilkjentYtelse(andelerTilkjentYtelse = listOf(andelTilkjentYtelse), behandlingId = behandlingIds[3], personident = personIdent, startdato = LocalDate.of(2022, 9, 1), grunnbeløpsmåned = YearMonth.of(2024, 5))
        tilkjentYtelseRepository.insert(tilkjentYtelse)

        kjørSomLeader {
            assertThat(kafkaMeldingSlot.isCaptured).isFalse()
            næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende()
        }
    }

    @Test
    fun `Bruker har under 4 mnd med overgangsstønad for dette året, og skal ikke ha fremleggsoppgave`() {
        val fom = LocalDate.of(YearMonth.now().year, 5, 1)
        val tom = LocalDate.of(YearMonth.now().year, 6, 30)
        val andelTilkjentYtelse = (lagAndelTilkjentYtelse(22761, fom, tom, personIdent, behandlingIds[3], 75200, 0, 494))
        val tilkjentYtelse = lagTilkjentYtelse(andelerTilkjentYtelse = listOf(andelTilkjentYtelse), behandlingId = behandlingIds[3], personident = personIdent, startdato = LocalDate.of(2022, 9, 1), grunnbeløpsmåned = YearMonth.of(2024, 5))
        tilkjentYtelseRepository.insert(tilkjentYtelse)

        kjørSomLeader {
            næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende()
            assertThat(opprettOppgaveRequestSlot.isCaptured).isFalse()
        }
    }

    @Test
    fun `Bruker oppfyller ikke aktivitetskravet og det skal dermed bes om regnskap med varsel til bruker`() {
        val pensjonsgivendeInntektForSkatteordning = PensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND, LocalDate.now(), 0, 0, 0, 0)
        every { sigrunClient.hentPensjonsgivendeInntekt(any(), any()) } answers {
            PensjonsgivendeInntektResponse(firstArg(), secondArg(), listOf(pensjonsgivendeInntektForSkatteordning))
        }
        val fom = LocalDate.of(YearMonth.now().year - 1, 5, 1)
        val tom = LocalDate.of(YearMonth.now().year - 1, 9, 30)
        val andelTilkjentYtelse = (lagAndelTilkjentYtelse(22761, fom, tom, personIdent, behandlingIds[3], 0, 0, 494))
        val tilkjentYtelse = lagTilkjentYtelse(andelerTilkjentYtelse = listOf(andelTilkjentYtelse), behandlingId = behandlingIds[3], personident = personIdent, startdato = LocalDate.of(2022, 9, 1), grunnbeløpsmåned = YearMonth.of(2024, 5))
        tilkjentYtelseRepository.insert(tilkjentYtelse)

        kjørSomLeader {
            næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende()
            assertThat(kafkaMeldingSlot.captured).contains("regnskap")
        }
    }
}

val vedtaksperiodeJsonList =
    listOf(
        """[{"datoFra":"2022-09-01","datoTil":"2023-07-31","aktivitet":"FORSØRGER_I_ARBEID","periodeType":"HOVEDPERIODE"}]""",
        """[{"datoFra":"2023-02-01","datoTil":"2023-07-31","aktivitet":"FORSØRGER_I_ARBEID","periodeType":"HOVEDPERIODE","sanksjonsårsak":null}]""",
        """[{"datoFra":"2023-08-01","datoTil":"2024-07-31","aktivitet":"FORLENGELSE_MIDLERTIDIG_SYKDOM","periodeType":"FORLENGELSE","sanksjonsårsak":null}]""",
        """[{"datoFra":"2024-05-01","datoTil":"2024-07-31","aktivitet":"FORLENGELSE_MIDLERTIDIG_SYKDOM","periodeType":"FORLENGELSE","sanksjonsårsak":null}]""",
    )

val inntektsperiodeJsonList =
    listOf(
        """[{"startDato":"2022-09-01","sluttDato":"+999999999-12-31","inntekt":146000,"samordningsfradrag":0}]""",
        """[{"startDato":null,"sluttDato":null,"periode":{"fom":"2023-02","tom":"999999999-12"},"dagsats":0,"månedsinntekt":0,"inntekt":96000,"samordningsfradrag":0}]""",
        """[{"startDato":null,"sluttDato":null,"periode":{"fom":"2023-08","tom":"999999999-12"},"dagsats":0,"månedsinntekt":0,"inntekt":72000,"samordningsfradrag":0}]""",
        """[{"startDato":null,"sluttDato":null,"periode":{"fom":"2024-05","tom":"999999999-12"},"dagsats":0,"månedsinntekt":0,"inntekt":75200,"samordningsfradrag":0}]""",
    )
