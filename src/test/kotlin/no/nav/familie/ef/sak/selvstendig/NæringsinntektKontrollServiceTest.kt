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
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
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
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
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

    @Autowired
    private lateinit var oppgaveRepository: OppgaveRepository

    @Autowired
    private lateinit var brevClient: BrevClient

    private val personIdent = "1"
    private val oppdaterOppgaveSlot = slot<Oppgave>()
    private val behandlingIds = mutableListOf<UUID>()
    private val finnOppgaveRequest =
        FinnOppgaveRequest(
            tema = Tema.ENF,
            behandlingstema = Behandlingstema.Overgangsstønad,
            oppgavetype = Oppgavetype.Fremlegg,
            fristTomDato = LocalDate.of(YearMonth.now().year, 12, 15),
            mappeId = 107,
        )

    @BeforeEach
    fun setup() {
        kafkaMeldingSlot.clear()
        behandlingIds.clear()

        every { oppgaveClient.hentOppgaver(finnOppgaveRequest) } returns FinnOppgaveResponseDto(1, listOf(lagEksternTestOppgave()))
        every { oppgaveClient.oppdaterOppgave(capture(oppdaterOppgaveSlot)) } returns 9
        every { oppgaveClient.finnOppgaveMedId(9) } returns lagEksternTestOppgave()
        every { brevClient.genererNæringsinntektUtenEndringNotat(any()) } returns ByteArray(0)
    }

    private fun lagEksternTestOppgave(personIdent: String = "1"): Oppgave = Oppgave(id = 9, tilordnetRessurs = null, oppgavetype = Oppgavetype.Fremlegg.toString(), fristFerdigstillelse = LocalDate.of(YearMonth.now().year, 12, 15).toString(), mappeId = 107, identer = listOf(OppgaveIdentV2(personIdent, IdentGruppe.FOLKEREGISTERIDENT)))

    @Test
    fun `Bruker har 10 prosent endring i inntekt - virkelighetsnært eksempel med andeler`() {
        every { oppgaveClient.hentOppgaver(finnOppgaveRequest) } returns FinnOppgaveResponseDto(1, listOf(lagEksternTestOppgave("2")))
        every { oppgaveClient.finnOppgaveMedId(9) } returns lagEksternTestOppgave("2")
        lagreTestdataForPersonIdent("2") // Har høy inntekt i mock
        lagreAndelerTilkjentYtelseForPersonIdent("2")

        kjørSomLeader {
            næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende(LocalDate.now().year - 1, 9)
            assertThat(kafkaMeldingSlot.isCaptured).isTrue
            assertThat(oppdaterOppgaveSlot.captured.fristFerdigstillelse).isEqualTo(LocalDate.of(LocalDate.now().year + 1, 1, 11).toString())
            assertThat(oppgaveRepository.findByBehandlingIdAndType(behandlingIds.last(), Oppgavetype.Fremlegg)?.size).isEqualTo(1)
        }
    }

    @Test
    fun `Bruker har under 10 prosent endring i inntekt - virkelighetsnært eksempel med andeler`() {
        lagreTestdataForPersonIdent(personIdent)
        val pensjonsgivendeInntektForSkatteordning = PensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND, LocalDate.now(), 0, 0, 90_000, 0)
        every { sigrunClient.hentPensjonsgivendeInntekt(any(), any()) } answers {
            PensjonsgivendeInntektResponse(firstArg(), secondArg(), listOf(pensjonsgivendeInntektForSkatteordning))
        }

        lagreAndelerTilkjentYtelseForPersonIdent()

        kjørSomLeader {
            næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende(2023, 9)
            assertThat(oppdaterOppgaveSlot.captured.status).isEqualTo(StatusEnum.FERDIGSTILT)
        }
    }

    @Test
    fun `Bruker har under 10 prosent endring i inntekt med løpende stønad - skal få beskjed`() {
        lagreTestdataForPersonIdent(personIdent)
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
            næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende(2023, 9)
            assertThat(kafkaMeldingSlot.isCaptured).isTrue
            assertThat(oppdaterOppgaveSlot.isCaptured).isTrue
        }
    }

    @Test
    fun `Bruker har innvilget overgangsstønad midt i fjoråret og forventet inntekt må beregnes riktig`() {
        lagreTestdataForPersonIdent(personIdent)
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
            næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende(2023, 9)
            assertThat(kafkaMeldingSlot.isCaptured).isTrue
            assertThat(oppdaterOppgaveSlot.isCaptured).isTrue
        }
    }

    @Test
    fun `Bruker har under 4 mnd med overgangsstønad for fjoråret, og skal dermed ikke kontrolleres`() {
        lagreTestdataForPersonIdent(personIdent)
        val fom = LocalDate.of(YearMonth.now().year - 1, 5, 1)
        val tom = LocalDate.of(YearMonth.now().year - 1, 6, 30)
        val andelTilkjentYtelse = (lagAndelTilkjentYtelse(22761, fom, tom, personIdent, behandlingIds[3], 75200, 0, 494))
        val tilkjentYtelse = lagTilkjentYtelse(andelerTilkjentYtelse = listOf(andelTilkjentYtelse), behandlingId = behandlingIds[3], personident = personIdent, startdato = LocalDate.of(2022, 9, 1), grunnbeløpsmåned = YearMonth.of(2024, 5))
        tilkjentYtelseRepository.insert(tilkjentYtelse)

        kjørSomLeader {
            assertThat(kafkaMeldingSlot.isCaptured).isFalse()
            næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende(2023, 9)
        }
    }

    @Test
    fun `Bruker har under 4 mnd med overgangsstønad for dette året, og skal ikke ha fremleggsoppgave`() {
        lagreTestdataForPersonIdent(personIdent)
        val fom = LocalDate.of(YearMonth.now().year, 5, 1)
        val tom = LocalDate.of(YearMonth.now().year, 6, 30)
        val andelTilkjentYtelse = (lagAndelTilkjentYtelse(22761, fom, tom, personIdent, behandlingIds[3], 75200, 0, 494))
        val tilkjentYtelse = lagTilkjentYtelse(andelerTilkjentYtelse = listOf(andelTilkjentYtelse), behandlingId = behandlingIds[3], personident = personIdent, startdato = LocalDate.of(2022, 9, 1), grunnbeløpsmåned = YearMonth.of(2024, 5))
        tilkjentYtelseRepository.insert(tilkjentYtelse)

        kjørSomLeader {
            næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende(2023, 9)
            assertThat(oppgaveRepository.findAll()).isEmpty()
        }
    }

    @Test
    fun `Bruker oppfyller ikke aktivitetskravet og det skal dermed bes om regnskap med varsel til bruker`() {
        lagreTestdataForPersonIdent(personIdent)
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
            næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende(2023, 9)
            assertThat(kafkaMeldingSlot.captured).contains("regnskap")
        }
    }

    private fun lagreTestdataForPersonIdent(personIdent: String) {
        val fagsakTilknyttetPersonIdent = fagsak(setOf(PersonIdent(personIdent)))
        testoppsettService.lagreFagsak(fagsakTilknyttetPersonIdent)

        for (i in 0..3) {
            val behandling = behandling(id = UUID.randomUUID(), fagsak = fagsakTilknyttetPersonIdent, status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET)
            val behandlingId = behandlingRepository.insert(behandling).id
            behandlingIds.add(behandlingId)
            val vedtak = vedtak(behandlingId = behandlingId, perioder = PeriodeWrapper(objectMapper.readValue<List<Vedtaksperiode>>(vedtaksperiodeJsonList[i])), inntekter = InntektWrapper(objectMapper.readValue<List<Inntektsperiode>>(inntektsperiodeJsonList[i])))
            vedtakRepository.insert(vedtak)
        }
    }

    private fun lagreAndelerTilkjentYtelseForPersonIdent(personIdent: String = "1") {
        val andelerTilkjentYtelse = mutableListOf<AndelTilkjentYtelse>()
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(17517, LocalDate.of(LocalDate.now().year - 3, 9, 1), LocalDate.of(LocalDate.now().year - 1, 1, 31), personIdent, behandlingIds[0], 146000, 0, 3385))
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(19392, LocalDate.of(LocalDate.now().year - 2, 2, 1), LocalDate.of(LocalDate.now().year - 1, 4, 30), personIdent, behandlingIds[1], 96000, 0, 1510))
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(20865, LocalDate.of(LocalDate.now().year - 2, 5, 1), LocalDate.of(LocalDate.now().year - 1, 7, 31), personIdent, behandlingIds[1], 96000, 0, 1376))
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(21765, LocalDate.of(LocalDate.now().year - 1, 8, 1), LocalDate.of(LocalDate.now().year, 4, 30), personIdent, behandlingIds[2], 72000, 0, 476))
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(22761, LocalDate.of(LocalDate.now().year, 5, 1), LocalDate.of(LocalDate.now().year, 7, 31), personIdent, behandlingIds[3], 75200, 0, 494))
        val tilkjentYtelse = lagTilkjentYtelse(andelerTilkjentYtelse = andelerTilkjentYtelse, behandlingId = behandlingIds[3], personident = personIdent, startdato = LocalDate.of(LocalDate.now().year - 2, 9, 1), grunnbeløpsmåned = YearMonth.of(LocalDate.now().year, 5))
        tilkjentYtelseRepository.insert(tilkjentYtelse)
    }

    val vedtaksperiodeJsonList =
        listOf(
            """[{"datoFra":"${LocalDate.now().year - 2}-09-01","datoTil":"${LocalDate.now().year - 1}-07-31","aktivitet":"FORSØRGER_I_ARBEID","periodeType":"HOVEDPERIODE"}]""",
            """[{"datoFra":"${LocalDate.now().year - 1}-02-01","datoTil":"${LocalDate.now().year - 1}-07-31","aktivitet":"FORSØRGER_I_ARBEID","periodeType":"HOVEDPERIODE","sanksjonsårsak":null}]""",
            """[{"datoFra":"${LocalDate.now().year - 1}-08-01","datoTil":"${LocalDate.now().year}-07-31","aktivitet":"FORLENGELSE_MIDLERTIDIG_SYKDOM","periodeType":"FORLENGELSE","sanksjonsårsak":null}]""",
            """[{"datoFra":"${LocalDate.now().year}-05-01","datoTil":"${LocalDate.now().year}-07-31","aktivitet":"FORLENGELSE_MIDLERTIDIG_SYKDOM","periodeType":"FORLENGELSE","sanksjonsårsak":null}]""",
        )

    val inntektsperiodeJsonList =
        listOf(
            """[{"startDato":"${LocalDate.now().year - 2}-09-01","sluttDato":"+999999999-12-31","inntekt":146000,"samordningsfradrag":0}]""",
            """[{"startDato":null,"sluttDato":null,"periode":{"fom":"${LocalDate.now().year - 1}-02","tom":"999999999-12"},"dagsats":0,"månedsinntekt":0,"inntekt":96000,"samordningsfradrag":0}]""",
            """[{"startDato":null,"sluttDato":null,"periode":{"fom":"${LocalDate.now().year - 1}-08","tom":"999999999-12"},"dagsats":0,"månedsinntekt":0,"inntekt":72000,"samordningsfradrag":0}]""",
            """[{"startDato":null,"sluttDato":null,"periode":{"fom":"${LocalDate.now().year - 1}-05","tom":"999999999-12"},"dagsats":0,"månedsinntekt":0,"inntekt":75200,"samordningsfradrag":0}]""",
        )
}
