package no.nav.familie.ef.sak.selvstendig

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
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
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
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

    private val personIdent = "11111111111"
    private val fagsakTilknyttetPersonIdent = fagsak(setOf(PersonIdent(personIdent)))

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
        testoppsettService.lagreFagsak(fagsakTilknyttetPersonIdent)

        for (i in 0..4) {
            val behandling = behandling(id = UUID.randomUUID(), fagsak = fagsakTilknyttetPersonIdent, status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET)
            behandlingRepository.insert(behandling)
            val vedtak = vedtak(behandlingId = behandling.id, perioder = PeriodeWrapper(objectMapper.readValue<List<Vedtaksperiode>>(vedtaksperiodeJsonList[i])), inntekter = InntektWrapper(objectMapper.readValue<List<Inntektsperiode>>(inntektsperiodeJsonList[i])))
            vedtakRepository.insert(vedtak)
        }
    }

    private fun lagEksternTestOppgave(tilordnetRessurs: String? = null): no.nav.familie.kontrakter.felles.oppgave.Oppgave =
        no.nav.familie.kontrakter.felles.oppgave
            .Oppgave(id = 1, tilordnetRessurs = tilordnetRessurs, oppgavetype = Oppgavetype.Fremlegg.toString(), fristFerdigstillelse = LocalDate.of(YearMonth.now().year, 12, 15).toString(), mappeId = 107, identer = listOf(OppgaveIdentV2(personIdent, IdentGruppe.FOLKEREGISTERIDENT)))

    @Test
    fun `sjekkNæringsinntektMotForventetInntekt med flere behandlinger og vedtak`() {
        kjørSomLeader {
            næringsinntektKontrollService.sjekkNæringsinntektMotForventetInntekt()
        }
    }
}

val vedtaksperiodeJsonList =
    listOf(
        """[{"datoFra":"2022-02-01","datoTil":"2025-01-31","aktivitet":"FORSØRGER_I_ARBEID","periodeType":"HOVEDPERIODE"}]""",
        """[{"datoFra":"2022-09-01","datoTil":"2025-04-30","aktivitet":"FORSØRGER_I_ARBEID","periodeType":"HOVEDPERIODE","sanksjonsårsak":null}]""",
        """[{"datoFra":"2023-05-01","datoTil":"2025-04-30","aktivitet":"FORSØRGER_I_ARBEID","periodeType":"HOVEDPERIODE","sanksjonsårsak":null}]""",
        """[{"datoFra":"2024-02-01","datoTil":"2025-04-30","aktivitet":"FORSØRGER_I_ARBEID","periodeType":"HOVEDPERIODE","sanksjonsårsak":null}]""",
        """[{"datoFra":"2024-05-01","datoTil":"2025-04-30","aktivitet":"FORSØRGER_I_ARBEID","periodeType":"HOVEDPERIODE","sanksjonsårsak":null}]""",
    )

val inntektsperiodeJsonList =
    listOf(
        """[{"startDato":"2022-02-01","sluttDato":"2022-02-28","inntekt":628000,"samordningsfradrag":0},{"startDato":"2022-03-01","sluttDato":"2022-03-31","inntekt":496000,"samordningsfradrag":0},{"startDato":"2022-04-01","sluttDato":"2022-04-30","inntekt":208000,"samordningsfradrag":0},{"startDato":"2022-05-01","sluttDato":"2022-05-31","inntekt":1739000,"samordningsfradrag":0},{"startDato":"2022-06-01","sluttDato":"+999999999-12-31","inntekt":692000,"samordningsfradrag":0}]""",
        """[{"startDato":null,"sluttDato":null,"periode":{"fom":"2022-09","tom":"2022-09"},"inntekt":251000,"samordningsfradrag":0},{"startDato":null,"sluttDato":null,"periode":{"fom":"2022-10","tom":"2022-10"},"inntekt":133000,"samordningsfradrag":0},{"startDato":null,"sluttDato":null,"periode":{"fom":"2022-11","tom":"2022-11"},"inntekt":191000,"samordningsfradrag":0},{"startDato":null,"sluttDato":null,"periode":{"fom":"2022-12","tom":"2022-12"},"inntekt":133000,"samordningsfradrag":0},{"startDato":null,"sluttDato":null,"periode":{"fom":"2023-01","tom":"999999999-12"},"inntekt":400000,"samordningsfradrag":0}]""",
        """[{"startDato":null,"sluttDato":null,"periode":{"fom":"2023-05","tom":"999999999-12"},"dagsats":0,"månedsinntekt":0,"inntekt":425600,"samordningsfradrag":0}]""",
        """[{"startDato":null,"sluttDato":null,"periode":{"fom":"2024-02","tom":"999999999-12"},"dagsats":0,"månedsinntekt":0,"inntekt":500000,"samordningsfradrag":0}]""",
        """[{"startDato":null,"sluttDato":null,"periode":{"fom":"2024-05","tom":"999999999-12"},"dagsats":0,"månedsinntekt":0,"inntekt":522700,"samordningsfradrag":0}]""",
    )
