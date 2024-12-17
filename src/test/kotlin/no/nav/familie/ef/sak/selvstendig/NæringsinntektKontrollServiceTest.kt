package no.nav.familie.ef.sak.selvstendig

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
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

    private val personIdent = "11111111111"
    private val fagsakTilknyttetPersonIdent = fagsak(setOf(PersonIdent(personIdent)))

    val oppgaveSlot = slot<Oppgave>()

    @BeforeEach
    fun setup() {
        val finnOppgaveRequest =
            FinnOppgaveRequest(
                tema = Tema.ENF,
                behandlingstema = Behandlingstema.Overgangsstønad,
                oppgavetype = Oppgavetype.Fremlegg,
                fristTomDato = LocalDate.of(YearMonth.now().year, 12, 15),
                mappeId = 107,
            )
        every { oppgaveClient.hentOppgaver(finnOppgaveRequest) } returns FinnOppgaveResponseDto(1, listOf(lagEksternTestOppgave()))
        every { oppgaveClient.oppdaterOppgave(capture(oppgaveSlot)) }

        testoppsettService.lagreFagsak(fagsakTilknyttetPersonIdent)

        val behandlingIds = mutableListOf<UUID>()
        for (i in 0..3) {
            val behandling = behandling(id = UUID.randomUUID(), fagsak = fagsakTilknyttetPersonIdent, status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET)
            val behandlingId = behandlingRepository.insert(behandling).id
            behandlingIds.add(behandlingId)
            val vedtak = vedtak(behandlingId = behandlingId, perioder = PeriodeWrapper(objectMapper.readValue<List<Vedtaksperiode>>(vedtaksperiodeJsonList[i])), inntekter = InntektWrapper(objectMapper.readValue<List<Inntektsperiode>>(inntektsperiodeJsonList[i])))
            vedtakRepository.insert(vedtak)
        }

        val andelerTilkjentYtelse = mutableListOf<AndelTilkjentYtelse>()

        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(17517, LocalDate.of(2022, 9, 1), LocalDate.of(2023, 1, 31), personIdent, behandlingIds[0], 146000, 0, 3385))
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(19392, LocalDate.of(2023, 2, 1), LocalDate.of(2023, 4, 30), personIdent, behandlingIds[1], 96000, 0, 1510))
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(20865, LocalDate.of(2023, 5, 1), LocalDate.of(2023, 7, 31), personIdent, behandlingIds[1], 96000, 0, 1376))
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(21765, LocalDate.of(2023, 8, 1), LocalDate.of(2024, 4, 30), personIdent, behandlingIds[2], 72000, 0, 476))
        andelerTilkjentYtelse.add(lagAndelTilkjentYtelse(22761, LocalDate.of(2024, 5, 1), LocalDate.of(2024, 7, 31), personIdent, behandlingIds[3], 75200, 0, 494))
        val tilkjentYtelse = lagTilkjentYtelse(andelerTilkjentYtelse = andelerTilkjentYtelse, behandlingId = behandlingIds[3], personident = personIdent, startdato = LocalDate.of(2022, 9, 1), grunnbeløpsmåned = YearMonth.of(2024, 5))
        tilkjentYtelseRepository.insert(tilkjentYtelse)
    }

    private fun lagEksternTestOppgave(tilordnetRessurs: String? = null): no.nav.familie.kontrakter.felles.oppgave.Oppgave =
        no.nav.familie.kontrakter.felles.oppgave
            .Oppgave(id = 1, tilordnetRessurs = tilordnetRessurs, oppgavetype = Oppgavetype.Fremlegg.toString(), fristFerdigstillelse = LocalDate.of(YearMonth.now().year, 12, 15).toString(), mappeId = 107, identer = listOf(OppgaveIdentV2(personIdent, IdentGruppe.FOLKEREGISTERIDENT)))

    @Test
    fun `sjekkNæringsinntektMotForventetInntekt med flere behandlinger og vedtak`() {
        kjørSomLeader {
            val fagsakIds = næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende()
            assertThat(fagsakIds.first()).isEqualTo(fagsakTilknyttetPersonIdent.id)
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
